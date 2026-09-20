package xyz.stormclient.module.impl.combat;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.AttackEvent;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.setting.NumberSetting;

/** Resets the sprint on hit so every hit lands with full knockback. */
public class WTap extends Module {

    private final ModeSetting mode  = add(new ModeSetting("Mode", "W-Tap", "W-Tap", "S-Tap", "Packet"));
    private final NumberSetting ticks = add(new NumberSetting("Ticks", 2, 1, 6, 1));

    private int counter;

    public WTap() {
        super("WTap", "Resets your sprint on hit for full knockback", Category.COMBAT);
    }

    @Override public String tag() { return mode.get(); }

    @Subscribe
    public void onAttack(AttackEvent event) {
        if (nullCheck() || !player().isSprinting()) return;
        counter = ticks.getInt();

        if (mode.is("Packet")) {
            mc().network().sendSilent(mc().network().createEntityActionPacket("STOP_SPRINTING"));
            mc().network().sendSilent(mc().network().createEntityActionPacket("START_SPRINTING"));
            counter = 0;
        }
    }

    @Subscribe
    public void onTick(TickEvent.Pre event) {
        if (nullCheck() || counter <= 0) return;
        counter--;

        if (mode.is("W-Tap")) {
            player().setSprinting(false);
            mc().input().setKeyState("forward", false);
        } else if (mode.is("S-Tap")) {
            mc().input().setKeyState("back", true);
        }

        if (counter == 0) {
            mc().input().setKeyState("forward", true);
            mc().input().setKeyState("back", false);
        }
    }
}
