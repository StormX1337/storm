package xyz.stormclient.ui.click;

import xyz.stormclient.ui.UiScale;
import java.util.ArrayList;
import java.util.List;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.Setting;
import xyz.stormclient.ui.Glyphs;
import xyz.stormclient.ui.theme.Theme;
import xyz.stormclient.util.Animation;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.MathUtil;

public final class ModuleButton {

    public static final double ROW_HEIGHT = 14;
    /** Space kept free on the right of a row for the expand arrow. */
    public static final double ARROW_ROOM = 10;

    private static final double PADDING = 8;

    private final Module module;
    private final List<Component> components = new ArrayList<Component>();
    private final Animation openAnim = new Animation(9F);
    private final Animation hoverAnim = new Animation(10F);

    private boolean expanded;
    private double x, y, width;

    public ModuleButton(Module module) {
        this.module = module;
        for (Setting<?> setting : module.settings()) {
            Component component = Components.of(setting);
            if (component != null) components.add(component);
        }
    }

    public Module module() { return module; }

    public boolean expanded() { return expanded; }
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        openAnim.set(expanded);
    }

    public double height() {
        double h = ROW_HEIGHT;
        float t = openAnim.eased();
        if (t > 0.001F) h += settingsHeight() * t;
        return h;
    }

    private double settingsHeight() {
        double h = 3;
        for (Component c : components) {
            if (!c.setting().visible()) continue;
            h += c.height();
        }
        return h + 3;
    }

    public void position(double x, double y, double width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public void render(int mouseX, int mouseY) {
        IRenderer r = Bridge.mc().renderer();
        Theme theme = Storm.get().theme();
        IFontRenderer font = Bridge.mc().font(theme.font(), UiScale.ROW_FONT);

        boolean hover = MathUtil.inside(mouseX, mouseY, x, y, width, ROW_HEIGHT);
        hoverAnim.set(hover);
        float hoverT = hoverAnim.eased();
        float enabledT = module.animation.eased();

        // an enabled module reads as a filled row, a hovered one only lifts a little
        if (enabledT > 0.001F) {
            r.gradientRectH(x, y, width, ROW_HEIGHT,
                    ColorUtil.withAlpha(theme.accent(), (int) (46 * enabledT)),
                    ColorUtil.withAlpha(theme.accent(), (int) (8 * enabledT)));
            r.rect(x, y, 2, ROW_HEIGHT, ColorUtil.fade(theme.accent(), enabledT));
        }
        if (hoverT > 0.001F) {
            r.rect(x + (enabledT > 0.001F ? 2 : 0), y, width - (enabledT > 0.001F ? 2 : 0), ROW_HEIGHT,
                    ColorUtil.withAlpha(theme.text(), (int) (14 * hoverT)));
        }

        double textY = y + (ROW_HEIGHT - font.height()) / 2.0;
        double right = x + width - PADDING;
        if (!components.isEmpty()) right -= ARROW_ROOM;

        // the tag sits against the right edge, so a long mode name can never
        // run over the module next to it
        String tag = module.tag();
        if (tag != null && !tag.isEmpty()) {
            double room = right - (x + PADDING) - font.width(module.name()) - 6;
            String shown = font.width(tag) > room ? font.trim(tag, (int) Math.max(0, room)) : tag;
            if (room > 8) {
                font.draw(shown, right - font.width(shown), textY, theme.textFaint());
                right -= font.width(shown) + 6;
            }
        }

        int textColor = enabledT > 0.5F
                ? ColorUtil.mix(theme.text(), 0xFFFFFFFF, enabledT)
                : ColorUtil.mix(theme.textDim(), theme.text(), Math.max(hoverT, enabledT));
        font.draw(font.trim(module.name(), (int) Math.max(8, right - x - PADDING)),
                x + PADDING, textY, textColor);

        if (!components.isEmpty()) {
            double size = 5;
            Glyphs.chevron(r, x + width - PADDING - size + 2, y + (ROW_HEIGHT - size / 2) / 2, size,
                    expanded, ColorUtil.mix(theme.textFaint(), theme.accent(), Math.max(hoverT, openAnim.eased())));
        }

        float t = openAnim.eased();
        if (t <= 0.001F) return;

        double contentHeight = settingsHeight() * t;
        r.scissorBegin(x, y + ROW_HEIGHT, width, contentHeight);
        r.rect(x, y + ROW_HEIGHT, width, contentHeight, ColorUtil.withAlpha(0xFF000000, 60));
        r.rect(x, y + ROW_HEIGHT, 1, contentHeight, ColorUtil.withAlpha(theme.accent(), 90));

        double cy = y + ROW_HEIGHT + 3;
        for (Component c : components) {
            if (!c.setting().visible()) continue;
            c.position(x + 5, cy, width - 10);
            c.render(mouseX, mouseY);
            cy += c.height();
        }
        r.scissorEnd();
    }

    public void mouseDown(int mouseX, int mouseY, int button) {
        if (MathUtil.inside(mouseX, mouseY, x, y, width, ROW_HEIGHT)) {
            if (button == 0) module.toggle();
            else if (button == 1) setExpanded(!expanded);
            else if (button == 2) module.keybindSetting().setListening(true);
            return;
        }
        if (!expanded) return;
        for (Component c : components) {
            if (c.setting().visible()) c.mouseDown(mouseX, mouseY, button);
        }
    }

    public void mouseUp(int mouseX, int mouseY, int button) {
        for (Component c : components) c.mouseUp(mouseX, mouseY, button);
    }

    public void mouseDragged(int mouseX, int mouseY) {
        if (!expanded) return;
        for (Component c : components) c.mouseDragged(mouseX, mouseY);
    }

    public void keyDown(int key, char typed) {
        if (module.keybindSetting().listening()) {
            module.keybindSetting().set(key == xyz.stormclient.util.Keyboard.KEY_ESCAPE
                    ? xyz.stormclient.util.Keyboard.KEY_NONE : key);
            module.keybindSetting().setListening(false);
            return;
        }
        if (!expanded) return;
        for (Component c : components) c.keyDown(key, typed);
    }

    public boolean capturingInput() {
        if (module.keybindSetting().listening()) return true;
        for (Component c : components) {
            if (c.setting() instanceof xyz.stormclient.setting.KeybindSetting
                    && ((xyz.stormclient.setting.KeybindSetting) c.setting()).listening()) return true;
        }
        return false;
    }
}
