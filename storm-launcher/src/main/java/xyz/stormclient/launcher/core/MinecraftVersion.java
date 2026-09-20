package xyz.stormclient.launcher.core;

/** One entry in the version picker. */
public final class MinecraftVersion {

    public enum Support {
        FULL      ("Full support",  0xFF45E08A),
        BETA      ("Beta",          0xFFFFB648),
        PLANNED   ("Planned",       0xFF7A8698);

        public final String label;
        public final int color;

        Support(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    public enum Loader { VANILLA, FORGE, FABRIC }

    private final String id;
    private final int protocol;
    private final Support support;
    private final Loader loader;
    private final String note;

    public MinecraftVersion(String id, int protocol, Support support, Loader loader, String note) {
        this.id = id;
        this.protocol = protocol;
        this.support = support;
        this.loader = loader;
        this.note = note;
    }

    public String id()       { return id; }
    public int protocol()    { return protocol; }
    public Support support() { return support; }
    public Loader loader()   { return loader; }
    public String note()     { return note; }

    public boolean playable() { return support != Support.PLANNED; }

    public String bridgeJarName() { return "storm-bridge-" + id + ".jar"; }

    @Override public String toString() { return id; }
}
