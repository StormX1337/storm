package xyz.stormclient.launcher.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
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
        VersionResolver.Resolved resolved = VersionResolver.resolve(options.gameDir, options.version);
        if (resolved == null || !resolved.complete()) {
            VersionResolver.diagnose(options.gameDir, options.version);
            throw new IllegalStateException(describeMissing(options, resolved));
        }
        Log.info("using the " + resolved.layout + " layout in " + options.gameDir.getName());

        Map<String, Object> json = Json.readObject(
                new String(Files.readAllBytes(resolved.manifest.toPath()), StandardCharsets.UTF_8));
        json = inherit(json, options.gameDir, 0);

        String mainClass = String.valueOf(json.getOrDefault("mainClass", "net.minecraft.client.main.Main"));
        String assetIndex = assetIndex(json);

        List<File> libraryRoots = LibraryIndex.roots(options.gameDir,
                resolved.manifest.getParentFile(), resolved.libraries);
        LibraryIndex index = LibraryIndex.build(libraryRoots);

        List<String> classpath = classpath(resolved, json, index, librariesDir(resolved, options.gameDir));
        File natives = NativesExtractor.prepare(json, index, resolved.natives,
                options.gameDir, options.version);

        File java = chooseJava(options);

        List<String> command = new ArrayList<>();
        command.add(java.getPath());
        command.add("-Xmx" + options.ram + "M");
        command.add("-Xms" + Math.min(options.ram, 1024) + "M");
        command.add("-Dstorm.mcversion=" + options.version);
        command.add("-Djava.library.path=" + natives.getAbsolutePath());
        command.add("-Dminecraft.launcher.brand=storm");
        command.add("-Dminecraft.launcher.version=" + xyz.stormclient.launcher.StormLauncher.VERSION);
        command.add("-XX:+EnableDynamicAgentLoading");

        installBridgeAsMod(options);

        if (options.agentJar != null && options.agentJar.isFile()) {
            command.add("-javaagent:" + options.agentJar.getAbsolutePath()
                    + (options.agentOptions.isEmpty() ? "" : "=" + options.agentOptions));
            Log.info("agent " + options.agentJar);
            Log.info("bridge " + (options.bridgeJar != null && options.bridgeJar.isFile()
                    ? options.bridgeJar.toString()
                    : "not found, Storm will not load into the game"));
        } else {
            Log.warn("agent jar missing, the game starts without Storm");
        }

        command.add("-cp");
        command.add(String.join(File.pathSeparator, classpath));
        command.add(mainClass);

        command.addAll(gameArguments(json, options, assetIndex, resolved));

        Log.info("launching " + options.version + " with " + classpath.size() + " classpath entries");
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
    private static String describeMissing(Options options, VersionResolver.Resolved resolved) {
        if (resolved != null) {
            return options.version + " was found in " + options.gameDir.getName()
                    + " but its libraries are missing. Run it once in your own launcher,"
                    + " or use Inject instead. The console has the details.";
        }
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

    /**
     * Resolves inheritsFrom.
     *
     * <p>A Forge or Fabric profile lists only what it adds and points at the
     * version it builds on for everything else. Without following that link the
     * classpath comes out with a handful of entries and the game dies on a
     * missing vanilla class.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> inherit(Map<String, Object> child, File gameDir, int depth) {
        Object parentId = child.get("inheritsFrom");
        if (parentId == null || depth > 4) return child;

        VersionResolver.Resolved parentFile = VersionResolver.resolveBase(gameDir, String.valueOf(parentId));
        if (parentFile == null || parentFile.manifest == null) {
            Log.warn("this profile inherits from " + parentId + ", which is not installed");
            return child;
        }
        Log.info("inheriting from " + parentId + ": " + parentFile.manifest.getName());

        Map<String, Object> parent;
        try {
            parent = Json.readObject(new String(
                    Files.readAllBytes(parentFile.manifest.toPath()), StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.warn("could not read the inherited manifest: " + e);
            return child;
        }
        parent = inherit(parent, gameDir, depth + 1);

        Map<String, Object> merged = new LinkedHashMap<>(parent);
        for (Map.Entry<String, Object> entry : child.entrySet()) {
            if ("libraries".equals(entry.getKey())) continue;
            merged.put(entry.getKey(), entry.getValue());
        }

        // the child's libraries come first, so its versions win on the classpath
        List<Object> libraries = new ArrayList<>();
        Object childLibraries = child.get("libraries");
        Object parentLibraries = parent.get("libraries");
        if (childLibraries instanceof List) libraries.addAll((List<Object>) childLibraries);
        if (parentLibraries instanceof List) libraries.addAll((List<Object>) parentLibraries);
        merged.put("libraries", libraries);

        return merged;
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
    private static List<String> classpath(VersionResolver.Resolved resolved, Map<String, Object> json,
                                          LibraryIndex index, File librariesDir) {
        List<String> out = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Object rawLibraries = json.get("libraries");

        if (rawLibraries instanceof List) {
            for (Object entry : (List<Object>) rawLibraries) {
                if (!(entry instanceof Map)) continue;
                Map<String, Object> library = (Map<String, Object>) entry;

                String path = artifactPath(library);
                if (path == null) continue;

                File jar = index.find(path);
                if (jar == null) {
                    // the installer left it behind, so fetch it rather than
                    // handing the game a classpath with a hole in it
                    jar = LibraryDownloader.fetch(path, baseUrl(library), librariesDir);
                }
                if (jar != null) out.add(jar.getAbsolutePath());
                else missing.add(path);
            }
        }

        if (!missing.isEmpty()) {
            Log.warn(missing.size() + " libraries could not be found, for example:");
            for (int i = 0; i < Math.min(5, missing.size()); i++) Log.warn("  " + missing.get(i));
        }

        if (resolved.clientJar != null && resolved.clientJar.isFile()) {
            out.add(resolved.clientJar.getAbsolutePath());
        } else {
            Log.warn("no client jar found, the game will very likely not start");
        }
        return out;
    }

    /**
     * Picks a Java the chosen Minecraft can run on.
     *
     * <p>The configured one is kept when it fits. It usually does not: the
     * launcher itself needs 17 or newer, and a 1.8.9 needs 8, so the java on
     * the path is the wrong one about as often as it is right.
     */
    private static File chooseJava(Options options) {
        int wanted = JavaLocator.requiredFor(options.version);
        File configured = options.javaPath;

        if (configured != null && configured.isFile()) {
            int have = JavaLocator.versionOf(configured);
            if (have == wanted) return configured;
            Log.info("the configured Java is " + have + ", " + options.version + " needs " + wanted);
        }

        File match = JavaLocator.findMatching(wanted);
        if (match != null) {
            Log.info("using Java " + wanted + ": " + match);
            return match;
        }

        Log.warn("no Java " + wanted + " found on this machine, and " + options.version
                + " will very likely not start without one");
        if (wanted == 8) {
            Log.warn("install one with: winget install EclipseAdoptium.Temurin.8.JDK");
        }
        return configured != null && configured.isFile() ? configured : new File("java");
    }

    /**
     * Puts the bridge in the mods folder.
     *
     * <p>Under Forge the agent runs before launchwrapper has loaded a single
     * game class, so the bridge cannot install itself there. As a mod it is
     * loaded at the right moment by the mod loader instead, and the agent's
     * own attempt simply finds the work already done.
     */
    private static void installBridgeAsMod(Options options) {
        if (options.bridgeJar == null || !options.bridgeJar.isFile()) return;

        File mods = new File(options.gameDir, "mods");
        if (!mods.isDirectory() && !mods.mkdirs()) {
            Log.warn("could not create " + mods);
            return;
        }
        File target = new File(mods, options.bridgeJar.getName());
        if (target.isFile() && target.length() == options.bridgeJar.length()
                && target.lastModified() >= options.bridgeJar.lastModified()) {
            return;
        }
        try {
            Files.copy(options.bridgeJar.toPath(), target.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Log.info("bridge copied into " + mods.getName() + "/");
        } catch (IOException e) {
            Log.warn("could not copy the bridge into the mods folder: " + e);
        }
    }

    /** Where a downloaded library belongs. */
    private static File librariesDir(VersionResolver.Resolved resolved, File gameDir) {
        if (resolved.libraries != null && resolved.libraries.isDirectory()) return resolved.libraries;
        return new File(gameDir, "libraries");
    }

    /** The maven base a library names, empty when it comes from Mojang. */
    private static String baseUrl(Map<String, Object> library) {
        Object url = library.get("url");
        return url == null ? "" : String.valueOf(url);
    }

    /**
     * The manifest gives the path twice: once as a maven name and once, on
     * modern manifests, as an explicit download path. The explicit one wins
     * because it already carries the classifier.
     */
    @SuppressWarnings("unchecked")
    private static String artifactPath(Map<String, Object> library) {
        Object downloads = library.get("downloads");
        if (downloads instanceof Map) {
            Object artifact = ((Map<String, Object>) downloads).get("artifact");
            if (artifact instanceof Map) {
                Object path = ((Map<String, Object>) artifact).get("path");
                if (path != null) return String.valueOf(path);
            }
            // natives are extracted by the launcher, not put on the class path
            if (((Map<String, Object>) downloads).containsKey("classifiers")
                    && !((Map<String, Object>) downloads).containsKey("artifact")) {
                return null;
            }
        }
        if (library.containsKey("natives")) return null;

        Object name = library.get("name");
        return name == null ? null : mavenToPath(String.valueOf(name));
    }

    /** {@code group:artifact:version} -> {@code group/path/artifact/version/artifact-version.jar} */
    public static String mavenToPath(String name) {
        String[] parts = name.split(":");
        if (parts.length < 3) return name;
        String group = parts[0].replace('.', '/');
        String artifact = parts[1];
        String version = parts[2];
        String classifier = parts.length > 3 ? "-" + parts[3] : "";
        return group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + classifier + ".jar";
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
    private static List<String> gameArguments(Map<String, Object> json, Options options,
                                              String assetIndex, VersionResolver.Resolved resolved) {
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("auth_player_name", options.username);
        tokens.put("version_name", options.version);
        tokens.put("game_directory", options.gameDir.getAbsolutePath());
        tokens.put("assets_root", resolved.assets.getAbsolutePath());
        tokens.put("game_assets", new File(resolved.assets, "virtual/legacy").getAbsolutePath());
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
