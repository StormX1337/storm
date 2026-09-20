package xyz.stormclient.ui.click;

import java.util.ArrayList;
import java.util.List;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.Setting;
import xyz.stormclient.ui.theme.Theme;
import xyz.stormclient.util.Animation;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.MathUtil;

public final class ModuleButton {

    public static final double ROW_HEIGHT = 16;

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
        double h = 2;
        for (Component c : components) {
            if (!c.setting().visible()) continue;
            h += c.height();
        }
        return h + 2;
    }

    public void position(double x, double y, double width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public void render(int mouseX, int mouseY) {
        IRenderer r = Bridge.mc().renderer();
        Theme theme = Storm.get().theme();
        IFontRenderer font = Bridge.mc().font(theme.font(), 16);

        boolean hover = MathUtil.inside(mouseX, mouseY, x, y, width, ROW_HEIGHT);
        hoverAnim.set(hover);
        float hoverT = hoverAnim.eased();
        float enabledT = module.animation.eased();

        if (hoverT > 0.001F) {
            r.rect(x, y, width, ROW_HEIGHT, ColorUtil.withAlpha(theme.accent(), (int) (28 * hoverT)));
        }
        if (enabledT > 0.001F) {
            r.rect(x, y, 2.0 + hoverT, ROW_HEIGHT, ColorUtil.fade(theme.accent(), enabledT));
        }

        int textColor = ColorUtil.mix(theme.textDim(), theme.text(), Math.max(hoverT, enabledT));
        font.draw(module.name(), x + 8, y + 4, textColor);

        String tag = module.tag();
        if (tag != null && !tag.isEmpty()) {
            font.draw(tag, x + 10 + font.width(module.name()), y + 4, theme.textFaint());
        }
        if (!components.isEmpty()) {
            String arrow = expanded ? "\u25b4" : "\u25be";
            font.draw(arrow, x + width - 12, y + 4, theme.textFaint());
        }

        float t = openAnim.eased();
        if (t <= 0.001F) return;

        double contentHeight = settingsHeight() * t;
        r.scissorBegin(x, y + ROW_HEIGHT, width, contentHeight);
        r.rect(x, y + ROW_HEIGHT, width, contentHeight, ColorUtil.withAlpha(0xFF000000, 45));

        double cy = y + ROW_HEIGHT + 2;
        for (Component c : components) {
            if (!c.setting().visible()) continue;
            c.position(x + 6, cy, width - 12);
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
