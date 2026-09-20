package xyz.stormclient.module.impl.hud;

import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ColorSetting;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.MovementUtil;

/** Replaces the vanilla crosshair, optionally with a dynamic gap. */
public class Crosshair extends HudModule {

    private final ModeSetting    style  = add(new ModeSetting("Style", "Cross", "Cross", "Dot", "Circle"));
    private final ColorSetting   color  = add(new ColorSetting("Color", 0xFFFFFFFF));
    private final NumberSetting  length = add(new NumberSetting("Length", 5, 1, 20, 1).suffix("px"));
    private final NumberSetting  gap    = add(new NumberSetting("Gap", 3, 0, 15, 1).suffix("px"));
    private final NumberSetting  thick  = add(new NumberSetting("Thickness", 1, 1, 4, 1).suffix("px"));
    private final BooleanSetting dynamic= add(new BooleanSetting("Dynamic", true));
    private final BooleanSetting outline= add(new BooleanSetting("Outline", true));

    public Crosshair() {
        super("Crosshair", "Custom crosshair", 0.5, 0.5);
    }

    @Override public double width()  { return (length.get() + gap.get()) * 2; }
    @Override public double height() { return (length.get() + gap.get()) * 2; }

    @Override public void renderElement(IRenderer r, IFontRenderer font) {
        double cx = 0, cy = 0;
        double g = gap.get();
        if (dynamic.get()) g += Math.min(4, MovementUtil.horizontalSpeed() * 12);
        double len = length.get();
        double th = thick.get();
        int argb = color.rgb();

        if (style.is("Dot")) {
            r.rect(cx - th / 2, cy - th / 2, th, th, argb);
            return;
        }
        if (style.is("Circle")) {
            r.arc(cx, cy, g + len / 2, 0, 360, (float) th, argb);
            return;
        }
        if (outline.get()) {
            int shadow = ColorUtil.withAlpha(0xFF000000, 130);
            line(r, cx, cy, g - 1, len + 2, th + 1, shadow);
        }
        line(r, cx, cy, g, len, th, argb);
    }

    private void line(IRenderer r, double cx, double cy, double g, double len, double th, int argb) {
        r.rect(cx - g - len, cy - th / 2, len, th, argb);
        r.rect(cx + g,       cy - th / 2, len, th, argb);
        r.rect(cx - th / 2, cy - g - len, th, len, argb);
        r.rect(cx - th / 2, cy + g,       th, len, argb);
    }
}
