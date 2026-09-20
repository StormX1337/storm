package xyz.stormclient;

import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.bridge.IMinecraft;
import xyz.stormclient.util.StormLogger;

/**
 * Single entry point every bridge and the agent call.
 * Keeping it here means the loader side never has to know about the internals.
 */
public final class StormBoot {

    private StormBoot() { }

    public static synchronized void boot(IMinecraft implementation) {
        if (Storm.get().initialised()) {
            StormLogger.warn("Storm is already running");
            return;
        }
        try {
            Bridge.install(implementation);
            Storm.get().start();
        } catch (Throwable t) {
            StormLogger.error("failed to start Storm", t);
            throw new IllegalStateException("Storm failed to start", t);
        }
    }

    public static synchronized void shutdown() {
        Storm.get().shutdown();
    }
}
