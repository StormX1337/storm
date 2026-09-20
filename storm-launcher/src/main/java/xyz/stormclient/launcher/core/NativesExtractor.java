package xyz.stormclient.launcher.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Puts the LWJGL native libraries where the game can load them.
 *
 * <p>Every launcher extracts natives somewhere different, and some clean up
 * after themselves. Rather than guessing, the native jars named in the
 * manifest are unpacked into a folder Storm owns.
 */
public final class NativesExtractor {

    private NativesExtractor() { }

    /** @return the directory to hand to java.library.path. */
    @SuppressWarnings("unchecked")
    public static File prepare(Map<String, Object> json, LibraryIndex index,
                               File existing, File gameDir, String version) {
        if (hasNativeFiles(existing)) {
            Log.info("natives already extracted: " + existing);
            return existing;
        }

        File target = new File(gameDir, "storm-natives/" + version);
        if (hasNativeFiles(target)) {
            Log.info("using previously extracted natives: " + target);
            return target;
        }
        if (!target.isDirectory() && !target.mkdirs()) {
            Log.warn("could not create " + target);
            return existing;
        }

        String key = nativeKey();
        int extracted = 0;

        Object rawLibraries = json.get("libraries");
        if (rawLibraries instanceof List) {
            for (Object entry : (List<Object>) rawLibraries) {
                if (!(entry instanceof Map)) continue;
                String path = nativePath((Map<String, Object>) entry, key);
                if (path == null) continue;

                File jar = index.find(path);
                if (jar == null) {
                    Log.warn("native jar missing: " + path);
                    continue;
                }
                extracted += unpack(jar, target);
            }
        }

        Log.info("extracted " + extracted + " native files into " + target);
        return extracted > 0 ? target : existing;
    }

    private static boolean hasNativeFiles(File directory) {
        if (directory == null || !directory.isDirectory()) return false;
        File[] children = directory.listFiles();
        if (children == null) return false;
        for (File child : children) {
            if (isNative(child.getName())) return true;
        }
        return false;
    }

    private static boolean isNative(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".dll") || lower.endsWith(".so")
                || lower.endsWith(".dylib") || lower.endsWith(".jnilib");
    }

    /** The classifier this operating system needs. */
    private static String nativeKey() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return "natives-windows";
        if (os.contains("mac")) return "natives-osx";
        return "natives-linux";
    }

    @SuppressWarnings("unchecked")
    private static String nativePath(Map<String, Object> library, String key) {
        Object downloads = library.get("downloads");
        if (downloads instanceof Map) {
            Object classifiers = ((Map<String, Object>) downloads).get("classifiers");
            if (classifiers instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) classifiers;
                Object artifact = map.get(key);
                if (artifact == null) artifact = map.get(key + "-64");
                if (artifact instanceof Map) {
                    Object path = ((Map<String, Object>) artifact).get("path");
                    if (path != null) return String.valueOf(path);
                }
            }
        }

        // older manifests only carry the maven name plus a natives map
        Object natives = library.get("natives");
        if (natives instanceof Map) {
            Object classifier = ((Map<String, Object>) natives).get(key.replace("natives-", ""));
            Object name = library.get("name");
            if (classifier != null && name != null) {
                String suffix = String.valueOf(classifier).replace("${arch}", "64");
                return GameLauncher.mavenToPath(String.valueOf(name) + ":" + suffix);
            }
        }
        return null;
    }

    private static int unpack(File jar, File target) {
        int count = 0;
        try (ZipFile zip = new ZipFile(jar)) {
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;

                String name = new File(entry.getName()).getName();
                if (!isNative(name)) continue;

                File out = new File(target, name);
                if (out.isFile() && out.length() == entry.getSize()) { count++; continue; }

                try (InputStream in = zip.getInputStream(entry)) {
                    Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    count++;
                }
            }
        } catch (IOException e) {
            Log.warn("could not unpack " + jar.getName() + ": " + e);
        }
        return count;
    }
}
