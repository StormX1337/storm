package xyz.stormclient.module.impl.player;

import java.util.ArrayList;
import java.util.List;

import xyz.stormclient.bridge.IPacket;
import xyz.stormclient.bridge.PacketType;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.PacketEvent;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;

/** Holds back your movement packets and releases them at once. */
public class Blink extends Module {

    private final NumberSetting  maxTicks = add(new NumberSetting("Max ticks", 60, 5, 200, 5));
    private final BooleanSetting autoRelease = add(new BooleanSetting("Auto release", true));
    private final BooleanSetting renderGhost = add(new BooleanSetting("Show ghost", true));

    private final List<IPacket> held = new ArrayList<IPacket>();
    private int ticks;

    public Blink() {
        super("Blink", "Delays your movement packets", Category.PLAYER);
    }

    @Override public String tag() { return held.isEmpty() ? "" : held.size() + " held"; }

    @Override public void onDisable() { release(); }

    private void release() {
        for (IPacket packet : held) mc().network().sendSilent(packet);
        held.clear();
        ticks = 0;
    }

    @Subscribe
    public void onPacket(PacketEvent.Send event) {
        if (nullCheck()) return;
        PacketType type = event.type();
        if (type == PacketType.C_PLAYER || type == PacketType.C_PLAYER_POSITION
                || type == PacketType.C_PLAYER_LOOK || type == PacketType.C_PLAYER_POSITION_LOOK) {
            held.add(event.packet());
            event.cancel();
        }
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (++ticks >= maxTicks.getInt() && autoRelease.get()) release();
    }

    public boolean ghost() { return renderGhost.get(); }
    public int heldCount() { return held.size(); }
}
