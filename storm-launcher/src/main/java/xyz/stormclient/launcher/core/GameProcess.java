package xyz.stormclient.launcher.core;

/** A running JVM the injector can attach to. */
public final class GameProcess {

    private final String pid;
    private final String displayName;
    private final String detectedVersion;
    private final boolean minecraft;

    public GameProcess(String pid, String displayName, String detectedVersion, boolean minecraft) {
        this.pid = pid;
        this.displayName = displayName == null || displayName.isEmpty() ? "java" : displayName;
        this.detectedVersion = detectedVersion;
        this.minecraft = minecraft;
    }

    public String pid()             { return pid; }
    public String displayName()     { return displayName; }
    public String detectedVersion() { return detectedVersion; }
    public boolean minecraft()      { return minecraft; }

    public String shortName() {
        String name = displayName;
        int space = name.indexOf(' ');
        if (space > 0) name = name.substring(0, space);
        int dot = name.lastIndexOf('.');
        return dot > 0 && dot < name.length() - 1 ? name.substring(dot + 1) : name;
    }

    @Override public String toString() { return pid + "  " + shortName(); }
}
