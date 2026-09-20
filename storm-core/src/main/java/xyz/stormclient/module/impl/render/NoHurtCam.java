package xyz.stormclient.module.impl.render;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.ViewEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.NumberSetting;

public class NoHurtCam extends Module {

    private final NumberSetting strength = add(new NumberSetting("Strength", 0, 0, 100, 5).suffix("%"));

    public NoHurtCam() {
        super("NoHurtCam", "Removes the camera tilt when you take damage", Category.RENDER);
    }

    @Subscribe
    public void onView(ViewEvent event) {
        if (event.kind() != ViewEvent.Kind.HURT_CAM) return;
        event.setValue(event.value() * (strength.getFloat() / 100F));
    }
}
