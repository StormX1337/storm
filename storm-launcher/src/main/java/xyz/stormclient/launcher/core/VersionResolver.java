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
    private static final long CACHE_MS = 4000;

    private static final Map<String, Object[]> CACHE = new java.util.HashMap<>();

    private VersionResolver() { }

    /**
     * @return the resolved paths, or null when no manifest for this version exists.
     *         Results are cached briefly, since the UI asks on every repaint.
     */
    public static synchronized Resolved resolve(File root, String version) {
        if (root == null || !root.isDirectory()) return null;

        String key = root.getAbsolutePath() + "|" + version;
        Object[] cached = CACHE.get(key);
        if (cached != null && System.currentTimeMillis() - (Long) cached[0] < CACHE_MS) {
            return (Resolved) cached[1];
        }
        Resolved resolved = resolveUncached(root, version);
        CACHE.put(key, new Object[] { System.currentTimeMillis(), resolved });
        return resolved;
    }

    public static synchronized void clearCache() { CACHE.clear(); }

    private static Resolved resolveUncached(File root, String version) {
        File manifest = findManifest(root, version);
        if (manifest == null) return null;
        Log.info("version manifest: " + manifest);

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
    /** Directories that only ever hold bulk data, never a version manifest. */
    private static final java.util.Set<String> SKIP = new java.util.HashSet<>(java.util.Arrays.asList(
            "assets", "libraries", "mods", "resourcepacks", "saves", "shaderpacks",
            "screenshots", "logs", "crash-reports", "natives", "cache", "caches",
            "texturepacks", "server-resource-packs", "webcache"));

    private static File findManifest(File root, String version) {
        File[] known = {
                new File(root, "versions/" + version + "/" + version + ".json"),   // official
                new File(root, "meta/versions/" + version + ".json"),              // Modrinth style
                new File(root, "meta/versions/" + version + "/" + version + ".json"),
                new File(root, "versions/" + version + ".json"),
                new File(root, version + "/" + version + ".json")
        };
        for (File candidate : known) {
            if (isManifest(candidate)) return candidate;
        }

        // Nothing known matched. Launchers name the file whatever they like, so
        // score every json by where it sits and pick the best one that actually
        // parses as a version manifest.
        List<File> candidates = new ArrayList<>();
        collectJson(root, 0, candidates);

        File best = null;
        int bestScore = Integer.MIN_VALUE;
        for (File candidate : candidates) {
            int score = score(candidate, version);
            if (score <= bestScore) continue;
            if (!isManifest(candidate)) continue;
            best = candidate;
            bestScore = score;
        }
        return best;
    }

    private static int score(File file, String version) {
        String name = file.getName().toLowerCase();
        String path = file.getAbsolutePath().toLowerCase().replace('\\', '/');
        String id = version.toLowerCase();

        int score = 0;
        if (name.equals(id + ".json")) score += 6;
        if (name.contains(id)) score += 3;
        if (path.contains("/" + id + "/")) score += 4;
        if (path.contains("/versions/")) score += 2;
        if (name.equals("version.json") || name.equals("client.json")) score += 2;
        if (name.contains("profile") || name.contains("launcher") || name.contains("index")) score -= 4;
        return score;
    }

    private static void collectJson(File directory, int depth, List<File> out) {
        if (depth > MAX_DEPTH + 1 || out.size() > 400) return;
        File[] children = directory.listFiles();
        if (children == null) return;

        for (File child : children) {
            if (child.isFile()) {
                if (child.getName().toLowerCase().endsWith(".json")) out.add(child);
            } else if (child.isDirectory()) {
                String name = child.getName().toLowerCase();
                if (name.startsWith(".") || SKIP.contains(name)) continue;
                collectJson(child, depth + 1, out);
            }
        }
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

    /** Prints the directory structure so an unknown layout can be identified. */
    public static void dumpTree(File root) {
        Log.info("--- directory tree of " + root + " ---");
        if (root == null || !root.isDirectory()) {
            Log.warn("not a directory");
            return;
        }
        dumpTree(root, "", 0, new int[] { 0 });
        Log.info("--- end of tree ---");
    }

    private static void dumpTree(File directory, String indent, int depth, int[] printed) {
        if (depth > 3 || printed[0] > 160) return;
        File[] children = directory.listFiles();
        if (children == null) return;

        java.util.Arrays.sort(children);
        int shown = 0;
        for (File child : children) {
            if (printed[0] > 160) return;
            if (shown++ > 24) {
                Log.info(indent + "  ... " + (children.length - shown) + " more");
                return;
            }
            printed[0]++;

            if (child.isDirectory()) {
                String name = child.getName().toLowerCase();
                boolean skipped = SKIP.contains(name);
                Log.info(indent + "[" + child.getName() + "]" + (skipped ? "  (skipped)" : ""));
                if (!skipped) dumpTree(child, indent + "  ", depth + 1, printed);
            } else {
                Log.info(indent + child.getName() + "  " + (child.length() / 1024) + " kB");
            }
        }
    }

    /** Writes everything it found to the log, for when a launch still fails. */
    public static void diagnose(File root, String version) {
        Log.info("--- version lookup ---");
        Log.info("root    " + root);
        clearCache();
        Resolved resolved = resolve(root, version);
        if (resolved == null) {
            Log.warn(version + " not found under " + root);
            Log.info("versions present: " + String.join(", ", GameDirectories.installedIn(root)));
            dumpTree(root);
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
