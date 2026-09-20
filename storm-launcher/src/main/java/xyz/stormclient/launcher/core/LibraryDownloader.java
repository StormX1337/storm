package xyz.stormclient.launcher.core;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Fetches libraries that are named in a manifest but missing from disk.
 *
 * <p>The Forge 1.8.9 installer points at hosts that have moved since, and
 * quietly leaves a few jars behind. Without launchwrapper the game does not
 * start at all, so rather than reporting the gap Storm closes it.
 */
public final class LibraryDownloader {

    private static final String MOJANG = "https://libraries.minecraft.net/";
    private static final int TIMEOUT_MS = 20000;

    private LibraryDownloader() { }

    /** @return the downloaded file, or null when it could not be fetched. */
    public static File fetch(String relativePath, String baseUrl, File librariesDir) {
        if (relativePath == null || librariesDir == null) return null;

        File target = new File(librariesDir, relativePath);
        if (target.isFile() && target.length() > 0) return target;

        for (String url : urlsFor(relativePath, baseUrl)) {
            if (download(url, target)) {
                Log.info("downloaded " + target.getName());
                return target;
            }
        }
        Log.warn("could not download " + relativePath);
        return null;
    }

    /** Several hosts serve these jars, and the old manifests name dead ones. */
    private static String[] urlsFor(String path, String baseUrl) {
        String forge = "https://maven.minecraftforge.net/" + path;
        String central = "https://repo1.maven.org/maven2/" + path;
        String mojang = MOJANG + path;

        if (baseUrl == null || baseUrl.isEmpty()) {
            return new String[] { mojang, central, forge };
        }
        String fixed = baseUrl
                .replace("http://", "https://")
                .replace("files.minecraftforge.net/maven/", "maven.minecraftforge.net/");
        if (!fixed.endsWith("/")) fixed = fixed + "/";

        return new String[] { fixed + path, forge, central, mojang };
    }

    private static boolean download(String url, File target) {
        HttpURLConnection connection = null;
        try {
            File parent = target.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) return false;

            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Storm-Launcher");

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return false;

            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return target.length() > 0;
        } catch (Exception e) {
            Log.debugLine("download failed " + url + ": " + e);
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
