package xyz.stormclient.module.impl.render;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.ViewEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.setting.NumberSetting;

public class Fullbright extends Module {

    private final ModeSetting   mode   = add(new ModeSetting("Mode", "Gamma", "Gamma", "Night vision"));
    private final NumberSetting gamma  = add(new NumberSetting("Gamma", 100, 1, 100, 1).suffix("%"));

    public Fullbright() {
        super("Fullbright", "See in the dark", Category.RENDER);
    }

    @Override public String tag() { return mode.get(); }

    @Subscribe
    public void onView(ViewEvent event) {
        if (event.kind() != ViewEvent.Kind.LIGHT) return;
        event.setValue(mode.is("Gamma") ? gamma.getFloat() / 10F : 1F);
    }

    public boolean nightVision() { return isEnabled() && mode.is("Night vision"); }
}
