package xyz.stormclient.launcher.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import xyz.stormclient.util.Json;

/**
 * Starts a vanilla installation directly, with the Storm agent already on the
 * command line. Reads {@code versions/<id>/<id>.json} the same way the official
 * launcher does, so no extra downloads are needed for a version that is
 * already installed.
 *
 * <p>Offline session only - Storm never asks for, stores or transmits account
 * credentials. Use the official launcher for an online session and inject into
 * it instead.
 */
public final class GameLauncher {

    private GameLauncher() { }

    public static final class Options {
        public String  version    = "1.8.9";
        public String  username   = "Player";
        public File    gameDir    = LauncherConfig.defaultGameDirectory();
        public File    javaPath   = new File(LauncherConfig.defaultJava());
        public File    agentJar;
        public File    bridgeJar;
        public String  agentOptions = "";
        public int     ram        = 4096;
        public boolean debug;
    }

    /** @return the started process, or null when something was missing. */
    @SuppressWarnings("unchecked")
    public static Process launch(Options options, Consumer<String> output) throws Exception {
        File versionDir = new File(options.gameDir, "versions/" + options.version);
        File manifest = new File(versionDir, options.version + ".json");
        if (!manifest.isFile()) {
            throw new IllegalStateException(describeMissing(options));
        }

        Map<String, Object> json = Json.readObject(
                new String(Files.readAllBytes(manifest.toPath()), StandardCharsets.UTF_8));

        String mainClass = String.valueOf(json.getOrDefault("mainClass", "net.minecraft.client.main.Main"));
        String assetIndex = assetIndex(json);

        List<String> classpath = classpath(options.gameDir, json, versionDir, options.version);
        File natives = natives(versionDir, options.version);

        List<String> command = new ArrayList<>();
        command.add(options.javaPath.getPath());
        command.add("-Xmx" + options.ram + "M");
        command.add("-Xms" + Math.min(options.ram, 1024) + "M");
        command.add("-Dstorm.mcversion=" + options.version);
        command.add("-Djava.library.path=" + natives.getAbsolutePath());
        command.add("-Dminecraft.launcher.brand=storm");
        command.add("-Dminecraft.launcher.version=" + xyz.stormclient.launcher.StormLauncher.VERSION);
        command.add("-XX:+EnableDynamicAgentLoading");

        if (options.agentJar != null && options.agentJar.isFile()) {
            command.add("-javaagent:" + options.agentJar.getAbsolutePath()
                    + (options.agentOptions.isEmpty() ? "" : "=" + options.agentOptions));
        }

        command.add("-cp");
        command.add(String.join(File.pathSeparator, classpath));
        command.add(mainClass);

        command.addAll(gameArguments(json, options, assetIndex));

        Log.info("launching " + options.version + " with " + classpath.size() + " libraries");
        Log.info("command: " + options.javaPath.getName() + " ... " + mainClass);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(options.gameDir);
        builder.redirectErrorStream(true);

        Process process = builder.start();
        pipe(process, output);
        return process;
    }

    /**
     * A missing version is almost always a third party launcher keeping its
     * files somewhere else, so say where it actually is instead of assuming
     * the user never installed it.
     */
    private static String describeMissing(Options options) {
        java.util.Set<String> here = GameDirectories.installedIn(options.gameDir);
        String elsewhere = GameDirectories.describeAlternatives(options.version);

        if (!elsewhere.isEmpty()) {
            return options.version + " is installed in " + elsewhere
                    + ", not in " + options.gameDir.getName()
                    + ". Point the game directory there in Settings, or start it in that"
                    + " launcher and use Inject.";
        }
        if (here.isEmpty()) {
            return "no Minecraft versions found in " + options.gameDir
                    + ". Set the game directory in Settings.";
        }
        return options.version + " is not installed in " + options.gameDir.getName()
                + ". Found: " + String.join(", ", here);
    }

    private static void pipe(Process process, Consumer<String> output) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output != null) output.accept(line);
                }
            } catch (Exception e) {
                Log.warn("lost the game output stream: " + e);
            }
            Log.info("the game exited with code " + process.exitValue());
        }, "Storm-GameOutput");
        thread.setDaemon(true);
        thread.start();
    }

    @SuppressWarnings("unchecked")
    private static List<String> classpath(File gameDir, Map<String, Object> json, File versionDir, String version) {
        List<String> out = new ArrayList<>();
        File libraries = new File(gameDir, "libraries");

        Object rawLibraries = json.get("libraries");
        if (rawLibraries instanceof List) {
            for (Object entry : (List<Object>) rawLibraries) {
                if (!(entry instanceof Map)) continue;
                Map<String, Object> library = (Map<String, Object>) entry;
                Object name = library.get("name");
                if (name == null) continue;
                if (library.containsKey("natives")) continue;          // extracted separately

                File jar = new File(libraries, mavenToPath(String.valueOf(name)));
                if (jar.isFile()) out.add(jar.getAbsolutePath());
            }
        }
        File client = new File(versionDir, version + ".jar");
        if (client.isFile()) out.add(client.getAbsolutePath());
        return out;
    }

    /** {@code group:artifact:version} -> {@code group/path/artifact/version/artifact-version.jar} */
    static String mavenToPath(String name) {
        String[] parts = name.split(":");
        if (parts.length < 3) return name;
        String group = parts[0].replace('.', '/');
        String artifact = parts[1];
        String version = parts[2];
        String classifier = parts.length > 3 ? "-" + parts[3] : "";
        return group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + classifier + ".jar";
    }

    private static File natives(File versionDir, String version) {
        File natives = new File(versionDir, version + "-natives");
        if (natives.isDirectory()) return natives;
        File legacy = new File(versionDir, "natives");
        return legacy.isDirectory() ? legacy : natives;
    }

    @SuppressWarnings("unchecked")
    private static String assetIndex(Map<String, Object> json) {
        Object index = json.get("assetIndex");
        if (index instanceof Map) {
            Object id = ((Map<String, Object>) index).get("id");
            if (id != null) return String.valueOf(id);
        }
        return String.valueOf(json.getOrDefault("assets", "legacy"));
    }

    @SuppressWarnings("unchecked")
    private static List<String> gameArguments(Map<String, Object> json, Options options, String assetIndex) {
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("auth_player_name", options.username);
        tokens.put("version_name", options.version);
        tokens.put("game_directory", options.gameDir.getAbsolutePath());
        tokens.put("assets_root", new File(options.gameDir, "assets").getAbsolutePath());
        tokens.put("game_assets", new File(options.gameDir, "assets/virtual/legacy").getAbsolutePath());
        tokens.put("assets_index_name", assetIndex);
        tokens.put("auth_uuid", UUID.nameUUIDFromBytes(("OfflinePlayer:" + options.username)
                .getBytes(StandardCharsets.UTF_8)).toString().replace("-", ""));
        tokens.put("auth_access_token", "0");
        tokens.put("auth_session", "0");
        tokens.put("user_type", "legacy");
        tokens.put("version_type", "release");
        tokens.put("user_properties", "{}");

        List<String> out = new ArrayList<>();
        Object legacy = json.get("minecraftArguments");
        if (legacy != null) {
            for (String argument : String.valueOf(legacy).split("\\s+")) out.add(replace(argument, tokens));
            return out;
        }

        Object argumentsRaw = json.get("arguments");
        if (argumentsRaw instanceof Map) {
            Object game = ((Map<String, Object>) argumentsRaw).get("game");
            if (game instanceof List) {
                for (Object argument : (List<Object>) game) {
                    if (argument instanceof String) out.add(replace((String) argument, tokens));
                    // conditional argument blocks are launcher features Storm does not need
                }
            }
        }
        return out;
    }

    private static String replace(String argument, Map<String, String> tokens) {
        String out = argument;
        for (Map.Entry<String, String> token : tokens.entrySet()) {
            out = out.replace("${" + token.getKey() + "}", token.getValue());
        }
        return out;
    }
}
