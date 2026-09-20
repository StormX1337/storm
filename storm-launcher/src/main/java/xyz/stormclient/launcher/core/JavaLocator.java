package xyz.stormclient.launcher.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds a Java that a given Minecraft can actually run on.
 *
 * <p>Old versions are not merely happier on Java 8, they require it:
 * launchwrapper casts the system class loader to a URLClassLoader, which stopped
 * being true in Java 9, so a Forge 1.8.9 dies on the first line of its own main
 * method under anything newer.
 */
public final class JavaLocator {

    private static final Map<String, Integer> VERSIONS = new HashMap<>();

    private JavaLocator() { }

    /** The major version a Minecraft release needs. */
    public static int requiredFor(String minecraftVersion) {
        int[] parts = parse(minecraftVersion);
        int minor = parts[0];
        int patch = parts[1];

        if (minor <= 16) return 8;                      // launchwrapper era
        if (minor == 17 || minor == 18 || minor == 19) return 17;
        if (minor == 20 && patch < 5) return 17;
        return 21;
    }

    /** @return {minor, patch} of a version like 1.8.9 */
    private static int[] parse(String version) {
        String[] parts = version.split("\\.");
        int minor = 8;
        int patch = 0;
        try {
            if (parts.length > 1) minor = Integer.parseInt(parts[1].replaceAll("\\D.*", ""));
            if (parts.length > 2) patch = Integer.parseInt(parts[2].replaceAll("\\D.*", ""));
        } catch (NumberFormatException ignored) {
            // an unusual version name, treat it as modern
            return new int[] { 21, 0 };
        }
        return new int[] { minor, patch };
    }

    /** The major version of a java executable, 0 when it cannot be read. */
    public static int versionOf(File javaExecutable) {
        if (javaExecutable == null || !javaExecutable.isFile()) return 0;

        String key = javaExecutable.getAbsolutePath();
        Integer cached = VERSIONS.get(key);
        if (cached != null) return cached;

        int version = readVersion(javaExecutable);
        VERSIONS.put(key, version);
        return version;
    }

    private static int readVersion(File javaExecutable) {
        // the release file next to the runtime is cheaper than starting a JVM
        File home = javaExecutable.getParentFile() == null
                ? null : javaExecutable.getParentFile().getParentFile();
        if (home != null) {
            File release = new File(home, "release");
            if (release.isFile()) {
                try {
                    for (String line : java.nio.file.Files.readAllLines(release.toPath())) {
                        if (!line.startsWith("JAVA_VERSION=")) continue;
                        return majorOf(line.substring(13).replace("\"", "").trim());
                    }
                } catch (Exception ignored) {
                    // fall through to running it
                }
            }
        }
        try {
            Process process = new ProcessBuilder(javaExecutable.getPath(), "-version")
                    .redirectErrorStream(true).start();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                process.destroy();
                if (line == null) return 0;

                java.util.regex.Matcher matcher =
                        java.util.regex.Pattern.compile("\"([0-9._]+)").matcher(line);
                return matcher.find() ? majorOf(matcher.group(1)) : 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private static int majorOf(String version) {
        String[] parts = version.split("[._]");
        try {
            int first = Integer.parseInt(parts[0]);
            if (first == 1 && parts.length > 1) return Integer.parseInt(parts[1]);
            return first;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Every java executable this machine seems to have. */
    public static List<File> installed() {
        Set<File> found = new LinkedHashSet<>();
        String os = System.getProperty("os.name", "").toLowerCase();
        String executable = os.contains("win") ? "java.exe" : "java";

        File own = new File(System.getProperty("java.home", ""), "bin/" + executable);
        if (own.isFile()) found.add(own);

        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            File candidate = new File(javaHome, "bin/" + executable);
            if (candidate.isFile()) found.add(candidate);
        }

        List<File> roots = new ArrayList<>();
        if (os.contains("win")) {
            for (String base : new String[] { System.getenv("ProgramFiles"),
                                              System.getenv("ProgramFiles(x86)"),
                                              System.getenv("LOCALAPPDATA") }) {
                if (base == null) continue;
                for (String vendor : new String[] { "Java", "Eclipse Adoptium", "Eclipse Foundation",
                                                    "Microsoft", "Amazon Corretto", "Zulu", "BellSoft",
                                                    "Semeru", "Programs\\Eclipse Adoptium" }) {
                    roots.add(new File(base, vendor));
                }
            }
            // the official launcher ships its own runtimes
            String appData = System.getenv("APPDATA");
            if (appData != null) roots.add(new File(appData, ".minecraft/runtime"));
        } else {
            roots.add(new File("/usr/lib/jvm"));
            roots.add(new File("/Library/Java/JavaVirtualMachines"));
            roots.add(new File(System.getProperty("user.home", "."), ".sdkman/candidates/java"));
        }

        for (File root : roots) {
            collect(root, executable, found, 0);
        }
        return new ArrayList<>(found);
    }

    private static void collect(File directory, String executable, Set<File> found, int depth) {
        if (depth > 3 || !directory.isDirectory() || found.size() > 40) return;
        File candidate = new File(directory, "bin/" + executable);
        if (candidate.isFile()) {
            found.add(candidate);
            return;
        }
        File[] children = directory.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) collect(child, executable, found, depth + 1);
        }
    }

    /** A java of the wanted major version, or null when none is installed. */
    public static File findMatching(int wantedMajor) {
        for (File candidate : installed()) {
            if (versionOf(candidate) == wantedMajor) return candidate;
        }
        return null;
    }
}
