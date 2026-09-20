package xyz.stormclient.module.impl.movement;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.MoveEvent;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.util.MovementUtil;

public class LongJump extends Module {

    private final NumberSetting boost  = add(new NumberSetting("Boost", 1.6, 1.0, 4.0, 0.05).suffix("x"));
    private final NumberSetting height = add(new NumberSetting("Height", 0.42, 0.2, 1.0, 0.01));
    private final NumberSetting decay  = add(new NumberSetting("Decay", 0.66, 0.1, 1.0, 0.01));

    private double motion;
    private int state;

    public LongJump() {
        super("LongJump", "Jump much further than vanilla", Category.MOVEMENT);
    }

    @Subscribe
    public void onTick(TickEvent.Pre event) {
        if (nullCheck() || !MovementUtil.isMoving()) { state = 0; return; }

        if (player().isOnGround()) {
            state = 1;
            motion = MovementUtil.WALK_SPEED * boost.get();
            player().setMotionY(height.get());
        } else if (state == 1) {
            motion -= motion * (1.0 - decay.get()) * 0.1;
        }
    }

    @Subscribe
    public void onMove(MoveEvent event) {
        if (nullCheck() || state != 1 || !MovementUtil.isMoving()) return;
        double yaw = Math.toRadians(MovementUtil.movementYaw());
        event.setX(-Math.sin(yaw) * motion);
        event.setZ(Math.cos(yaw) * motion);
    }
}
