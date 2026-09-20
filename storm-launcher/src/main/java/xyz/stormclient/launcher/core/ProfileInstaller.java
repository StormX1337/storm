package xyz.stormclient.launcher.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import xyz.stormclient.util.Json;

/**
 * Adds a "Storm &lt;version&gt;" profile to the official launcher, with the agent
 * already in the JVM arguments. Handy for anyone who wants to keep using a
 * real Mojang session instead of the offline launch button.
 */
public final class ProfileInstaller {

    private ProfileInstaller() { }

    @SuppressWarnings("unchecked")
    public static boolean install(File gameDirectory, String version, File agentJar, String agentOptions) {
        File file = new File(gameDirectory, "launcher_profiles.json");
        if (!file.isFile()) {
            Log.warn("launcher_profiles.json not found in " + gameDirectory
                    + " - that file only exists in the official launcher, so there is no"
                    + " profile to add to here");
            return false;
        }

        try {
            Map<String, Object> root = Json.readObject(
                    new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));

            Object profilesRaw = root.get("profiles");
            Map<String, Object> profiles = profilesRaw instanceof Map
                    ? (Map<String, Object>) profilesRaw
                    : new LinkedHashMap<String, Object>();

            String jvmArgs = "-Xmx4G -XX:+EnableDynamicAgentLoading -Dstorm.mcversion=" + version
                    + " -javaagent:" + agentJar.getAbsolutePath()
                    + (agentOptions == null || agentOptions.isEmpty() ? "" : "=" + agentOptions);

            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("name", "Storm " + version);
            profile.put("type", "custom");
            profile.put("lastVersionId", version);
            profile.put("javaArgs", jvmArgs);
            profile.put("icon", "Furnace");

            profiles.put("storm-" + version, profile);
            root.put("profiles", profiles);

            backup(file);
            Files.write(file.toPath(), Json.write(root).getBytes(StandardCharsets.UTF_8));
            Log.info("installed the Storm " + version + " profile into the official launcher");
            return true;
        } catch (Exception e) {
            Log.error("could not write launcher_profiles.json", e);
            return false;
        }
    }

    private static void backup(File file) throws IOException {
        File backup = new File(file.getParentFile(), file.getName() + ".storm-backup");
        if (!backup.exists()) Files.copy(file.toPath(), backup.toPath());
    }
}
