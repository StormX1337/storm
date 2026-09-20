package xyz.stormclient.module.impl.movement;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.MoveEvent;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.util.MovementUtil;

/** Faster ground movement. Strict mode stays inside what vanilla physics allows. */
public class Speed extends Module {

    private final ModeSetting   mode   = add(new ModeSetting("Mode", "Strict", "Strict", "Bhop", "Boost"));
    private final NumberSetting amount = add(new NumberSetting("Speed", 1.15, 1.0, 2.0, 0.01).suffix("x"));
    private final NumberSetting jumpBoost = add(new NumberSetting("Jump boost", 0.0, 0.0, 0.2, 0.005));
    private final BooleanSetting sprintOnly = add(new BooleanSetting("Only while sprinting", true));

    public Speed() {
        super("Speed", "Move faster", Category.MOVEMENT);
        amount.visibleWhen(() -> !mode.is("Strict"));
    }

    @Override public String tag() { return mode.get(); }

    @Subscribe
    public void onTick(TickEvent.Pre event) {
        if (nullCheck() || !MovementUtil.isMoving()) return;
        if (sprintOnly.get() && !player().isSprinting()) return;

        if (mode.is("Bhop") && player().isOnGround()) {
            player().jump();
            if (jumpBoost.get() > 0) player().setMotionY(player().motionY() + jumpBoost.get());
        }
    }

    @Subscribe
    public void onMove(MoveEvent event) {
        if (nullCheck() || !MovementUtil.isMoving()) return;
        if (sprintOnly.get() && !player().isSprinting()) return;

        if (mode.is("Strict")) return;              // vanilla physics only, handled by Bhop jumps

        double speed = MovementUtil.horizontalSpeed() * amount.get();
        double yaw = Math.toRadians(MovementUtil.movementYaw());
        event.setX(-Math.sin(yaw) * speed);
        event.setZ(Math.cos(yaw) * speed);
    }
}
