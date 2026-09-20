package xyz.stormclient.agent;

import java.util.HashMap;
import java.util.Map;

import xyz.stormclient.bridge.GameVersion;

/** {@code key=value;key=value} options handed over by the launcher. */
public final class AgentOptions {

    private final Map<String, String> values = new HashMap<String, String>();

    private AgentOptions() { }

    public static AgentOptions parse(String raw) {
        AgentOptions options = new AgentOptions();
        if (raw == null || raw.isEmpty()) return options;

        for (String pair : raw.split(";")) {
            int split = pair.indexOf('=');
            if (split <= 0) {
                options.values.put(pair.trim().toLowerCase(), "true");
            } else {
                options.values.put(pair.substring(0, split).trim().toLowerCase(), pair.substring(split + 1).trim());
            }
        }
        return options;
    }

    public String get(String key, String fallback) {
        String value = values.get(key.toLowerCase());
        return value == null ? fallback : value;
    }

    public boolean flag(String key) { return Boolean.parseBoolean(get(key, "false")); }

    public boolean debug()      { return flag("debug"); }
    public String bridgeJar()   { return get("bridge", null); }
    public String configName()  { return get("config", "default"); }

    public GameVersion forcedVersion() {
        String id = get("version", null);
        return id == null ? GameVersion.UNKNOWN : GameVersion.fromId(id);
    }

    @Override public String toString() { return values.toString(); }
}
