package xyz.stormclient.agent;

import java.lang.reflect.Field;

import xyz.stormclient.util.StormLogger;

/**
 * Finds the class loader that actually holds the game classes.
 * Legacy versions run under LaunchWrapper, modern ones under the mod loader's
 * own loader, and a vanilla jar simply uses the system loader.
 */
public final class GameClassLoader {

    private static ClassLoader cached;

    private GameClassLoader() { }

    public static synchronized ClassLoader get() {
        if (cached != null) return cached;

        cached = fromLaunchWrapper();
        if (cached != null) {
            StormLogger.debug("using the LaunchWrapper class loader");
            return cached;
        }
        cached = fromKnownClass("net.minecraft.client.Minecraft");
        if (cached != null) {
            StormLogger.debug("using the Minecraft class loader");
            return cached;
        }
        cached = Thread.currentThread().getContextClassLoader();
        if (cached == null) cached = ClassLoader.getSystemClassLoader();
        StormLogger.debug("falling back to the context class loader");
        return cached;
    }

    private static ClassLoader fromLaunchWrapper() {
        try {
            Class<?> launch = Class.forName("net.minecraft.launchwrapper.Launch");
            Field field = launch.getDeclaredField("classLoader");
            field.setAccessible(true);
            return (ClassLoader) field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ClassLoader fromKnownClass(String name) {
        for (ClassLoader loader : new ClassLoader[] {
                Thread.currentThread().getContextClassLoader(),
                ClassLoader.getSystemClassLoader() }) {
            if (loader == null) continue;
            try {
                return Class.forName(name, false, loader).getClassLoader();
            } catch (Throwable ignored) { }
        }
        return null;
    }
}
