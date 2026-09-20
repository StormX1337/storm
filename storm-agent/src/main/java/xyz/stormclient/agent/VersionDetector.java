package xyz.stormclient.agent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import xyz.stormclient.bridge.GameVersion;
import xyz.stormclient.util.StormLogger;

/**
 * Works out which Minecraft the agent landed in.
 * Four strategies, cheapest and most reliable first.
 */
public final class VersionDetector {

    private VersionDetector() { }

    public static GameVersion detect(GameVersion forced) {
        if (forced != null && forced != GameVersion.UNKNOWN) {
            StormLogger.debug("version forced to " + forced.id());
            return forced;
        }

        GameVersion version = fromSystemProperty();
        if (version != GameVersion.UNKNOWN) return version;

        version = fromSharedConstants();
        if (version != GameVersion.UNKNOWN) return version;

        version = fromForge();
        if (version != GameVersion.UNKNOWN) return version;

        return fromMarkerClasses();
    }

    /** The launcher sets this before starting the game. */
    private static GameVersion fromSystemProperty() {
        String id = System.getProperty("storm.mcversion", "");
        return id.isEmpty() ? GameVersion.UNKNOWN : GameVersion.fromId(id);
    }

    /** 1.14+ exposes the running version through SharedConstants. */
    private static GameVersion fromSharedConstants() {
        try {
            Class<?> shared = Class.forName("net.minecraft.SharedConstants");
            for (Method method : shared.getDeclaredMethods()) {
                if (method.getParameterTypes().length != 0) continue;
                if (!method.getReturnType().getName().contains("GameVersion")
                        && !method.getReturnType().getName().contains("MinecraftVersion")) continue;

                method.setAccessible(true);
                Object gameVersion = method.invoke(null);
                if (gameVersion == null) continue;

                for (Method inner : gameVersion.getClass().getMethods()) {
                    if (inner.getParameterTypes().length != 0) continue;
                    if (inner.getReturnType() != String.class) continue;
                    if (!inner.getName().toLowerCase().contains("name")) continue;

                    inner.setAccessible(true);
                    GameVersion found = GameVersion.fromId(String.valueOf(inner.invoke(gameVersion)));
                    if (found != GameVersion.UNKNOWN) return found;
                }
            }
        } catch (Throwable ignored) { }
        return GameVersion.UNKNOWN;
    }

    /** Forge keeps the target version in a constant, which covers 1.7 to 1.12. */
    private static GameVersion fromForge() {
        for (String name : new String[] { "net.minecraftforge.common.ForgeVersion",
                                          "net.minecraftforge.versions.mcp.MCPVersion" }) {
            try {
                Class<?> forge = Class.forName(name);
                for (Field field : forge.getDeclaredFields()) {
                    if (field.getType() != String.class) continue;
                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value == null) continue;
                    GameVersion found = GameVersion.fromId(String.valueOf(value));
                    if (found != GameVersion.UNKNOWN) return found;
                }
            } catch (Throwable ignored) { }
        }
        return GameVersion.UNKNOWN;
    }

    /**
     * Last resort: look for classes that only exist in a certain era.
     * This cannot tell 1.8 from 1.8.9, so it reports the release Storm supports.
     */
    private static GameVersion fromMarkerClasses() {
        if (present("net.minecraft.world.level.block.state.BlockState"))  return GameVersion.V1_20_4;
        if (present("net.minecraft.client.gui.screens.TitleScreen"))      return GameVersion.V1_18_2;
        if (present("net.minecraft.client.gui.screen.MainMenuScreen"))    return GameVersion.V1_16_5;
        if (present("net.minecraft.util.math.BlockPos")
                && present("net.minecraft.client.renderer.chunk.ChunkRenderDispatcher")) return GameVersion.V1_12_2;
        if (present("net.minecraft.util.BlockPos"))                       return GameVersion.V1_8_9;
        if (present("net.minecraft.client.entity.EntityClientPlayerMP"))  return GameVersion.V1_7_10;
        return GameVersion.UNKNOWN;
    }

    private static boolean present(String name) {
        try {
            Class.forName(name, false, GameClassLoader.get());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
