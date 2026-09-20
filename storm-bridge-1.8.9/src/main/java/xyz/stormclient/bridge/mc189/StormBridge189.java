package xyz.stormclient.bridge.mc189;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;

import xyz.stormclient.StormBoot;
import xyz.stormclient.util.StormLogger;

/**
 * Entry point of the Minecraft 1.8.9 bridge.
 *
 * <p>The agent calls {@link #install()} through reflection, a Forge mod build
 * calls it from its init handler. Either way this is the only place that
 * decides when the client core comes up.
 */
public final class StormBridge189 {

    private static boolean installed;
    private static StormEventHooks hooks;

    private StormBridge189() { }

    public static synchronized void install() {
        if (installed) {
            StormLogger.warn("the 1.8.9 bridge is already installed");
            return;
        }
        installed = true;

        if (!forgePresent()) {
            StormLogger.error("this bridge needs Forge 1.8.9, and this game is running without it.");
            StormLogger.error("install Forge 1.8.9 and start that profile, see docs/BRIDGE-BUILD.md");
            installed = false;
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            StormLogger.error("Minecraft has not been created yet, install the bridge later in the boot");
            installed = false;
            return;
        }

        StormBoot.boot(new Mc189Minecraft(mc));

        hooks = new StormEventHooks();
        MinecraftForge.EVENT_BUS.register(hooks);
        FMLCommonHandler.instance().bus().register(hooks);

        Runtime.getRuntime().addShutdownHook(new Thread(StormBoot::shutdown, "Storm-Shutdown"));
        StormLogger.info("1.8.9 bridge installed");
    }

    /**
     * The event hooks are Forge events, so a vanilla game would only fail later
     * with a NoClassDefFoundError deep inside the first tick. Checking up front
     * turns that into one readable line.
     */
    private static boolean forgePresent() {
        try {
            Class.forName("net.minecraftforge.common.MinecraftForge", false,
                    StormBridge189.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean installed() { return installed; }

    public static StormEventHooks hooks() { return hooks; }
}
