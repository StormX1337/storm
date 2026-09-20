package xyz.stormclient.module.impl.player;

import xyz.stormclient.bridge.PacketType;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.PacketEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.NumberSetting;

public class AutoRespawn extends Module {

    private final NumberSetting delay = add(new NumberSetting("Delay", 200, 0, 2000, 50).suffix("ms"));

    public AutoRespawn() {
        super("AutoRespawn", "Respawns you the moment you die", Category.PLAYER);
    }

    @Subscribe
    public void onPacket(PacketEvent.Receive event) {
        if (event.type() != PacketType.S_UPDATE_HEALTH) return;
        if (event.packet().getFloat("health") > 0) return;

        final long wait = delay.getLong();
        new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(wait); } catch (InterruptedException ignored) { }
                if (!nullCheck()) mc().network().sendSilent(mc().network().createEntityActionPacket("RESPAWN"));
            }
        }, "Storm-Respawn").start();
    }
}
