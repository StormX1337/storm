package xyz.stormclient.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import xyz.stormclient.Storm;
import xyz.stormclient.StormInfo;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.Setting;
import xyz.stormclient.util.Json;
import xyz.stormclient.util.StormLogger;

/**
 * Profile based config storage. Everything lives in
 * {@code .minecraft/storm/configs/<name>.json} as plain readable JSON so a
 * config can be shared by copying one file.
 */
public final class ConfigManager {

    private static final String DEFAULT = "default";

    private final File directory;
    private String current = DEFAULT;

    public ConfigManager(File gameDirectory) {
        this.directory = new File(gameDirectory, "storm/configs");
        if (!directory.exists() && !directory.mkdirs()) {
            StormLogger.warn("could not create " + directory);
        }
    }

    public File directory()     { return directory; }
    public String currentName() { return current; }

    public List<String> profiles() {
        List<String> out = new ArrayList<String>();
        File[] files = directory.listFiles();
        if (files == null) return out;
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".json")) {
                out.add(f.getName().substring(0, f.getName().length() - 5));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    //  saving
    // ------------------------------------------------------------------
    public boolean saveCurrent() { return save(current); }

    public boolean save(String name) {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("client", StormInfo.NAME);
        root.put("version", StormInfo.VERSION);
        root.put("saved", System.currentTimeMillis());

        Map<String, Object> modules = new LinkedHashMap<String, Object>();
        for (Module module : Storm.get().modules().all()) {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("enabled", module.isEnabled());

            Map<String, Object> settings = new LinkedHashMap<String, Object>();
            for (Setting<?> setting : module.settings()) {
                settings.put(setting.name(), setting.serialize());
            }
            entry.put("settings", settings);
            modules.put(module.name(), entry);
        }
        root.put("modules", modules);

        Map<String, Object> friends = new LinkedHashMap<String, Object>();
        friends.putAll(Storm.get().friends().entries());
        root.put("friends", friends);

        Map<String, Object> theme = new LinkedHashMap<String, Object>();
        theme.put("preset", Storm.get().theme().preset().name());
        theme.put("accent", String.format("%08X", Storm.get().theme().accent()));
        theme.put("rainbow", Storm.get().theme().rainbow());
        root.put("theme", theme);

        File file = new File(directory, name + ".json");
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8"));
            writer.write(Json.write(root));
            current = name;
            StormLogger.debug("saved config " + name);
            return true;
        } catch (IOException e) {
            StormLogger.error("could not save config " + name, e);
            return false;
        } finally {
            close(writer);
        }
    }

    // ------------------------------------------------------------------
    //  loading
    // ------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public boolean load(String name) {
        File file = new File(directory, name + ".json");
        if (!file.exists()) {
            StormLogger.warn("config " + name + " does not exist");
            return false;
        }
        try {
            String raw = new String(Files.readAllBytes(file.toPath()), Charset.forName("UTF-8"));
            Map<String, Object> root = Json.readObject(raw);

            Object modulesRaw = root.get("modules");
            if (modulesRaw instanceof Map) {
                Map<String, Object> modules = (Map<String, Object>) modulesRaw;
                for (Module module : Storm.get().modules().all()) {
                    Object entryRaw = modules.get(module.name());
                    if (!(entryRaw instanceof Map)) continue;
                    Map<String, Object> entry = (Map<String, Object>) entryRaw;

                    Object settingsRaw = entry.get("settings");
                    if (settingsRaw instanceof Map) {
                        Map<String, Object> settings = (Map<String, Object>) settingsRaw;
                        for (Setting<?> setting : module.settings()) {
                            Object value = settings.get(setting.name());
                            if (value != null) setting.deserialize(String.valueOf(value));
                        }
                    }
                    boolean enabled = Boolean.TRUE.equals(entry.get("enabled"));
                    module.setEnabled(enabled);
                }
            }

            Storm.get().friends().clear();
            Object friendsRaw = root.get("friends");
            if (friendsRaw instanceof Map) {
                for (Map.Entry<String, Object> e : ((Map<String, Object>) friendsRaw).entrySet()) {
                    Storm.get().friends().add(e.getKey(), String.valueOf(e.getValue()));
                }
            }

            Object themeRaw = root.get("theme");
            if (themeRaw instanceof Map) {
                Map<String, Object> theme = (Map<String, Object>) themeRaw;
                Object preset = theme.get("preset");
                if (preset != null) {
                    try {
                        Storm.get().theme().setPreset(
                                xyz.stormclient.ui.theme.Theme.Preset.valueOf(String.valueOf(preset)));
                    } catch (IllegalArgumentException ignored) { }
                }
                Object accent = theme.get("accent");
                if (accent != null) {
                    Storm.get().theme().setAccent((int) Long.parseLong(String.valueOf(accent), 16));
                }
                Storm.get().theme().setRainbow(Boolean.TRUE.equals(theme.get("rainbow")));
            }

            current = name;
            StormLogger.info("loaded config " + name);
            return true;
        } catch (Exception e) {
            StormLogger.error("could not load config " + name, e);
            return false;
        }
    }

    public boolean loadOrCreateDefault() {
        File file = new File(directory, DEFAULT + ".json");
        if (file.exists()) return load(DEFAULT);
        return save(DEFAULT);
    }

    public boolean delete(String name) {
        File file = new File(directory, name + ".json");
        return file.exists() && file.delete();
    }

    public boolean create(String name) {
        if (profiles().contains(name)) return false;
        return save(name);
    }

    private void close(Writer writer) {
        if (writer == null) return;
        try { writer.close(); } catch (IOException ignored) { }
    }
}
