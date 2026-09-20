package xyz.stormclient.bridge.mc189;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;

import xyz.stormclient.bridge.IEntity;

/**
 * Entity wrappers are created many times per frame, so they are cached against
 * the entity itself. A weak map keeps the cache from holding entities alive
 * after they leave the world.
 */
public final class Mc189Entities {

    private static final Map<Entity, IEntity> CACHE = new WeakHashMap<Entity, IEntity>();

    private Mc189Entities() { }

    public static IEntity wrap(Entity entity) {
        if (entity == null) return null;

        IEntity cached = CACHE.get(entity);
        if (cached != null) return cached;

        IEntity wrapper = entity == Minecraft.getMinecraft().thePlayer
                ? new Mc189Player((EntityPlayerSP) entity)
                : new Mc189Entity(entity);
        CACHE.put(entity, wrapper);
        return wrapper;
    }

    public static Entity unwrap(IEntity entity) {
        return entity instanceof Mc189Entity ? ((Mc189Entity) entity).handle() : null;
    }

    public static void clear() { CACHE.clear(); }
}
