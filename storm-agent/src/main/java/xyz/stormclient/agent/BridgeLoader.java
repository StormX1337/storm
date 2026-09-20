package xyz.stormclient.agent;

import java.lang.reflect.Method;

import xyz.stormclient.bridge.GameVersion;
import xyz.stormclient.util.StormLogger;

/**
 * Maps a Minecraft version onto the bridge that drives it and starts it.
 * A bridge only has to expose {@code public static void install()}.
 */
public final class BridgeLoader {

    private BridgeLoader() { }

    public static boolean load(GameVersion version, AgentOptions options) {
        String className = bridgeClass(version);
        if (className == null) {
            StormLogger.error("no bridge is shipped for " + version.id());
            return false;
        }

        try {
            Class<?> bridge = Class.forName(className, true, GameClassLoader.get());
            Method install = bridge.getMethod("install");
            Object result = install.invoke(null);

            // a bridge that cannot run in this game says so by returning false,
            // and reporting it as installed anyway helps nobody
            if (Boolean.FALSE.equals(result)) {
                StormLogger.error("bridge " + className + " declined to install, Storm is not active");
                return false;
            }
            StormLogger.info("bridge " + className + " installed");
            return true;
        } catch (ClassNotFoundException e) {
            StormLogger.error("bridge " + className + " is not on the class path. "
                    + "Pass bridge=<path to jar> to the agent or install it as a mod.");
        } catch (NoSuchMethodException e) {
            StormLogger.error(className + " does not expose install()");
        } catch (Throwable t) {
            Throwable cause = t.getCause() == null ? t : t.getCause();

            // Under a mod loader the agent runs before any game class exists.
            // That is not a failure, it just means the bridge has to be loaded
            // by the mod loader, from the mods folder.
            if (cause instanceof NoClassDefFoundError || cause instanceof ClassNotFoundException) {
                StormLogger.info("the game is not up yet, so the bridge will install itself"
                        + " once the mod loader reaches it");
                return true;
            }
            StormLogger.error("bridge " + className + " failed to install", cause);
        }
        return false;
    }

    public static String bridgeClass(GameVersion version) {
        switch (version) {
            case V1_7_10: return "xyz.stormclient.bridge.mc1710.StormBridge1710";
            case V1_8_9:  return "xyz.stormclient.bridge.mc189.StormBridge189";
            case V1_12_2: return "xyz.stormclient.bridge.mc1122.StormBridge1122";
            case V1_16_5: return "xyz.stormclient.bridge.mc1165.StormBridge1165";
            case V1_18_2:
            case V1_19_4:
            case V1_20_4:
            case V1_20_6:
            case V1_21_4: return "xyz.stormclient.bridge.modern.StormBridgeModern";
            default:      return null;
        }
    }
}
