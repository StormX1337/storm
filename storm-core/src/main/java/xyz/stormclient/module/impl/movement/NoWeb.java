package xyz.stormclient.module.impl.movement;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.MoveEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.NumberSetting;

public class NoWeb extends Module {

    private final NumberSetting speed = add(new NumberSetting("Speed", 0.25, 0.05, 0.4, 0.01));

    public NoWeb() {
        super("NoWeb", "Move normally inside cobwebs", Category.MOVEMENT);
    }

    @Subscribe
    public void onMove(MoveEvent event) {
        if (nullCheck() || !player().isInWeb()) return;
        double factor = speed.get() / 0.05;
        event.setX(event.x() * factor);
        event.setZ(event.z() * factor);
        if (event.y() < 0) event.setY(event.y() * factor);
    }
}
