package xyz.stormclient.launcher.core;

import java.util.ArrayList;
import java.util.List;

import xyz.stormclient.launcher.core.MinecraftVersion.Loader;
import xyz.stormclient.launcher.core.MinecraftVersion.Support;

/** Everything the launcher offers, newest first is not the point here - PvP versions come first. */
public final class VersionRegistry {

    private static final List<MinecraftVersion> VERSIONS = new ArrayList<>();

    static {
        add("1.8.9",  47,  Support.FULL,    Loader.FORGE,  "The PvP standard, primary target");
        add("1.7.10", 5,   Support.BETA,    Loader.FORGE,  "Legacy combat, no off hand");
        add("1.12.2", 340, Support.BETA,    Loader.FORGE,  "Last of the pre flattening builds");
        add("1.16.5", 754, Support.BETA,    Loader.FABRIC, "Popular for modded SMP");
        add("1.18.2", 758, Support.PLANNED, Loader.FABRIC, "Bridge in progress");
        add("1.19.4", 762, Support.PLANNED, Loader.FABRIC, "Bridge in progress");
        add("1.20.6", 766, Support.PLANNED, Loader.FABRIC, "Bridge in progress");
        add("1.21.4", 769, Support.PLANNED, Loader.FABRIC, "Bridge in progress");
    }

    private VersionRegistry() { }

    private static void add(String id, int protocol, Support support, Loader loader, String note) {
        VERSIONS.add(new MinecraftVersion(id, protocol, support, loader, note));
    }

    public static List<MinecraftVersion> all() { return VERSIONS; }

    public static List<MinecraftVersion> playable() {
        List<MinecraftVersion> out = new ArrayList<>();
        for (MinecraftVersion v : VERSIONS) if (v.playable()) out.add(v);
        return out;
    }

    public static MinecraftVersion byId(String id) {
        for (MinecraftVersion v : VERSIONS) if (v.id().equals(id)) return v;
        return VERSIONS.get(0);
    }
}
