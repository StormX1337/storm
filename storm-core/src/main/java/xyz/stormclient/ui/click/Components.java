package xyz.stormclient.ui.click;

import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ColorSetting;
import xyz.stormclient.setting.KeybindSetting;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.setting.Setting;
import xyz.stormclient.setting.StringSetting;
import xyz.stormclient.util.Animation;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.Keyboard;
import xyz.stormclient.util.MathUtil;

/** All concrete setting rows live here so they stay consistent with each other. */
public final class Components {

    private Components() { }

    public static Component of(Setting<?> setting) {
        if (setting instanceof BooleanSetting) return new Toggle((BooleanSetting) setting);
        if (setting instanceof NumberSetting)  return new Slider((NumberSetting) setting);
        if (setting instanceof ModeSetting)    return new Mode((ModeSetting) setting);
        if (setting instanceof ColorSetting)   return new Color((ColorSetting) setting);
        if (setting instanceof KeybindSetting) return new Keybind((KeybindSetting) setting);
        if (setting instanceof StringSetting)  return new Text((StringSetting) setting);
        return null;
    }

    // ------------------------------------------------------------------
    public static final class Toggle extends Component {

        private final BooleanSetting value;
        private final Animation anim = new Animation(10F);

        Toggle(BooleanSetting value) { super(value); this.value = value; anim.snap(value.get() ? 1F : 0F); }

        @Override public double height() { return 15; }

        @Override public void render(int mouseX, int mouseY) {
            anim.set(value.get());
            float t = anim.eased();

            font().draw(value.name(), x + 4, y + 4, hovered(mouseX, mouseY) ? theme().text() : theme().textDim());

            double bw = 18, bh = 9;
            double bx = x + width - bw - 6;
            double by = y + 3;
            r().roundedRect(bx, by, bw, bh, (float) (bh / 2.0),
                    ColorUtil.mix(theme().panelDark(), theme().accent(), t));
            r().circle(bx + bh / 2 + (bw - bh) * t, by + bh / 2, bh / 2 - 1.2, 0xFFFFFFFF);
        }

        @Override public void mouseDown(int mouseX, int mouseY, int button) {
            if (button == 0 && hovered(mouseX, mouseY)) value.toggle();
        }
    }

    // ------------------------------------------------------------------
    public static final class Slider extends Component {

        private final NumberSetting value;
        private boolean dragging;

        Slider(NumberSetting value) { super(value); this.value = value; }

        @Override public double height() { return 22; }

        @Override public void render(int mouseX, int mouseY) {
            font().draw(value.name(), x + 4, y + 2, theme().textDim());
            String text = value.display();
            font().draw(text, x + width - font().width(text) - 6, y + 2, theme().text());

            double bx = x + 5, bw = width - 11, by = y + 15;
            r().roundedRect(bx, by, bw, 3, 1.5F, theme().panelDark());
            double filled = bw * value.fraction();
            r().roundedRect(bx, by, filled, 3, 1.5F, theme().accent());
            r().circle(bx + filled, by + 1.5, dragging ? 4 : 3, 0xFFFFFFFF);
        }

        @Override public void mouseDown(int mouseX, int mouseY, int button) {
            if (button == 0 && hovered(mouseX, mouseY)) {
                dragging = true;
                drag(mouseX);
            }
        }

        @Override public void mouseUp(int mouseX, int mouseY, int button) { dragging = false; }

        @Override public void mouseDragged(int mouseX, int mouseY) { if (dragging) drag(mouseX); }

        private void drag(int mouseX) {
            value.setFraction((mouseX - (x + 5)) / (width - 11));
        }
    }

    // ------------------------------------------------------------------
    public static final class Mode extends Component {

        private final ModeSetting value;
        private boolean open;

        Mode(ModeSetting value) { super(value); this.value = value; }

        @Override public double height() { return open ? 15 + value.modes().size() * 12 : 15; }

        @Override public void render(int mouseX, int mouseY) {
            font().draw(value.name(), x + 4, y + 4, theme().textDim());
            String text = value.get() + (open ? " ▴" : " ▾");
            font().draw(text, x + width - font().width(text) - 6, y + 4, theme().accent());

            if (!open) return;
            double oy = y + 15;
            for (String mode : value.modes()) {
                boolean selected = value.is(mode);
                boolean hover = MathUtil.inside(mouseX, mouseY, x, oy, width, 12);
                if (hover) r().rect(x, oy, width, 12, ColorUtil.withAlpha(theme().accent(), 40));
                font().draw(mode, x + 12, oy + 2, selected ? theme().accent() : theme().textDim());
                oy += 12;
            }
        }

        @Override public void mouseDown(int mouseX, int mouseY, int button) {
            if (MathUtil.inside(mouseX, mouseY, x, y, width, 15)) {
                if (button == 0) open = !open;
                else value.next();
                return;
            }
            if (!open) return;
            double oy = y + 15;
            for (String mode : value.modes()) {
                if (MathUtil.inside(mouseX, mouseY, x, oy, width, 12)) {
                    value.set(mode);
                    open = false;
                    return;
                }
                oy += 12;
            }
        }
    }

    // ------------------------------------------------------------------
    public static final class Color extends Component {

        private final ColorSetting value;
        private boolean open;
        private int dragging = -1;          // 0 = sat/bright box, 1 = hue, 2 = alpha

        Color(ColorSetting value) { super(value); this.value = value; }

        @Override public double height() { return open ? 15 + 62 : 15; }

        @Override public void render(int mouseX, int mouseY) {
            font().draw(value.name(), x + 4, y + 4, theme().textDim());
            r().roundedRect(x + width - 22, y + 3, 16, 9, 2F, value.rgb());

            if (!open) return;
            float[] hsb = value.hsb();
            double bx = x + 6, by = y + 18, bw = width - 34, bh = 44;

            // saturation / brightness field
            r().gradientRectH(bx, by, bw, bh, 0xFFFFFFFF, ColorUtil.fromHsb(hsb[0], 1F, 1F, 255));
            r().gradientRect(bx, by, bw, bh, 0x00000000, 0xFF000000);
            r().circle(bx + bw * hsb[1], by + bh * (1 - hsb[2]), 2.5, 0xFFFFFFFF);

            // hue strip
            double hx = x + width - 24;
            for (int i = 0; i < 44; i++) {
                r().rect(hx, by + i, 8, 1, ColorUtil.fromHsb(i / 44F, 1F, 1F, 255));
            }
            r().rect(hx, by + 44 * hsb[0] - 1, 8, 2, 0xFFFFFFFF);

            // alpha strip
            double ax = x + width - 13;
            r().gradientRect(ax, by, 8, bh, ColorUtil.withAlpha(value.get(), 255), ColorUtil.withAlpha(value.get(), 0));
            r().rect(ax, by + bh * (1 - value.alpha() / 255F) - 1, 8, 2, 0xFFFFFFFF);

            String rainbow = value.rainbow() ? "rainbow on" : "rainbow off";
            font().draw(rainbow, bx, by + bh + 2, value.rainbow() ? theme().accent() : theme().textFaint());
        }

        @Override public void mouseDown(int mouseX, int mouseY, int button) {
            if (MathUtil.inside(mouseX, mouseY, x, y, width, 15)) {
                if (button == 0) open = !open;
                else value.setRainbow(!value.rainbow());
                return;
            }
            if (!open) return;

            double bx = x + 6, by = y + 18, bw = width - 34, bh = 44;
            if (MathUtil.inside(mouseX, mouseY, bx, by, bw, bh)) dragging = 0;
            else if (MathUtil.inside(mouseX, mouseY, x + width - 24, by, 8, bh)) dragging = 1;
            else if (MathUtil.inside(mouseX, mouseY, x + width - 13, by, 8, bh)) dragging = 2;
            else if (MathUtil.inside(mouseX, mouseY, bx, by + bh, bw, 12)) value.setRainbow(!value.rainbow());
            mouseDragged(mouseX, mouseY);
        }

        @Override public void mouseUp(int mouseX, int mouseY, int button) { dragging = -1; }

        @Override public void mouseDragged(int mouseX, int mouseY) {
            if (dragging < 0) return;
            float[] hsb = value.hsb();
            double bx = x + 6, by = y + 18, bw = width - 34, bh = 44;

            if (dragging == 0) {
                float s = (float) MathUtil.clamp((mouseX - bx) / bw, 0, 1);
                float b = 1F - (float) MathUtil.clamp((mouseY - by) / bh, 0, 1);
                value.setHsb(hsb[0], s, b);
            } else if (dragging == 1) {
                float h = (float) MathUtil.clamp((mouseY - by) / bh, 0, 1);
                value.setHsb(h, hsb[1], hsb[2]);
            } else {
                value.setAlpha((int) ((1 - MathUtil.clamp((mouseY - by) / bh, 0, 1)) * 255));
            }
        }
    }

    // ------------------------------------------------------------------
    public static final class Keybind extends Component {

        private final KeybindSetting value;

        Keybind(KeybindSetting value) { super(value); this.value = value; }

        @Override public double height() { return 15; }

        @Override public void render(int mouseX, int mouseY) {
            font().draw(value.name(), x + 4, y + 4, theme().textDim());
            String text = value.display();
            font().draw(text, x + width - font().width(text) - 6, y + 4,
                    value.listening() ? theme().accent() : theme().text());
        }

        @Override public void mouseDown(int mouseX, int mouseY, int button) {
            if (!hovered(mouseX, mouseY)) return;
            if (button == 1) { value.set(Keyboard.KEY_NONE); return; }
            value.setListening(!value.listening());
        }

        @Override public void keyDown(int key, char typed) {
            if (!value.listening()) return;
            value.set(key == Keyboard.KEY_ESCAPE ? Keyboard.KEY_NONE : key);
            value.setListening(false);
        }
    }

    // ------------------------------------------------------------------
    public static final class Text extends Component {

        private final StringSetting value;
        private boolean focused;

        Text(StringSetting value) { super(value); this.value = value; }

        @Override public double height() { return 24; }

        @Override public void render(int mouseX, int mouseY) {
            font().draw(value.name(), x + 4, y + 2, theme().textDim());
            r().roundedRect(x + 5, y + 12, width - 11, 10, 2F, theme().panelDark());
            String shown = font().trim(value.get(), (int) width - 16) + (focused ? "_" : "");
            font().draw(shown, x + 8, y + 13, focused ? theme().text() : theme().textDim());
        }

        @Override public void mouseDown(int mouseX, int mouseY, int button) {
            focused = hovered(mouseX, mouseY);
        }

        @Override public void keyDown(int key, char typed) {
            if (!focused) return;
            if (key == Keyboard.KEY_BACK) {
                String current = value.get();
                if (!current.isEmpty()) value.set(current.substring(0, current.length() - 1));
            } else if (key == Keyboard.KEY_RETURN) {
                focused = false;
            } else if (typed >= 32 && typed != 127) {
                value.set(value.get() + typed);
            }
        }
    }
}
