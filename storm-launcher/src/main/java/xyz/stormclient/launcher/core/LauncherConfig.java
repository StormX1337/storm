package xyz.stormclient.launcher.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/** Launcher settings, stored next to the game directory. */
public final class LauncherConfig {

    private static final String FILE_NAME = "storm-launcher.properties";

    private final Properties properties = new Properties();
    private final File file;

    private LauncherConfig(File file) {
        this.file = file;
    }

    public static LauncherConfig load() {
        File file = new File(defaultGameDirectory(), FILE_NAME);
        LauncherConfig config = new LauncherConfig(file);
        if (file.isFile()) {
            try (FileInputStream in = new FileInputStream(file)) {
                config.properties.load(in);
                Log.info("loaded launcher settings from " + file);
            } catch (IOException e) {
                Log.warn("could not read " + file + ": " + e);
            }
        }
        return config;
    }

    public void save() {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            Log.warn("could not create " + parent);
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            properties.store(out, "Storm Launcher");
        } catch (IOException e) {
            Log.error("could not save launcher settings", e);
        }
    }

    // ------------------------------------------------------------------
    public String version()            { return get("version", "1.8.9"); }
    public void setVersion(String v)   { set("version", v); }

    public String username()           { return get("username", System.getProperty("user.name", "Player")); }
    public void setUsername(String v)  { set("username", v); }

    public int ram()                   { return getInt("ram", 4096); }
    public void setRam(int mb)         { set("ram", String.valueOf(mb)); }

    public String javaPath()           { return get("java", defaultJava()); }
    public void setJavaPath(String v)  { set("java", v); }

    public String gameDirectory() {
        String stored = properties.getProperty("gameDir");
        if (stored != null) return stored;

        // nothing configured yet: use whatever installation actually has versions
        GameDirectories.Install preferred = GameDirectories.preferred();
        return preferred != null ? preferred.root.getAbsolutePath()
                                 : defaultGameDirectory().getAbsolutePath();
    }
    public void setGameDirectory(String v) { set("gameDir", v); }

    public String agentJar()           { return get("agent", LauncherPaths.defaultAgentJar().getAbsolutePath()); }
    public void setAgentJar(String v)  { set("agent", v); }

    public String configProfile()          { return get("profile", "default"); }
    public void setConfigProfile(String v) { set("profile", v); }

    public boolean autoInject()             { return getBool("autoInject", true); }
    public void setAutoInject(boolean v)    { set("autoInject", String.valueOf(v)); }

    public boolean debug()                  { return getBool("debug", false); }
    public void setDebug(boolean v)         { set("debug", String.valueOf(v)); }

    public boolean closeOnLaunch()          { return getBool("closeOnLaunch", false); }
    public void setCloseOnLaunch(boolean v) { set("closeOnLaunch", String.valueOf(v)); }

    // ------------------------------------------------------------------
    private String get(String key, String fallback) { return properties.getProperty(key, fallback); }
    private void set(String key, String value)      { properties.setProperty(key, value); }

    private int getInt(String key, int fallback) {
        try { return Integer.parseInt(properties.getProperty(key, String.valueOf(fallback))); }
        catch (NumberFormatException e) { return fallback; }
    }

    private boolean getBool(String key, boolean fallback) {
        return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(fallback)));
    }

    public static File defaultGameDirectory() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home", ".");
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return new File(appData == null ? home : appData, ".minecraft");
        }
        if (os.contains("mac")) {
            return new File(home, "Library/Application Support/minecraft");
        }
        return new File(home, ".minecraft");
    }

    public static String defaultJava() {
        File java = new File(System.getProperty("java.home", ""), "bin/java");
        return java.isFile() ? java.getAbsolutePath() : "java";
    }
}
