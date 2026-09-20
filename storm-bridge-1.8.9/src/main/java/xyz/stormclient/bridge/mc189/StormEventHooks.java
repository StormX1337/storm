package xyz.stormclient.bridge.mc189;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;

import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import xyz.stormclient.Storm;
import xyz.stormclient.event.events.ChatEvent;
import xyz.stormclient.event.events.MotionEvent;
import xyz.stormclient.event.events.MouseEvent;
import xyz.stormclient.event.events.PacketEvent;
import xyz.stormclient.event.events.RenderEvent;
import xyz.stormclient.event.events.WorldEvent;
import xyz.stormclient.util.StormLogger;

/**
 * Turns Forge events and the netty pipeline into Storm events.
 *
 * <p>Motion events are derived from the outgoing movement packets instead of a
 * mixin, which keeps this bridge usable as a plain Forge mod. The events that
 * genuinely need a bytecode hook (move, jump, step, slowdown, fov) come in
 * through {@link StormHooks}.
 */
public final class StormEventHooks {

    private static final String HANDLER_NAME = "storm_packet_handler";
    private static final ThreadLocal<Boolean> SUPPRESSED = new ThreadLocal<Boolean>();

    private NetworkManager attachedTo;
    private int lastKey = Keyboard.KEY_NONE;

    /** Runs the action without firing Storm's outgoing packet event. */
    public static void withoutEvents(Runnable action) {
        SUPPRESSED.set(Boolean.TRUE);
        try {
            action.run();
        } finally {
            SUPPRESSED.remove();
        }
    }

    private static boolean suppressed() {
        return Boolean.TRUE.equals(SUPPRESSED.get());
    }

    // ------------------------------------------------------------------
    //  ticks
    // ------------------------------------------------------------------
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        attachPacketHandler();

        if (event.phase == TickEvent.Phase.START) {
            Storm.get().bus().post(new xyz.stormclient.event.events.TickEvent.Pre());
        } else {
            Storm.get().bus().post(new xyz.stormclient.event.events.TickEvent.Post());
        }
    }

    // ------------------------------------------------------------------
    //  rendering
    // ------------------------------------------------------------------
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Storm.get().bus().post(new RenderEvent.Hud(event.partialTicks));
    }

    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        Storm.get().bus().post(new RenderEvent.World(event.partialTicks));
    }

    // ------------------------------------------------------------------
    //  input
    // ------------------------------------------------------------------
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        int key = Keyboard.getEventKey();
        if (!Keyboard.getEventKeyState()) { lastKey = Keyboard.KEY_NONE; return; }
        if (key == lastKey) return;
        lastKey = key;
        Storm.get().bus().post(new xyz.stormclient.event.events.KeyEvent(key, Keyboard.getEventCharacter()));
    }

    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public void onMouse(InputEvent.MouseInputEvent event) {
        int button = Mouse.getEventButton();
        if (button < 0) return;
        Storm.get().bus().post(new MouseEvent(button, Mouse.getEventButtonState(), Mouse.getEventDWheel()));
    }

    // ------------------------------------------------------------------
    //  chat and world
    // ------------------------------------------------------------------
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        ChatEvent.Receive storm = new ChatEvent.Receive(event.message.getFormattedText());
        Storm.get().bus().post(storm);
        if (storm.isCancelled()) event.setCanceled(true);
    }

    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public void onWorldLoad(net.minecraftforge.event.world.WorldEvent.Load event) {
        Mc189Entities.clear();
        Storm.get().bus().post(new WorldEvent.Load());
    }

    // ------------------------------------------------------------------
    //  packets
    // ------------------------------------------------------------------
    private void attachPacketHandler() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null) { attachedTo = null; return; }

        NetworkManager manager = mc.getNetHandler().getNetworkManager();
        if (manager == attachedTo) return;

        ChannelPipeline pipeline = manager.channel().pipeline();
        if (pipeline.get(HANDLER_NAME) != null) { attachedTo = manager; return; }

        try {
            pipeline.addBefore("packet_handler", HANDLER_NAME, new PacketHandler());
            attachedTo = manager;
            StormLogger.debug("packet handler attached");
        } catch (Throwable t) {
            StormLogger.error("could not attach the packet handler", t);
        }
    }

    private final class PacketHandler extends ChannelDuplexHandler {

        @Override public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
            if (!(message instanceof Packet)) {
                super.channelRead(context, message);
                return;
            }
            Mc189Packet wrapper = new Mc189Packet((Packet<?>) message);
            PacketEvent.Receive event = new PacketEvent.Receive(wrapper);
            Storm.get().bus().post(event);
            if (event.isCancelled()) return;

            super.channelRead(context, wrapper.handle());
        }

        @Override public void write(ChannelHandlerContext context, Object message, ChannelPromise promise)
                throws Exception {
            if (suppressed() || !(message instanceof Packet)) {
                super.write(context, message, promise);
                return;
            }

            Packet<?> packet = (Packet<?>) message;
            Mc189Packet wrapper = new Mc189Packet(packet);

            boolean movement = packet instanceof C03PacketPlayer;
            MotionEvent motion = null;

            if (movement) {
                C03PacketPlayer move = (C03PacketPlayer) packet;
                motion = new MotionEvent(true, move.getPositionX(), move.getPositionY(), move.getPositionZ(),
                        move.getYaw(), move.getPitch(), move.isOnGround());
                Storm.get().bus().post(motion);
                if (motion.isCancelled()) return;
                applyMotion(wrapper, motion);
            }

            if (packet instanceof C01PacketChatMessage) {
                ChatEvent.Send chat = new ChatEvent.Send(((C01PacketChatMessage) packet).getMessage());
                Storm.get().bus().post(chat);
                if (chat.isCancelled()) return;
                if (!chat.message().equals(((C01PacketChatMessage) packet).getMessage())) {
                    super.write(context, new C01PacketChatMessage(chat.message()), promise);
                    return;
                }
            }

            PacketEvent.Send event = new PacketEvent.Send(wrapper);
            Storm.get().bus().post(event);
            if (event.isCancelled()) return;

            super.write(context, wrapper.handle(), promise);

            if (motion != null) {
                Storm.get().bus().post(new MotionEvent(false, motion.x(), motion.y(), motion.z(),
                        motion.yaw(), motion.pitch(), motion.onGround()));
            }
        }
    }

    /** Writes the values a module changed back into the vanilla packet. */
    private void applyMotion(Mc189Packet wrapper, MotionEvent event) {
        wrapper.setDouble("x", event.x());
        wrapper.setDouble("y", event.y());
        wrapper.setDouble("z", event.z());
        wrapper.setFloat("yaw", event.yaw());
        wrapper.setFloat("pitch", event.pitch());
        wrapper.setBoolean("onGround", event.onGround());
    }
}
