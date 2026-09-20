package xyz.stormclient.module.impl.render;

import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.NumberSetting;

/** Lets the third person camera pass through blocks. */
public class ViewClip extends Module {

    private final NumberSetting distance = add(new NumberSetting("Distance", 4.0, 1.0, 12.0, 0.5).suffix("m"));

    public ViewClip() {
        super("ViewClip", "Third person camera ignores walls", Category.RENDER);
    }

    public float distance() { return distance.getFloat(); }
}
