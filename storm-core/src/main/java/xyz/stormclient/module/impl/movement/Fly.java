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

public class Fly extends Module {

    private final ModeSetting   mode     = add(new ModeSetting("Mode", "Creative", "Creative", "Motion", "Glide"));
    private final NumberSetting speed    = add(new NumberSetting("Speed", 1.0, 0.1, 5.0, 0.05));
    private final NumberSetting glide    = add(new NumberSetting("Glide speed", 0.02, 0.0, 0.2, 0.005));
    private final BooleanSetting antiKick = add(new BooleanSetting("Anti kick", true));

    private int ticks;

    public Fly() {
        super("Fly", "Free movement through the air", Category.MOVEMENT);
        glide.visibleWhen(() -> mode.is("Glide"));
    }

    @Override public String tag() { return mode.get(); }

    @Override public void onDisable() {
        if (!nullCheck()) player().setMotionY(0);
        ticks = 0;
    }

    @Subscribe
    public void onTick(TickEvent.Pre event) {
        if (nullCheck()) return;
        player().setFallDistance(0);

        if (antiKick.get() && ++ticks % 20 == 0) {
            player().setMotionY(-0.04);            // a tiny descend keeps most servers happy
        }
    }

    @Subscribe
    public void onMove(MoveEvent event) {
        if (nullCheck()) return;

        if (mode.is("Glide")) {
            if (!player().isOnGround()) event.setY(-glide.get());
            return;
        }

        double y = 0;
        if (mc().input().jump())  y += speed.get() * 0.5;
        if (mc().input().sneak()) y -= speed.get() * 0.5;
        event.setY(y);

        if (MovementUtil.isMoving()) {
            double yaw = Math.toRadians(MovementUtil.movementYaw());
            event.setX(-Math.sin(yaw) * speed.get() * 0.5);
            event.setZ(Math.cos(yaw) * speed.get() * 0.5);
        } else {
            event.setX(0);
            event.setZ(0);
        }
        player().setMotion(event.x(), event.y(), event.z());
    }
}
