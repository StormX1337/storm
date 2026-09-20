package xyz.stormclient.module.impl.hud;

import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ColorSetting;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.util.Animation;
import xyz.stormclient.util.ColorUtil;

/** WASD, mouse buttons, CPS and the space bar. */
public class Keystrokes extends HudModule {

    private final ModeSetting    mode    = add(new ModeSetting("Mode", "Full", "Full", "WASD", "Mouse"));
    private final ColorSetting   pressed = add(new ColorSetting("Pressed", 0xFF35C4FF));
    private final ColorSetting   idle    = add(new ColorSetting("Idle", 0x66101318));
    private final BooleanSetting showCps = add(new BooleanSetting("Show CPS", true));
    private final BooleanSetting rounded = add(new BooleanSetting("Rounded", true));

    private static final int SIZE = 22;
    private static final int GAP  = 2;

    private final Animation[] keys = new Animation[7];

    public Keystrokes() {
        super("Keystrokes", "Shows which keys you are pressing", 0.02, 0.6);
        for (int i = 0; i < keys.length; i++) keys[i] = new Animation(14F);
    }

    @Override public double width()  { return SIZE * 3 + GAP * 2; }
    @Override public double height() { return mode.is("Mouse") ? SIZE : SIZE * 3 + GAP * 2 + 10; }

    @Override public void renderElement(IRenderer r, IFontRenderer font) {
        boolean[] state = {
                mc().input().forward(), mc().input().left(), mc().input().back(), mc().input().right(),
                mc().input().mouseDown(0), mc().input().mouseDown(1), mc().input().jump()
        };
        for (int i = 0; i < keys.length; i++) keys[i].set(state[i]);

        double row = SIZE + GAP;
        if (!mode.is("Mouse")) {
            key(r, font, 0, row, 0, SIZE, "W");
            key(r, font, 1, 0, row, SIZE, "A");
            key(r, font, 2, row, row, SIZE, "S");
            key(r, font, 3, row * 2, row, SIZE, "D");
        }
        if (!mode.is("WASD")) {
            double y = mode.is("Mouse") ? 0 : row * 2;
            key(r, font, 4, 0, y, SIZE + GAP / 2.0, showCps.get() ? cps(0) : "LMB");
            key(r, font, 5, SIZE + GAP + GAP / 2.0, y, SIZE + GAP / 2.0, showCps.get() ? cps(1) : "RMB");
            if (mode.is("Full")) {
                r.roundedRect(0, y + row, width(), 8, rounded.get() ? 2F : 0F,
                        ColorUtil.mix(idle.rgb(), pressed.rgb(), keys[6].eased()));
                font.drawCentered("\u2423", width() / 2, y + row, textColor(keys[6].eased()));
            }
        }
    }

    private void key(IRenderer r, IFontRenderer font, int index, double x, double y, double w, String label) {
        float t = keys[index].eased();
        r.roundedRect(x, y, w, SIZE, rounded.get() ? 3F : 0F, ColorUtil.mix(idle.rgb(), pressed.rgb(), t));
        font.drawCentered(label, x + w / 2, y + SIZE / 2.0 - font.height() / 2.0, textColor(t));
    }

    private int textColor(float t) {
        return ColorUtil.mix(theme().text(), 0xFF101318, t);
    }

    private String cps(int button) {
        return String.valueOf(xyz.stormclient.Storm.get().cps(button)) + " cps";
    }
}
