package xyz.stormclient.bridge.mc189;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import xyz.stormclient.util.StormLogger;

/**
 * Reaches the handful of Minecraft fields that are not public.
 *
 * <p>Both the readable name and the searge name are tried, so the same build
 * works in a development workspace and against a production mapping.
 */
public final class Mc189Reflect {

    private static final Map<String, Field> CACHE = new HashMap<String, Field>();

    private Mc189Reflect() { }

    public static Field field(Class<?> owner, String... names) {
        String key = owner.getName() + "#" + names[0];
        if (CACHE.containsKey(key)) return CACHE.get(key);

        Field found = null;
        for (String name : names) {
            try {
                found = owner.getDeclaredField(name);
                found.setAccessible(true);
                break;
            } catch (NoSuchFieldException ignored) {
                // try the next name
            }
        }
        if (found == null) {
            StormLogger.warn("field " + names[0] + " not found on " + owner.getSimpleName());
        }
        CACHE.put(key, found);
        return found;
    }

    public static boolean getBoolean(Field field, Object instance, boolean fallback) {
        if (field == null || instance == null) return fallback;
        try {
            return field.getBoolean(instance);
        } catch (IllegalAccessException e) {
            return fallback;
        }
    }
}
