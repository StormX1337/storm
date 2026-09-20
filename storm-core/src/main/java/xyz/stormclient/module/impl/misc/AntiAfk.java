package xyz.stormclient.module.impl.misc;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.util.RandomUtil;
import xyz.stormclient.util.TimerUtil;

public class AntiAfk extends Module {

    private final NumberSetting  interval = add(new NumberSetting("Interval", 20, 2, 120, 1).suffix("s"));
    private final BooleanSetting rotate   = add(new BooleanSetting("Look around", true));
    private final BooleanSetting jump     = add(new BooleanSetting("Jump", true));
    private final BooleanSetting swing    = add(new BooleanSetting("Swing", false));

    private final TimerUtil timer = new TimerUtil();

    public AntiAfk() {
        super("AntiAFK", "Keeps you from being kicked for idling", Category.MISC);
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (nullCheck() || !timer.passedAndReset((long) (interval.get() * 1000.0))) return;

        if (rotate.get()) {
            player().setRotation(player().yaw() + (float) RandomUtil.range(-40, 40),
                                 (float) RandomUtil.range(-15, 25));
        }
        if (jump.get() && player().isOnGround()) player().jump();
        if (swing.get()) player().swingArm();
    }
}
