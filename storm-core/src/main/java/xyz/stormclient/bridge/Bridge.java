package xyz.stormclient.bridge;

/**
 * Static access point to the version specific implementation.
 * A bridge module calls {@link #install(IMinecraft)} once during boot,
 * from then on the whole client just uses {@code Bridge.mc()}.
 */
public final class Bridge {

    private static IMinecraft mc;

    private Bridge() { }

    public static void install(IMinecraft impl) {
        if (impl == null) throw new IllegalArgumentException("bridge impl is null");
        mc = impl;
    }

    public static boolean installed() { return mc != null; }

    public static IMinecraft mc() {
        if (mc == null) throw new IllegalStateException("Storm bridge not installed yet");
        return mc;
    }

    public static GameVersion version() {
        return mc == null ? GameVersion.UNKNOWN : mc.version();
    }
}
