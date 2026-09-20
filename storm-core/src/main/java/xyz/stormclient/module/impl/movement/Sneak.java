package xyz.stormclient.module.impl.movement;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.ModeSetting;

/** Keeps the sneak flag on without the movement penalty. */
public class Sneak extends Module {

    private final ModeSetting mode = add(new ModeSetting("Mode", "Packet", "Packet", "Vanilla"));

    public Sneak() {
        super("Sneak", "Always appear to be sneaking", Category.MOVEMENT);
    }

    @Override public String tag() { return mode.get(); }

    @Override public void onEnable() {
        if (!nullCheck() && mode.is("Packet")) {
            mc().network().sendSilent(mc().network().createEntityActionPacket("START_SNEAKING"));
        }
    }

    @Override public void onDisable() {
        if (!nullCheck()) {
            mc().network().sendSilent(mc().network().createEntityActionPacket("STOP_SNEAKING"));
            mc().input().setKeyState("sneak", false);
        }
    }

    @Subscribe
    public void onTick(TickEvent.Pre event) {
        if (nullCheck() || !mode.is("Vanilla")) return;
        mc().input().setKeyState("sneak", true);
    }
}
