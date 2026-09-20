package xyz.stormclient.module.impl.movement;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.util.MovementUtil;

public class Sprint extends Module {

    private final ModeSetting   mode    = add(new ModeSetting("Mode", "Legit", "Legit", "Omni"));
    private final BooleanSetting whenLow = add(new BooleanSetting("While hungry", false));
    private final BooleanSetting whileBlocking = add(new BooleanSetting("While blocking", false));

    public Sprint() {
        super("Sprint", "Always sprint", Category.MOVEMENT);
    }

    @Override public String tag() { return mode.get(); }

    @Subscribe
    public void onTick(TickEvent.Pre event) {
        if (nullCheck()) return;
        if (!MovementUtil.isMoving()) return;
        if (!whenLow.get() && player().hunger() <= 6F) return;
        if (!whileBlocking.get() && player().isBlocking()) return;
        if (mode.is("Legit") && player().moveForward() <= 0) return;
        if (player().isCollidedHorizontally() && mode.is("Legit")) return;

        player().setSprinting(true);
    }
}
