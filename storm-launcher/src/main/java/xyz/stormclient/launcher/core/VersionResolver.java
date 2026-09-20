package xyz.stormclient.launcher.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import xyz.stormclient.util.Json;

/**
 * Locates the files needed to start a version, whatever launcher put them there.
 *
 * <p>Every launcher invents its own directory layout. Rather than hard coding
 * each one, this looks in the places they are known to use and, failing that,
 * searches the directory for a manifest that names a main class. The libraries,
 * natives and assets are then found relative to wherever the manifest turned up.
 */
public final class VersionResolver {

    public static final class Resolved {
        public final File manifest;
        public final File clientJar;
        public final File libraries;
        public final File natives;
        public final File assets;
        public final String layout;

        Resolved(File manifest, File clientJar, File libraries, File natives, File assets, String layout) {
            this.manifest = manifest;
            this.clientJar = clientJar;
            this.libraries = libraries;
            this.natives = natives;
            this.assets = assets;
            this.layout = layout;
        }

        public boolean complete() {
            return manifest != null && manifest.isFile() && libraries != null && libraries.isDirectory();
        }
    }

    private static final int MAX_DEPTH = 4;

    private VersionResolver() { }

    /** @return the resolved paths, or null when no manifest for this version exists. */
    public static Resolved resolve(File root, String version) {
        if (root == null || !root.isDirectory()) return null;

        File manifest = findManifest(root, version);
        if (manifest == null) return null;

        File manifestDir = manifest.getParentFile();
        File libraries = firstDirectory(
                new File(root, "libraries"),
                new File(root, "meta/libraries"),
                new File(manifestDir, "libraries"),
                new File(manifestDir.getParentFile(), "libraries"),
                new File(root.getParentFile(), "libraries"));

        File natives = firstDirectory(
                new File(manifestDir, version + "-natives"),
                new File(manifestDir, "natives"),
                new File(root, "versions/" + version + "/natives"),
                new File(root, "meta/natives/" + version),
                new File(root, "natives"));
        if (natives == null) natives = new File(manifestDir, version + "-natives");

        File assets = firstDirectory(
                new File(root, "assets"),
                new File(root, "meta/assets"),
                new File(root.getParentFile(), "assets"));
        if (assets == null) assets = new File(root, "assets");

        File clientJar = findClientJar(root, version, manifestDir);

        String layout = describeLayout(root, manifest);
        return new Resolved(manifest, clientJar, libraries, natives, assets, layout);
    }

    // ------------------------------------------------------------------
    //  manifest
    // ------------------------------------------------------------------
    private static File findManifest(File root, String version) {
        File[] known = {
                new File(root, "versions/" + version + "/" + version + ".json"),   // official
                new File(root, "meta/versions/" + version + ".json"),              // Modrinth style
                new File(root, "meta/versions/" + version + "/" + version + ".json"),
                new File(root, "versions/" + version + ".json"),
                new File(root, version + "/" + version + ".json")
        };
        for (File candidate : known) {
            if (isManifest(candidate)) {
                Log.info("version manifest: " + candidate);
                return candidate;
            }
        }

        // nothing known matched, go looking
        List<File> found = new ArrayList<>();
        search(root, version + ".json", 0, found);
        for (File candidate : found) {
            if (isManifest(candidate)) {
                Log.info("version manifest found by search: " + candidate);
                return candidate;
            }
        }
        Log.warn("no manifest for " + version + " under " + root);
        return null;
    }

    /** A manifest is a json file that names a main class or carries libraries. */
    private static boolean isManifest(File file) {
        if (file == null || !file.isFile() || file.length() > 4L * 1024 * 1024) return false;
        try {
            Map<String, Object> json = Json.readObject(
                    new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            return json.containsKey("mainClass") || json.containsKey("libraries")
                    || json.containsKey("inheritsFrom");
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static void search(File directory, String name, int depth, List<File> out) {
        if (depth > MAX_DEPTH || out.size() > 8) return;
        File[] children = directory.listFiles();
        if (children == null) return;

        for (File child : children) {
            if (child.isFile()) {
                if (child.getName().equalsIgnoreCase(name)) out.add(child);
            } else if (child.isDirectory() && !child.getName().startsWith(".")) {
                search(child, name, depth + 1, out);
            }
        }
    }

    // ------------------------------------------------------------------
    //  client jar
    // ------------------------------------------------------------------
    private static File findClientJar(File root, String version, File manifestDir) {
        File[] known = {
                new File(manifestDir, version + ".jar"),
                new File(manifestDir, "client.jar"),
                new File(root, "versions/" + version + "/" + version + ".jar"),
                new File(root, "meta/versions/" + version + "/" + version + ".jar"),
                new File(root, "meta/versions/" + version + ".jar"),
                new File(root, "libraries/com/mojang/minecraft/" + version
                        + "/minecraft-" + version + "-client.jar")
        };
        for (File candidate : known) {
            if (candidate.isFile()) return candidate;
        }

        List<File> found = new ArrayList<>();
        search(root, version + ".jar", 0, found);
        return found.isEmpty() ? null : found.get(0);
    }

    // ------------------------------------------------------------------
    private static File firstDirectory(File... candidates) {
        for (File candidate : candidates) {
            if (candidate != null && candidate.isDirectory()) return candidate;
        }
        return null;
    }

    private static String describeLayout(File root, File manifest) {
        String path = manifest.getAbsolutePath().replace(root.getAbsolutePath(), "");
        if (path.startsWith(File.separator + "versions")) return "official";
        if (path.contains("meta" + File.separator + "versions")) return "meta";
        return "custom";
    }

    /** Writes everything it found to the log, for when a launch still fails. */
    public static void diagnose(File root, String version) {
        Log.info("--- version lookup ---");
        Log.info("root    " + root);
        Resolved resolved = resolve(root, version);
        if (resolved == null) {
            Log.warn(version + " not found under " + root);
            Log.info("versions present: " + String.join(", ", GameDirectories.installedIn(root)));
            return;
        }
        Log.info("layout    " + resolved.layout);
        Log.info("manifest  " + resolved.manifest);
        Log.info("client    " + resolved.clientJar);
        Log.info("libraries " + resolved.libraries);
        Log.info("natives   " + resolved.natives);
        Log.info("assets    " + resolved.assets);
        Log.info("complete  " + resolved.complete());
    }
}
