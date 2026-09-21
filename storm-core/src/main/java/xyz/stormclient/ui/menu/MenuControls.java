package xyz.stormclient.ui.menu;

import java.util.ArrayList;
import java.util.List;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ColorSetting;
import xyz.stormclient.setting.KeybindSetting;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.setting.Setting;
import xyz.stormclient.setting.StringSetting;
import xyz.stormclient.ui.Glyphs;
import xyz.stormclient.ui.UiScale;
import xyz.stormclient.ui.theme.Theme;
import xyz.stormclient.util.Animation;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.Keyboard;
import xyz.stormclient.util.MathUtil;

/**
 * The setting rows of the menu window.
 *
 * <p>Every row is laid out the same way: the name on the left with its
 * description under it, and one control pinned to the right edge. That is what
 * makes a long settings list readable, so the control decides only how wide it
 * is, never where the row starts.
 */
public final class MenuControls {

    static final double PAD = 14;
    static final double CONTROL_HEIGHT = 15;

    private MenuControls() { }

    public static Row of(Setting<?> setting) {
        if (setting instanceof BooleanSetting) return new Toggle((BooleanSetting) setting);
        if (setting instanceof NumberSetting)  return new Slider((NumberSetting) setting);
        if (setting instanceof ModeSetting)    return new Dropdown((ModeSetting) setting);
        if (setting instanceof ColorSetting)   return new Colour((ColorSetting) setting);
        if (setting instanceof KeybindSetting) return new Bind((KeybindSetting) setting);
        if (setting instanceof StringSetting)  return new Text((StringSetting) setting);
        return null;
    }

    // ==================================================================
    public abstract static class Row {

        protected final Setting<?> setting;
        protected double x, y, width;

        protected Row(Setting<?> setting) { this.setting = setting; }

        public Setting<?> setting() { return setting; }

        private boolean labelled = true;

        public void position(double x, double y, double width) {
            this.x = x;
            this.y = y;
            this.width = width;
        }

        /**
         * Turns the row's own label off. A page that already prints a heading
         * in that spot — the keybind list prints the module name there — would
         * otherwise draw two strings on top of each other.
         */
        public Row withoutLabel() { this.labelled = false; return this; }

        /** Extra height this row needs below the standard line, e.g. an open list. */
        protected double extraHeight() { return 0; }

        public double height() {
            return (hasDescription() ? 30 : 22) + extraHeight();
        }

        protected boolean hasDescription() {
            return setting.description() != null && !setting.description().isEmpty();
        }

        /** The band the label and the control share, ignoring anything unfolded below. */
        protected double lineHeight() { return hasDescription() ? 30 : 22; }

        public void render(int mouseX, int mouseY) {
            IFontRenderer name = font(UiScale.ROW_FONT);
            IFontRenderer small = font(UiScale.COMPONENT_FONT);
            Theme theme = theme();

            if (labelled) {
                double top = y + (hasDescription() ? 6 : (lineHeight() - name.height()) / 2);
                name.draw(setting.name(), x + PAD, top, theme.text());
                if (hasDescription()) {
                    small.draw(small.trim(setting.description(),
                                    (int) (width - PAD * 2 - controlWidth() - 12)),
                            x + PAD, top + name.height() + 1, theme.textFaint());
                }
            }
            renderControl(mouseX, mouseY);
        }

        /** How much room the control takes on the right. */
        protected abstract double controlWidth();

        protected abstract void renderControl(int mouseX, int mouseY);

        /** Left edge of the control, right aligned inside the row. */
        protected double controlX() { return x + width - PAD - controlWidth(); }

        protected double controlY() { return y + (lineHeight() - CONTROL_HEIGHT) / 2; }

        protected boolean onLine(int mouseX, int mouseY) {
            return MathUtil.inside(mouseX, mouseY, x, y, width, lineHeight());
        }

        protected boolean onControl(int mouseX, int mouseY) {
            return MathUtil.inside(mouseX, mouseY, controlX(), controlY(), controlWidth(), CONTROL_HEIGHT);
        }

        public void mouseDown(int mouseX, int mouseY, int button) { }
        public void mouseUp(int mouseX, int mouseY, int button)   { }
        public void mouseDragged(int mouseX, int mouseY)          { }
        public void keyDown(int key, char typed)                  { }
        public boolean capturingInput()                           { return false; }

        protected IRenderer r()   { return Bridge.mc().renderer(); }
        protected Theme theme()   { return Storm.get().theme(); }
        protected IFontRenderer font(int size) { return Bridge.mc().font(theme().font(), size); }
    }

    // ==================================================================
    /** The pill switch. */
    public static final class Toggle extends Row {

        private final BooleanSetting value;
        private final Animation anim = new Animation(11F);

        Toggle(BooleanSetting value) {
            super(value);
            this.value = value;
            anim.snap(value.get() ? 1F : 0F);
        }

        @Override protected double controlWidth() { return 24; }

        @Override protected void renderControl(int mouseX, int mouseY) {
            anim.set(value.get());
            float t = anim.eased();
            double h = 13;
            double bx = controlX();
            double by = y + (lineHeight() - h) / 2;

            r().roundedRect(bx, by, 24, h, (float) (h / 2),
                    ColorUtil.mix(ColorUtil.withAlpha(theme().text(), 28), theme().accent(), t));
            r().circle(bx + h / 2 + (24 - h) * t, by + h / 2, h / 2 - 1.6, 0xFFFFFFFF);
        }

        @Override public void mouseDown(int mouseX, int mouseY, int button) {
            if (button == 0 && onLine(mouseX, mouseY)) value.toggle();
        }
    }

    // ==================================================================
    /** A track with the number in a box beside it, as in the reference layout. */
    public static final class Slider extends Row {

        private static final double BOX = 46;
        private static final double TRACK = 104;

        private final NumberSetting value;
        private boolean dragging;

        Slider(NumberSetting value) { super(value); this.value = value; }

        @Override protected double controlWidth() { return TRACK + 8 + BOX; }

        private double trackX() { return controlX(); }
        private double trackWidth() { return Math.min(TRACK, Math.max(40, width * 0.3)); }

        @Override protected void renderControl(int mouseX, int mouseY) {
            IFontRenderer font = font(UiScale.COMPONENT_FONT);
            double cy = y + lineHeight() / 2;
            double tw = trackWidth();
            double tx = x + width - PAD - BOX - 8 - tw;

            r().roundedRect(tx, cy - 1.5, tw, 3, 1.5F, ColorUtil.withAlpha(theme().text(), 26));
            double filled = tw * value.fraction();
            r().roundedRect(tx, cy - 1.5, filled, 3, 1.5F, theme().accent());
            r().circle(tx + filled, cy, dragging ? 4.2 : 3.4, 0xFFFFFFFF);

            double bx = x + width - PAD - BOX;
            r().roundedRect(bx, cy - CONTROL_HEIGHT / 2, BOX, CONTROL_HEIGHT, 4F,
                    ColorUtil.withAlpha(0xFF000000, 90));
            r().roundedRectOutline(bx, cy - CONTROL_HEIGHT / 2, BOX, CONTROL_HEIGHT, 4F, 1F,
                    theme().outline());
            String text = value.display();
            font.draw(text, bx + (BOX - font.width(text)) / 2, cy - font.height() / 2.0, theme().text());
        }

        @Override public void mouseDown(int mouseX, int mouseY, int button) {
            if (button != 0) return;
            double tw = trackWidth();
            double tx = x + width - PAD - BOX - 8 - tw;
            if (!MathUtil.inside(mouseX, mouseY, tx - 4, y, tw + 8, lineHeight())) return;
            dragging = true;
            drag(mouseX);
        }

        @Override public void mouseUp(int mouseX, int mouseY, int button) { dragging = false; }

        @Override public void mouseDragged(int mouseX, int mouseY) { if (dragging) drag(mouseX); }

        private void drag(int mouseX) {
            double tw = trackWidth();
            double tx = x + width - PAD - BOX - 8 - tw;
            value.setFraction((mouseX - tx) / tw);
        }
    }

    // ==================================================================
    /** A closed box that folds a list of choices out underneath it. */
    public static final class Dropdown extends Row {

        private static final double BOX = 92;
        private static final double ITEM = 14;

        private final ModeSetting value;
        private boolean open;

        Dropdown(ModeSetting value) { super(value); this.value = value; }

        @Override protected double controlWidth() { return BOX; }

        @Override protected double extraHeight() {
            return open ? value.modes().size() * ITEM + 4 : 0;
        }

        @Override protected void renderControl(int mouseX, int mouseY) {
            IFontRenderer font = font(UiScale.COMPONENT_FONT);
            double bx = controlX();
            double by = controlY();

            r().roundedRect(bx, by, BOX, CONTROL_HEIGHT, 4F, ColorUtil.withAlpha(0xFF000000, 90));
            r().roundedRectOutline(bx, by, BOX, CONTROL_HEIGHT, 4F, 1F,
                    open ? theme().accent() : theme().outline());
            font.draw(font.trim(value.get(), (int) BOX - 22), bx + 7,
                    by + (CONTROL_HEIGHT - font.height()) / 2.0, theme().text());
            Glyphs.chevron(r(), bx + BOX - 13, by + CONTROL_HEIGHT / 2 - 1.5, 5, open, theme().textDim());

            if (!open) return;
            double oy = by + CONTROL_HEIGHT + 3;
            r().roundedRect(bx, oy, BOX, value.modes().size() * ITEM + 2, 4F, 0xF00A0A12);
            r().roundedRectOutline(bx, oy, BOX, value.modes().size() * ITEM + 2, 4F, 1F, theme().outline());
            oy += 1;
            for (String mode : value.modes()) {
                boolean hover = MathUtil.inside(mouseX, mouseY, bx, oy, BOX, ITEM);
                if (hover) r().roundedRect(bx + 1, oy, BOX - 2, ITEM, 3F,
                        ColorUtil.withAlpha(theme().accent(), 45));
                boolean selected = value.is(mode);
                if (selected) r().roundedRect(bx + 4, oy + ITEM / 2 - 1.5, 3, 3, 1.5F, theme().accent());
                font.draw(mode, bx + 11, oy + (ITEM - font.height()) / 2.0,
                        selected ? theme().text() : theme().textDim());
                oy += ITEM;
            }
        }

        @Override public void mouseDown(int mouseX, int mouseY, int button) {
            if (onControl(mouseX, mouseY)) {
                if (button == 0) open = !open;
                else value.next();
                return;
            }
            if (!open) return;
            double oy = controlY() + CONTROL_HEIGHT + 4;
            for (String mode : value.modes()) {
                if (MathUtil.inside(mouseX, mouseY, controlX(), oy, BOX, ITEM)) {
                    value.set(mode);
                    open = false;
                    return;
                }
                oy += ITEM;
            }
            open = false;
        }
    }

    // ==================================================================
    /** A swatch that folds a small picker out underneath it. */
    public static final class Colour extends Row {

        private static final double FIELD = 96;
        private static final double FIELD_H = 46;

        private final ColorSetting value;
        private boolean open;
        private int dragging = -1;          // 0 = saturation field, 1 = hue, 2 = alpha

        Colour(ColorSetting value) { super(value); this.value = value; }

        @Override protected double controlWidth() { return 28; }

        @Override protected double extraHeight() { return open ? FIELD_H + 18 : 0; }

        @Override protected void renderControl(int mouseX, int mouseY) {
            double bx = controlX();
            double by = controlY();
            r().roundedRect(bx, by, 28, CONTROL_HEIGHT, 4F, value.rgb());
            r().roundedRectOutline(bx, by, 28, CONTROL_HEIGHT, 4F, 1F,
                    ColorUtil.withAlpha(theme().text(), 45));

            if (!open) return;
            float[] hsb = value.hsb();
            double fx = x + PAD;
            double fy = y + lineHeight() + 2;

            r().gradientRectH(fx, fy, FIELD, FIELD_H, 0xFFFFFFFF,
                    ColorUtil.fromHsb(hsb[0], 1F, 1F, 255));
            r().gradientRect(fx, fy, FIELD, FIELD_H, 0x00000000, 0xFF000000);
            r().circle(fx + FIELD * hsb[1], fy + FIELD_H * (1 - hsb[2]), 2.5, 0xFFFFFFFF);

            double hx = fx + FIELD + 8;
            for (int i = 0; i < FIELD_H; i++) {
                r().rect(hx, fy + i, 9, 1, ColorUtil.fromHsb(i / (float) FIELD_H, 1F, 1F, 255));
            }
            r().rect(hx, fy + FIELD_H * hsb[0] - 1, 9, 2, 0xFFFFFFFF);

            double ax = hx + 15;
            r().gradientRect(ax, fy, 9, FIELD_H, ColorUtil.withAlpha(value.get(), 255),
                    ColorUtil.withAlpha(value.get(), 0));
            r().rect(ax, fy + FIELD_H * (1 - value.alpha() / 255F) - 1, 9, 2, 0xFFFFFFFF);

            IFontRenderer font = font(UiScale.COMPONENT_FONT);
            String rainbow = value.rainbow() ? "rainbow on" : "rainbow off";
            font.draw(rainbow, fx, fy + FIELD_H + 4,
                    value.rainbow() ? theme().accent() : theme().textFaint());
        }

        @Override public void mouseDown(int mouseX, int mouseY, int button) {
            if (onControl(mouseX, mouseY)) {
                if (button == 0) open = !open;
                else value.setRainbow(!value.rainbow());
                return;
            }
            if (!open) return;
            double fx = x + PAD;
            double fy = y + lineHeight() + 2;
            if (MathUtil.inside(mouseX, mouseY, fx, fy, FIELD, FIELD_H)) dragging = 0;
            else if (MathUtil.inside(mouseX, mouseY, fx + FIELD + 8, fy, 9, FIELD_H)) dragging = 1;
            else if (MathUtil.inside(mouseX, mouseY, fx + FIELD + 23, fy, 9, FIELD_H)) dragging = 2;
            else if (MathUtil.inside(mouseX, mouseY, fx, fy + FIELD_H, FIELD, 12)) {
                value.setRainbow(!value.rainbow());
                return;
            }
            mouseDragged(mouseX, mouseY);
        }

        @Override public void mouseUp(int mouseX, int mouseY, int button) { dragging = -1; }

        @Override public void mouseDragged(int mouseX, int mouseY) {
            if (dragging < 0) return;
            float[] hsb = value.hsb();
            double fx = x + PAD;
            double fy = y + lineHeight() + 2;
            if (dragging == 0) {
                value.setHsb(hsb[0],
                        (float) MathUtil.clamp((mouseX - fx) / FIELD, 0, 1),
                        1F - (float) MathUtil.clamp((mouseY - fy) / FIELD_H, 0, 1));
            } else if (dragging == 1) {
                value.setHsb((float) MathUtil.clamp((mouseY - fy) / FIELD_H, 0, 1), hsb[1], hsb[2]);
            } else {
                value.setAlpha((int) ((1 - MathUtil.clamp((mouseY - fy) / FIELD_H, 0, 1)) * 255));
            }
        }
    }

    // ==================================================================
    /** A button that shows the bound key and listens for the next one. */
    public static final class Bind extends Row {

        private static final double BOX = 62;

        private final KeybindSetting value;

        Bind(KeybindSetting value) { super(value); this.value = value; }

        @Override protected double controlWidth() { return BOX; }

        @Override protected void renderControl(int mouseX, int mouseY) {
            IFontRenderer font = font(UiScale.COMPONENT_FONT);
            double bx = controlX();
            double by = controlY();
            boolean hover = onControl(mouseX, mouseY);

            r().roundedRect(bx, by, BOX, CONTROL_HEIGHT, 4F, ColorUtil.withAlpha(
                    value.listening() ? theme().accent() : 0xFF000000, value.listening() ? 55 : 90));
            r().roundedRectOutline(bx, by, BOX, CONTROL_HEIGHT, 4F, 1F,
                    value.listening() || hover ? theme().accent() : theme().outline());

            String text = value.listening() ? "press a key" : value.display();
            font.draw(font.trim(text, (int) BOX - 8), bx + (BOX - font.width(font.trim(text, (int) BOX - 8))) / 2,
                    by + (CONTROL_HEIGHT - font.height()) / 2.0,
                    value.listening() ? theme().accent() : theme().text());
        }

        @Override public void mouseDown(int mouseX, int mouseY, int button) {
            if (!onControl(mouseX, mouseY)) return;
            if (button == 1) { value.set(Keyboard.KEY_NONE); value.setListening(false); return; }
            value.setListening(!value.listening());
        }

        @Override public void keyDown(int key, char typed) {
            if (!value.listening()) return;
            value.set(key == Keyboard.KEY_ESCAPE ? Keyboard.KEY_NONE : key);
            value.setListening(false);
        }

        @Override public boolean capturingInput() { return value.listening(); }
    }

    // ==================================================================
    /** A single line text field. */
    public static final class Text extends Row {

        private static final double BOX = 112;

        private final StringSetting value;
        private boolean focused;

        Text(StringSetting value) { super(value); this.value = value; }

        @Override protected double controlWidth() { return BOX; }

        @Override protected void renderControl(int mouseX, int mouseY) {
            IFontRenderer font = font(UiScale.COMPONENT_FONT);
            double bx = controlX();
            double by = controlY();

            r().roundedRect(bx, by, BOX, CONTROL_HEIGHT, 4F, ColorUtil.withAlpha(0xFF000000, 90));
            r().roundedRectOutline(bx, by, BOX, CONTROL_HEIGHT, 4F, 1F,
                    focused ? theme().accent() : theme().outline());
            String shown = font.trim(value.get(), (int) BOX - 12) + (focused ? "|" : "");
            font.draw(shown, bx + 6, by + (CONTROL_HEIGHT - font.height()) / 2.0,
                    focused ? theme().text() : theme().textDim());
        }

        @Override public void mouseDown(int mouseX, int mouseY, int button) {
            focused = onControl(mouseX, mouseY);
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

        @Override public boolean capturingInput() { return focused; }
    }

    /** Builds the rows for every visible setting of a module. */
    public static List<Row> rowsFor(Iterable<Setting<?>> settings) {
        List<Row> rows = new ArrayList<Row>();
        for (Setting<?> setting : settings) {
            Row row = of(setting);
            if (row != null) rows.add(row);
        }
        return rows;
    }
}
