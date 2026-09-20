package xyz.stormclient.module.impl.movement;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.MoveEvent;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.ModeSetting;

public class Jesus extends Module {

    private final ModeSetting mode = add(new ModeSetting("Mode", "Solid", "Solid", "Dolphin"));

    public Jesus() {
        super("Jesus", "Walk on water", Category.MOVEMENT);
    }

    @Override public String tag() { return mode.get(); }

    @Subscribe
    public void onTick(TickEvent.Pre event) {
        if (nullCheck() || !player().isInWater()) return;
        if (mc().input().sneak()) return;

        if (mode.is("Dolphin")) {
            player().setMotionY(player().motionY() > 0 ? 0.08 : -0.04);
        }
    }

    @Subscribe
    public void onMove(MoveEvent event) {
        if (nullCheck() || !mode.is("Solid")) return;
        if (!player().isInWater() || mc().input().sneak()) return;

        boolean deep = world().isLiquid((int) Math.floor(player().x()),
                                        (int) Math.floor(player().y() - 0.5),
                                        (int) Math.floor(player().z()));
        if (deep) event.setY(0.08);
    }
}
