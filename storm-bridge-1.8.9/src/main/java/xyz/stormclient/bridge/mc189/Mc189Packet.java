package xyz.stormclient.bridge.mc189;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;

import xyz.stormclient.bridge.IPacket;
import xyz.stormclient.bridge.PacketType;

/**
 * Wraps a vanilla packet behind the version neutral interface.
 * Field access goes through reflection once per field and is then cached, which
 * is cheap enough for the handful of packets Storm actually rewrites.
 */
public final class Mc189Packet implements IPacket {

    private static final Map<String, Field> FIELDS = new HashMap<String, Field>();

    private final Packet<?> packet;
    private final PacketType type;

    public Mc189Packet(Packet<?> packet) {
        this.packet = packet;
        this.type = classify(packet);
    }

    public Packet<?> handle() { return packet; }

    @Override public PacketType type()  { return type; }
    @Override public String rawName()   { return packet.getClass().getSimpleName(); }
    @Override public Object getRaw()    { return packet; }

    private static PacketType classify(Packet<?> packet) {
        if (packet instanceof C03PacketPlayer.C06PacketPlayerPosLook) return PacketType.C_PLAYER_POSITION_LOOK;
        if (packet instanceof C03PacketPlayer.C04PacketPlayerPosition) return PacketType.C_PLAYER_POSITION;
        if (packet instanceof C03PacketPlayer.C05PacketPlayerLook)     return PacketType.C_PLAYER_LOOK;
        if (packet instanceof C03PacketPlayer)            return PacketType.C_PLAYER;
        if (packet instanceof C02PacketUseEntity)         return PacketType.C_USE_ENTITY;
        if (packet instanceof C0APacketAnimation)         return PacketType.C_ANIMATION;
        if (packet instanceof C0BPacketEntityAction)      return PacketType.C_ENTITY_ACTION;
        if (packet instanceof C08PacketPlayerBlockPlacement) return PacketType.C_BLOCK_PLACE;
        if (packet instanceof C07PacketPlayerDigging)     return PacketType.C_BLOCK_DIG;
        if (packet instanceof C09PacketHeldItemChange)    return PacketType.C_HELD_ITEM_CHANGE;
        if (packet instanceof C01PacketChatMessage)       return PacketType.C_CHAT;
        if (packet instanceof C00PacketKeepAlive)         return PacketType.C_KEEP_ALIVE;
        if (packet instanceof C0EPacketClickWindow)       return PacketType.C_WINDOW_CLICK;
        if (packet instanceof C0DPacketCloseWindow)       return PacketType.C_CLOSE_WINDOW;
        if (packet instanceof C0FPacketConfirmTransaction) return PacketType.C_CONFIRM_TRANSACTION;

        if (packet instanceof S08PacketPlayerPosLook)     return PacketType.S_PLAYER_POS_LOOK;
        if (packet instanceof S12PacketEntityVelocity)    return PacketType.S_ENTITY_VELOCITY;
        if (packet instanceof S27PacketExplosion)         return PacketType.S_EXPLOSION;
        if (packet instanceof S19PacketEntityStatus)      return PacketType.S_ENTITY_STATUS;
        if (packet instanceof S02PacketChat)              return PacketType.S_CHAT;
        if (packet instanceof S45PacketTitle)             return PacketType.S_TITLE;
        if (packet instanceof S00PacketKeepAlive)         return PacketType.S_KEEP_ALIVE;
        if (packet instanceof S40PacketDisconnect)        return PacketType.S_DISCONNECT;
        if (packet instanceof S0CPacketSpawnPlayer)       return PacketType.S_SPAWN_PLAYER;
        if (packet instanceof S0EPacketSpawnObject)       return PacketType.S_SPAWN_ENTITY;
        if (packet instanceof S13PacketDestroyEntities)   return PacketType.S_DESTROY_ENTITIES;
        if (packet instanceof S06PacketUpdateHealth)      return PacketType.S_UPDATE_HEALTH;
        if (packet instanceof S2DPacketOpenWindow)        return PacketType.S_OPEN_WINDOW;
        if (packet instanceof S30PacketWindowItems)       return PacketType.S_WINDOW_ITEMS;
        if (packet instanceof S32PacketConfirmTransaction) return PacketType.S_CONFIRM_TRANSACTION;
        if (packet instanceof S23PacketBlockChange)       return PacketType.S_BLOCK_CHANGE;
        if (packet instanceof S18PacketEntityTeleport)    return PacketType.S_TELEPORT;
        if (packet instanceof S07PacketRespawn)           return PacketType.S_RESPAWN;
        return PacketType.OTHER;
    }

    // ------------------------------------------------------------------
    //  field access
    // ------------------------------------------------------------------
    private Field field(String name) {
        String key = packet.getClass().getName() + "#" + name;
        Field cached = FIELDS.get(key);
        if (cached != null) return cached;

        for (Class<?> c = packet.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field candidate : c.getDeclaredFields()) {
                if (!matches(candidate.getName(), name)) continue;
                candidate.setAccessible(true);
                FIELDS.put(key, candidate);
                return candidate;
            }
        }
        return null;
    }

    /** Accepts both MCP names and the searge names a production mapping produces. */
    private boolean matches(String actual, String wanted) {
        if (actual.equalsIgnoreCase(wanted)) return true;
        String simple = actual.toLowerCase();
        String target = wanted.toLowerCase();
        return simple.endsWith("_" + target) || simple.equals(target)
                || (simple.startsWith("field_") && simple.endsWith(target));
    }

    @Override public double getDouble(String name) {
        Field f = field(name);
        try { return f == null ? 0 : f.getDouble(packet); } catch (Exception e) { return 0; }
    }

    @Override public float getFloat(String name) {
        Field f = field(name);
        try { return f == null ? 0 : f.getFloat(packet); } catch (Exception e) { return 0; }
    }

    @Override public int getInt(String name) {
        Field f = field(name);
        try { return f == null ? 0 : f.getInt(packet); } catch (Exception e) { return 0; }
    }

    @Override public boolean getBoolean(String name) {
        Field f = field(name);
        try { return f != null && f.getBoolean(packet); } catch (Exception e) { return false; }
    }

    @Override public String getString(String name) {
        Field f = field(name);
        try { return f == null ? "" : String.valueOf(f.get(packet)); } catch (Exception e) { return ""; }
    }

    @Override public void setDouble(String name, double value) {
        Field f = field(name);
        try { if (f != null) f.setDouble(packet, value); } catch (Exception ignored) { }
    }

    @Override public void setFloat(String name, float value) {
        Field f = field(name);
        try { if (f != null) f.setFloat(packet, value); } catch (Exception ignored) { }
    }

    @Override public void setInt(String name, int value) {
        Field f = field(name);
        try { if (f != null) f.setInt(packet, value); } catch (Exception ignored) { }
    }

    @Override public void setBoolean(String name, boolean value) {
        Field f = field(name);
        try { if (f != null) f.setBoolean(packet, value); } catch (Exception ignored) { }
    }
}
