package xyz.stormclient.module.impl.movement;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.MoveEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.NumberSetting;

public class Spider extends Module {

    private final NumberSetting speed = add(new NumberSetting("Climb speed", 0.2, 0.05, 0.6, 0.01));

    public Spider() {
        super("Spider", "Climb up walls", Category.MOVEMENT);
    }

    @Subscribe
    public void onMove(MoveEvent event) {
        if (nullCheck() || !player().isCollidedHorizontally()) return;
        event.setY(speed.get());
        player().setMotionY(speed.get());
    }
}
