package xyz.stormclient.launcher.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the jars a version manifest asks for.
 *
 * <p>Launchers agree on the manifest format and on nothing else. Some keep the
 * libraries in a maven tree, some rename them, some share one tree between
 * several installations. So every candidate root is walked once, jars are
 * indexed by file name, and a library that is not where the manifest says it
 * should be is still found by its name.
 */
public final class LibraryIndex {

    private static final int MAX_DEPTH = 8;
    private static final int MAX_FILES = 20000;

    /** Big folders that never hold a library jar. */
    private static final Set<String> SKIP = new java.util.HashSet<>(java.util.Arrays.asList(
            "assets", "saves", "logs", "screenshots", "crash-reports", "resourcepacks",
            "shaderpacks", "texturepacks", "server-resource-packs", "webcache", "backups"));

    private final List<File> roots;
    private final Map<String, File> byName = new HashMap<>();

    private LibraryIndex(List<File> roots) {
        this.roots = roots;
        for (File root : roots) walk(root, 0);
        Log.info("library index: " + byName.size() + " jars across " + roots.size() + " roots");
    }

    public static LibraryIndex build(List<File> roots) {
        return new LibraryIndex(roots);
    }

    /** Every place this machine might keep Minecraft libraries. */
    public static List<File> roots(File gameDir, File manifestDir, File resolvedLibraries) {
        Set<File> out = new LinkedHashSet<>();
        addIfDirectory(out, resolvedLibraries);
        addIfDirectory(out, new File(gameDir, "libraries"));
        addIfDirectory(out, new File(gameDir, "meta/libraries"));

        if (manifestDir != null) {
            addIfDirectory(out, new File(manifestDir, "libraries"));
            File parent = manifestDir.getParentFile();
            if (parent != null) {
                addIfDirectory(out, new File(parent, "libraries"));
                if (parent.getParentFile() != null) {
                    addIfDirectory(out, new File(parent.getParentFile(), "libraries"));
                }
            }
        }
        if (gameDir != null && gameDir.getParentFile() != null) {
            addIfDirectory(out, new File(gameDir.getParentFile(), "libraries"));
        }
        // the official launcher shares the same jars, so it is a good last resort
        addIfDirectory(out, new File(LauncherConfig.defaultGameDirectory(), "libraries"));

        // Last of all the whole installation, because a launcher is free to put
        // its jars anywhere and several do. The explicit roots are searched
        // first, so this only ever adds what the others missed.
        addIfDirectory(out, gameDir);

        return new ArrayList<>(out);
    }

    private static void addIfDirectory(Set<File> out, File candidate) {
        if (candidate != null && candidate.isDirectory()) out.add(candidate);
    }

    private void walk(File directory, int depth) {
        if (depth > MAX_DEPTH || byName.size() > MAX_FILES) return;
        File[] children = directory.listFiles();
        if (children == null) return;

        for (File child : children) {
            if (child.isDirectory()) {
                String name = child.getName().toLowerCase();
                if (name.startsWith(".") || SKIP.contains(name)) continue;
                walk(child, depth + 1);
            } else if (child.getName().endsWith(".jar")) {
                byName.putIfAbsent(child.getName(), child);
            }
        }
    }

    /**
     * @param relativePath the path the manifest asks for, maven style
     * @return the jar, or null when nothing on this machine matches
     */
    public File find(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return null;

        for (File root : roots) {
            File direct = new File(root, relativePath);
            if (direct.isFile()) return direct;
        }
        int slash = relativePath.lastIndexOf('/');
        String fileName = slash < 0 ? relativePath : relativePath.substring(slash + 1);
        return byName.get(fileName);
    }

    public int size() { return byName.size(); }
}
