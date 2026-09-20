package xyz.stormclient.module.impl.render;

import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.ColorSetting;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.setting.NumberSetting;

/**
 * Post processing Storm applies over the world: a soft glow around entities and
 * an optional blur behind open menus. The bridge owns the actual GL program.
 */
public class Shaders extends Module {

    private final ModeSetting    mode      = add(new ModeSetting("Mode", "Glow", "Glow", "Outline", "Blur"));
    private final ColorSetting   color     = add(new ColorSetting("Color", 0xFF35C4FF));
    private final NumberSetting  radius    = add(new NumberSetting("Radius", 4, 1, 16, 1).suffix("px"));
    private final NumberSetting  intensity = add(new NumberSetting("Intensity", 1.0, 0.1, 3.0, 0.1));

    public Shaders() {
        super("Shaders", "Glow and blur post processing", Category.RENDER);
    }

    @Override public String tag() { return mode.get(); }

    public String mode()     { return mode.get(); }
    public int    color()    { return color.rgb(); }
    public float  radius()   { return radius.getFloat(); }
    public float  intensity(){ return intensity.getFloat(); }
}
