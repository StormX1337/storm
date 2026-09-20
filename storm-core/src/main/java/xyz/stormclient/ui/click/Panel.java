package xyz.stormclient.ui.click;

import java.util.ArrayList;
import java.util.List;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.ui.theme.Theme;
import xyz.stormclient.util.Animation;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.MathUtil;

/** One draggable category window. */
public final class Panel {

    public static final double HEADER = 20;
    public static final double WIDTH  = 122;
    private static final double MAX_HEIGHT = 240;

    private final Category category;
    private final List<ModuleButton> buttons = new ArrayList<ModuleButton>();
    private final Animation openAnim = new Animation(8F);

    private double x, y;
    private boolean open = true;
    private boolean dragging;
    private double dragOffsetX, dragOffsetY;
    private double scroll;

    public Panel(Category category, double x, double y) {
        this.category = category;
        this.x = x;
        this.y = y;
        openAnim.snap(1F);
        for (Module module : Storm.get().modules().byCategory(category)) {
            buttons.add(new ModuleButton(module));
        }
    }

    public Category category() { return category; }
    public double x() { return x; }
    public double y() { return y; }
    public boolean open() { return open; }
    public void setOpen(boolean open) { this.open = open; openAnim.set(open); }
    public void setPosition(double x, double y) { this.x = x; this.y = y; }
    public List<ModuleButton> buttons() { return buttons; }

    public double contentHeight() {
        double h = 0;
        for (ModuleButton b : buttons) h += b.height();
        return h;
    }

    private double visibleHeight() {
        return Math.min(MAX_HEIGHT, contentHeight()) * openAnim.eased();
    }

    public void render(int mouseX, int mouseY, String filter) {
        IRenderer r = Bridge.mc().renderer();
        Theme theme = Storm.get().theme();
        IFontRenderer font = Bridge.mc().font(theme.font(), 17);

        double bodyHeight = visibleHeight();
        double total = HEADER + bodyHeight;

        if (theme.shadows()) r.shadow(x, y, WIDTH, total, theme.radius(), 0x50000000);
        if (theme.blur()) r.blur(x, y, WIDTH, total, 6F);

        r.roundedRect(x, y, WIDTH, total, theme.radius(), ColorUtil.withAlpha(theme.panel(), 240));
        r.roundedRectOutline(x, y, WIDTH, total, theme.radius(), 1F, theme.outline());

        // header
        r.roundedRect(x, y, WIDTH, HEADER, theme.radius(), ColorUtil.withAlpha(theme.panelLight(), 255));
        r.rect(x, y + HEADER - 1, WIDTH, 1, ColorUtil.withAlpha(theme.accent(), 120));
        font.draw(category.icon(), x + 8, y + 6, theme.accent());
        font.draw(category.label(), x + 20, y + 6, theme.text());
        font.draw(open ? "−" : "+", x + WIDTH - 12, y + 6, theme.textDim());

        if (bodyHeight <= 0.01) return;

        r.scissorBegin(x, y + HEADER, WIDTH, bodyHeight);
        double by = y + HEADER - scroll;
        for (ModuleButton button : buttons) {
            if (!matches(button, filter)) continue;
            button.position(x, by, WIDTH);
            if (by + button.height() >= y + HEADER && by <= y + HEADER + bodyHeight) {
                button.render(mouseX, mouseY);
            }
            by += button.height();
        }
        r.scissorEnd();

        // scrollbar
        double content = contentHeight();
        if (content > MAX_HEIGHT) {
            double ratio = MAX_HEIGHT / content;
            double barHeight = bodyHeight * ratio;
            double barY = y + HEADER + (bodyHeight - barHeight) * (scroll / Math.max(1, content - MAX_HEIGHT));
            r.roundedRect(x + WIDTH - 3, barY, 2, barHeight, 1F, ColorUtil.withAlpha(theme.accent(), 140));
        }
    }

    private boolean matches(ModuleButton button, String filter) {
        if (filter == null || filter.isEmpty()) return true;
        return button.module().name().toLowerCase().contains(filter.toLowerCase());
    }

    public void mouseDown(int mouseX, int mouseY, int button) {
        if (MathUtil.inside(mouseX, mouseY, x, y, WIDTH, HEADER)) {
            if (button == 0) {
                dragging = true;
                dragOffsetX = mouseX - x;
                dragOffsetY = mouseY - y;
            } else if (button == 1) {
                setOpen(!open);
            }
            return;
        }
        if (!open) return;
        if (!MathUtil.inside(mouseX, mouseY, x, y + HEADER, WIDTH, visibleHeight())) return;
        for (ModuleButton b : buttons) b.mouseDown(mouseX, mouseY, button);
    }

    public void mouseUp(int mouseX, int mouseY, int button) {
        dragging = false;
        for (ModuleButton b : buttons) b.mouseUp(mouseX, mouseY, button);
    }

    public void mouseDragged(int mouseX, int mouseY) {
        if (dragging) {
            x = mouseX - dragOffsetX;
            y = mouseY - dragOffsetY;
            return;
        }
        for (ModuleButton b : buttons) b.mouseDragged(mouseX, mouseY);
    }

    public void scroll(int amount, int mouseX, int mouseY) {
        if (!MathUtil.inside(mouseX, mouseY, x, y, WIDTH, HEADER + visibleHeight())) return;
        double max = Math.max(0, contentHeight() - MAX_HEIGHT);
        scroll = MathUtil.clamp(scroll - amount * 12, 0, max);
    }

    public void keyDown(int key, char typed) {
        for (ModuleButton b : buttons) b.keyDown(key, typed);
    }

    public boolean capturingInput() {
        for (ModuleButton b : buttons) if (b.capturingInput()) return true;
        return false;
    }
}
