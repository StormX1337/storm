package xyz.stormclient.bridge.mc189;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;

import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.bridge.INetwork;
import xyz.stormclient.bridge.IPacket;
import xyz.stormclient.util.StormLogger;

public final class Mc189Network implements INetwork {

    private final Minecraft mc;

    public Mc189Network(Minecraft mc) { this.mc = mc; }

    private NetHandlerPlayClient handler() { return mc.getNetHandler(); }

    @Override public void send(IPacket packet) {
        if (handler() == null || !(packet instanceof Mc189Packet)) return;
        handler().addToSendQueue(((Mc189Packet) packet).handle());
    }

    /** Bypasses the outgoing event by writing straight into the channel. */
    @Override public void sendSilent(IPacket packet) {
        if (handler() == null || !(packet instanceof Mc189Packet)) return;
        StormEventHooks.withoutEvents(() -> handler().getNetworkManager()
                .sendPacket(((Mc189Packet) packet).handle()));
    }

    @Override public void receive(IPacket packet) {
        if (handler() == null || !(packet instanceof Mc189Packet)) return;
        try {
            Packet<?> raw = ((Mc189Packet) packet).handle();

            // processPacket is declared with the concrete handler type, so it
            // cannot be looked up by INetHandler.class
            for (java.lang.reflect.Method method : raw.getClass().getMethods()) {
                if (!"processPacket".equals(method.getName())) continue;
                if (method.getParameterTypes().length != 1) continue;
                method.invoke(raw, handler());
                return;
            }
            StormLogger.debug("no processPacket on " + packet.rawName());
        } catch (Throwable t) {
            StormLogger.debug("could not replay " + packet.rawName() + ": " + t);
        }
    }

    @Override public int ping() {
        if (handler() == null || mc.thePlayer == null) return 0;
        NetworkPlayerInfo info = handler().getPlayerInfo(mc.thePlayer.getUniqueID());
        return info == null ? 0 : info.getResponseTime();
    }

    @Override public boolean connected() {
        return handler() != null && handler().getNetworkManager().isChannelOpen();
    }

    @Override public void sendChat(String message) {
        if (mc.thePlayer == null || message == null || message.isEmpty()) return;
        mc.thePlayer.sendChatMessage(message.length() > 100 ? message.substring(0, 100) : message);
    }

    // ------------------------------------------------------------------
    //  packet factory
    // ------------------------------------------------------------------
    @Override public IPacket createRotationPacket(float yaw, float pitch, boolean onGround) {
        return new Mc189Packet(new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, onGround));
    }

    @Override public IPacket createPositionPacket(double x, double y, double z, boolean onGround) {
        return new Mc189Packet(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, onGround));
    }

    @Override public IPacket createFullPacket(double x, double y, double z,
                                              float yaw, float pitch, boolean onGround) {
        return new Mc189Packet(new C03PacketPlayer.C06PacketPlayerPosLook(x, y, z, yaw, pitch, onGround));
    }

    @Override public IPacket createOnGroundPacket(boolean onGround) {
        return new Mc189Packet(new C03PacketPlayer(onGround));
    }

    @Override public IPacket createUseEntityPacket(IEntity target, boolean attack) {
        return new Mc189Packet(new C02PacketUseEntity(Mc189Entities.unwrap(target),
                attack ? C02PacketUseEntity.Action.ATTACK : C02PacketUseEntity.Action.INTERACT));
    }

    @Override public IPacket createSwingPacket() {
        return new Mc189Packet(new C0APacketAnimation());
    }

    @Override public IPacket createEntityActionPacket(String action) {
        if (mc.thePlayer == null) return new Mc189Packet(new C00PacketKeepAlive());
        try {
            C0BPacketEntityAction.Action value = C0BPacketEntityAction.Action.valueOf(action);
            return new Mc189Packet(new C0BPacketEntityAction(mc.thePlayer, value));
        } catch (IllegalArgumentException e) {
            if ("RESPAWN".equals(action)) {
                return new Mc189Packet(new net.minecraft.network.play.client.C16PacketClientStatus(
                        net.minecraft.network.play.client.C16PacketClientStatus.EnumState.PERFORM_RESPAWN));
            }
            StormLogger.warn("unknown entity action " + action);
            return new Mc189Packet(new C00PacketKeepAlive());
        }
    }

    public IPacket createChatPacket(String message) {
        return new Mc189Packet(new C01PacketChatMessage(message));
    }
}
