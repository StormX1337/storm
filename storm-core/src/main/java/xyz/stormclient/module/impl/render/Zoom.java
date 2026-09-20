package xyz.stormclient.module.impl.render;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.ViewEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.util.Animation;

/** Hold the bind to zoom. Smooth, because a hard cut looks terrible. */
public class Zoom extends Module {

    private final NumberSetting  amount = add(new NumberSetting("Amount", 4.0, 1.5, 12.0, 0.5).suffix("x"));
    private final BooleanSetting smooth = add(new BooleanSetting("Smooth", true));
    private final BooleanSetting lowerSensitivity = add(new BooleanSetting("Lower sensitivity", true));

    private final Animation zoom = new Animation(8F);

    public Zoom() {
        super("Zoom", "Hold to zoom in", Category.RENDER);
        setKeybind(xyz.stormclient.util.Keyboard.code("C"));
    }

    @Subscribe
    public void onView(ViewEvent event) {
        if (event.kind() != ViewEvent.Kind.FOV) return;
        zoom.set(true);
        float t = smooth.get() ? zoom.eased() : 1F;
        event.setValue(event.value() / (1F + (amount.getFloat() - 1F) * t));
    }

    @Override public void onDisable() { zoom.snap(0F); }

    public boolean lowerSensitivity() { return lowerSensitivity.get(); }
}
