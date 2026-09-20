package xyz.stormclient.launcher.core;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds Minecraft installations on this machine.
 *
 * <p>The official launcher keeps versions in {@code <root>/versions/<id>/<id>.json}.
 * Third party launchers each do their own thing, so Storm looks in the places
 * they are known to use and reports what it actually found instead of assuming
 * one layout.
 */
public final class GameDirectories {

    /** One installation root with the versions inside it. */
    public static final class Install {
        public final String launcher;
        public final File root;
        public final Set<String> versions;
        public final boolean vanillaLayout;

        Install(String launcher, File root, Set<String> versions, boolean vanillaLayout) {
            this.launcher = launcher;
            this.root = root;
            this.versions = versions;
            this.vanillaLayout = vanillaLayout;
        }

        public boolean has(String version) { return versions.contains(version); }

        @Override public String toString() {
            return launcher + " (" + versions.size() + " versions) " + root;
        }
    }

    private GameDirectories() { }

    // ------------------------------------------------------------------
    //  known roots
    // ------------------------------------------------------------------
    private static Map<String, File> roots() {
        Map<String, File> out = new LinkedHashMap<>();
        String home = System.getProperty("user.home", ".");
        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            String localAppData = System.getenv("LOCALAPPDATA");
            if (appData != null) {
                out.put("Official launcher", new File(appData, ".minecraft"));
                out.put("Modrinth App",      new File(appData, "com.modrinth.theseus"));
                out.put("Prism Launcher",    new File(appData, "PrismLauncher"));
                out.put("ATLauncher",        new File(appData, "ATLauncher"));
                out.put("GDLauncher",        new File(appData, "gdlauncher_next"));
            }
            if (localAppData != null) {
                out.put("CurseForge", new File(localAppData, "Packages/CurseForge"));
            }
            out.put("MultiMC", new File(home, "MultiMC"));
            out.put("Legacy Launcher", new File(home, ".tlauncher"));
        } else if (os.contains("mac")) {
            File support = new File(home, "Library/Application Support");
            out.put("Official launcher", new File(support, "minecraft"));
            out.put("Modrinth App",      new File(support, "com.modrinth.theseus"));
            out.put("Prism Launcher",    new File(support, "PrismLauncher"));
        } else {
            out.put("Official launcher", new File(home, ".minecraft"));
            out.put("Modrinth App",      new File(home, ".config/com.modrinth.theseus"));
            out.put("Prism Launcher",    new File(home, ".local/share/PrismLauncher"));
            out.put("MultiMC",           new File(home, ".local/share/multimc"));
        }
        return out;
    }

    /** Every installation that actually holds at least one version. */
    public static List<Install> scan() {
        List<Install> out = new ArrayList<>();
        for (Map.Entry<String, File> entry : roots().entrySet()) {
            Install install = inspect(entry.getKey(), entry.getValue());
            if (install != null && !install.versions.isEmpty()) out.add(install);
        }
        return out;
    }

    /** Looks at one directory the user picked, whatever launcher made it. */
    public static Install inspect(String name, File root) {
        if (root == null || !root.isDirectory()) return null;

        Set<String> versions = new LinkedHashSet<>();
        boolean vanilla = false;

        // official layout: versions/<id>/<id>.json
        File versionsDir = new File(root, "versions");
        if (versionsDir.isDirectory()) {
            File[] children = versionsDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!child.isDirectory()) continue;
                    if (new File(child, child.getName() + ".json").isFile()) {
                        versions.add(child.getName());
                        vanilla = true;
                    }
                }
            }
        }

        // Modrinth and friends: meta/versions/<id>.json
        File meta = new File(root, "meta/versions");
        if (meta.isDirectory()) {
            File[] children = meta.listFiles();
            if (children != null) {
                for (File child : children) {
                    String fileName = child.getName();
                    if (child.isFile() && fileName.endsWith(".json")) {
                        versions.add(fileName.substring(0, fileName.length() - 5));
                    } else if (child.isDirectory()) {
                        versions.add(fileName);
                    }
                }
            }
        }

        // Prism and MultiMC: instances/<name>/mmc-pack.json
        File instances = new File(root, "instances");
        if (instances.isDirectory()) {
            File[] children = instances.listFiles();
            if (children != null) {
                for (File child : children) {
                    File inner = new File(child, ".minecraft/versions");
                    if (inner.isDirectory()) versions.add(child.getName());
                }
            }
        }

        return new Install(name, root, versions, vanilla);
    }

    /** The installation Storm should use by default: the one with the most versions. */
    public static Install preferred() {
        Install best = null;
        for (Install install : scan()) {
            if (best == null || install.versions.size() > best.versions.size()) best = install;
            if (install.vanillaLayout && best != null && !best.vanillaLayout) best = install;
        }
        return best;
    }

    /** Versions installed in the directory the launcher is configured to use. */
    public static Set<String> installedIn(File root) {
        Install install = inspect("configured", root);
        return install == null ? new LinkedHashSet<String>() : install.versions;
    }

    /** True when this directory can be started directly by {@link GameLauncher}. */
    public static boolean canLaunchFrom(File root, String version) {
        return new File(root, "versions/" + version + "/" + version + ".json").isFile();
    }

    /** Human readable summary for the error message when a version is missing. */
    public static String describeAlternatives(String version) {
        StringBuilder sb = new StringBuilder();
        for (Install install : scan()) {
            if (!install.has(version)) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(install.launcher);
        }
        return sb.toString();
    }
}
