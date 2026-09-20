package xyz.stormclient.module.impl.render;

import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.setting.NumberSetting;

/** Custom item and hand animations. The bridge applies the transform before the hand is drawn. */
public class Animations extends Module {

    private final ModeSetting    preset = add(new ModeSetting("Preset", "Storm", "Storm", "1.7", "Swank", "Exhibition", "Custom"));
    private final NumberSetting  x      = add(new NumberSetting("X", 0.0, -1.0, 1.0, 0.01));
    private final NumberSetting  y      = add(new NumberSetting("Y", 0.0, -1.0, 1.0, 0.01));
    private final NumberSetting  z      = add(new NumberSetting("Z", 0.0, -1.0, 1.0, 0.01));
    private final NumberSetting  scale  = add(new NumberSetting("Scale", 1.0, 0.5, 2.0, 0.01).suffix("x"));
    private final NumberSetting  swing  = add(new NumberSetting("Swing speed", 6, 1, 20, 1).suffix(" ticks"));
    private final BooleanSetting oldBlock = add(new BooleanSetting("1.7 blocking", true));

    public Animations() {
        super("Animations", "Custom hand and item animations", Category.RENDER);
        x.visibleWhen(() -> preset.is("Custom"));
        y.visibleWhen(() -> preset.is("Custom"));
        z.visibleWhen(() -> preset.is("Custom"));
    }

    @Override public String tag() { return preset.get(); }

    /** x, y, z, scale for the current preset. */
    public float[] transform() {
        if (preset.is("1.7"))        return new float[] { -0.14F, 0.08F, 0.0F, 1.0F };
        if (preset.is("Swank"))      return new float[] { -0.28F, 0.12F, -0.2F, 1.1F };
        if (preset.is("Exhibition")) return new float[] { -0.33F, 0.05F, -0.3F, 1.2F };
        if (preset.is("Custom"))     return new float[] { x.getFloat(), y.getFloat(), z.getFloat(), scale.getFloat() };
        return new float[] { -0.2F, 0.1F, -0.1F, 1.05F };          // Storm default
    }

    public int swingSpeed()   { return swing.getInt(); }
    public boolean oldBlock() { return oldBlock.get(); }
}
