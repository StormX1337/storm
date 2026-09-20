package xyz.stormclient.module.impl.render;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.ViewEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;

public class CustomFov extends Module {

    private final NumberSetting  fov      = add(new NumberSetting("FOV", 90, 30, 160, 1).suffix("°"));
    private final BooleanSetting noSprint = add(new BooleanSetting("Static while sprinting", true));
    private final BooleanSetting noBow    = add(new BooleanSetting("Static while aiming", true));

    public CustomFov() {
        super("FOV", "Fixed field of view", Category.RENDER);
    }

    @Override public String tag() { return fov.getInt() + ""; }

    @Subscribe
    public void onView(ViewEvent event) {
        if (event.kind() != ViewEvent.Kind.FOV) return;
        event.setValue(fov.getFloat());
    }

    public boolean noSprintChange() { return noSprint.get(); }
    public boolean noBowChange()    { return noBow.get(); }
}
