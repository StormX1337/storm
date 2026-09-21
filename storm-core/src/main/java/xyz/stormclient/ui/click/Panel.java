package xyz.stormclient.ui.click;

import xyz.stormclient.ui.UiScale;
import java.util.ArrayList;
import java.util.List;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.ui.Glyphs;
import xyz.stormclient.ui.theme.Theme;
import xyz.stormclient.util.Animation;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.MathUtil;

/** One draggable category window. */
public final class Panel {

    public static final double HEADER = 19;
    /** Only a starting point: every panel sizes itself to its longest row. */
    public static final double WIDTH = 116;

    private static final double MIN_WIDTH = 96;
    private static final double MAX_WIDTH = 160;
    private static final double PADDING = 8;

    private final Category category;
    private final List<ModuleButton> buttons = new ArrayList<ModuleButton>();
    private final Animation openAnim = new Animation(8F);
    private final Animation hoverAnim = new Animation(9F);

    private double x, y;
    private double width = WIDTH;
    private boolean measured;
    private double maxHeight = 240;
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
    public void setMaxHeight(double maxHeight) { this.maxHeight = Math.max(60, maxHeight); }
    public List<ModuleButton> buttons() { return buttons; }

    /**
     * Wide enough for the longest module row, so nothing ever has to be drawn
     * over its neighbour. Measured once, because the font does not change while
     * the menu is open.
     */
    public double width() {
        if (measured) return width;
        IFontRenderer font = Bridge.mc().font(Storm.get().theme().font(), UiScale.ROW_FONT);
        IFontRenderer head = Bridge.mc().font(Storm.get().theme().font(), UiScale.HEADER_FONT);

        double widest = head.width(category.label()) + HEADER + 14;
        for (ModuleButton button : buttons) {
            double row = font.width(button.module().name());
            String tag = button.module().tag();
            if (tag != null && !tag.isEmpty()) row += 6 + font.width(tag);
            widest = Math.max(widest, row + PADDING * 2 + ModuleButton.ARROW_ROOM);
        }
        width = MathUtil.clamp(Math.ceil(widest), MIN_WIDTH, MAX_WIDTH);
        measured = true;
        return width;
    }

    public double contentHeight() {
        double h = 0;
        for (ModuleButton b : buttons) h += b.height();
        return h;
    }

    private double bodyLimit() {
        return Math.min(maxHeight, contentHeight());
    }

    private double visibleHeight() {
        return bodyLimit() * openAnim.eased();
    }

    public void render(int mouseX, int mouseY, String filter) {
        IRenderer r = Bridge.mc().renderer();
        Theme theme = Storm.get().theme();
        IFontRenderer font = Bridge.mc().font(theme.font(), UiScale.HEADER_FONT);

        double w = width();
        double bodyHeight = visibleHeight();
        double total = HEADER + bodyHeight;

        hoverAnim.set(MathUtil.inside(mouseX, mouseY, x, y, w, HEADER));
        float hover = hoverAnim.eased();

        if (theme.shadows()) r.shadow(x, y, w, total, theme.radius(), 0x66000000);
        if (theme.blur()) r.blur(x, y, w, total, 6F);

        r.roundedRect(x, y, w, total, theme.radius(), ColorUtil.withAlpha(theme.panel(), 246));

        // header: a slight lift out of the body, with the accent underlining it
        r.roundedRect(x, y, w, HEADER, theme.radius(), theme.panelLight());
        r.rect(x, y + HEADER - 4, w, 4, theme.panelLight());
        r.gradientRectH(x, y + HEADER - 1, w, 1,
                ColorUtil.withAlpha(theme.accent(), (int) (120 + 110 * hover)),
                ColorUtil.withAlpha(theme.accent(), 20));

        double iconSize = 9;
        Glyphs.category(r, category, x + PADDING, y + (HEADER - iconSize) / 2, iconSize,
                ColorUtil.mix(theme.accent(), 0xFFFFFFFF, hover * 0.35F));
        font.draw(category.label(), x + PADDING + iconSize + 6,
                y + (HEADER - font.height()) / 2.0, theme.text());

        double markSize = 6;
        Glyphs.collapse(r, x + w - PADDING - markSize, y + (HEADER - markSize) / 2, markSize, open,
                ColorUtil.mix(theme.textFaint(), theme.text(), hover));

        r.roundedRectOutline(x, y, w, total, theme.radius(), 1F, theme.outline());

        if (bodyHeight <= 0.01) return;

        r.scissorBegin(x, y + HEADER, w, bodyHeight);
        double by = y + HEADER + 2 - scroll;
        for (ModuleButton button : buttons) {
            if (!matches(button, filter)) continue;
            button.position(x, by, w);
            if (by + button.height() >= y + HEADER && by <= y + HEADER + bodyHeight) {
                button.render(mouseX, mouseY);
            }
            by += button.height();
        }
        r.scissorEnd();

        // scrollbar, only while there is something to scroll
        double content = contentHeight() + 4;
        double limit = bodyLimit();
        if (content > limit) {
            double ratio = limit / content;
            double barHeight = Math.max(12, bodyHeight * ratio);
            double travel = bodyHeight - barHeight;
            double barY = y + HEADER + travel * (scroll / Math.max(1, content - limit));
            r.roundedRect(x + w - 3.5, y + HEADER + 1, 2, bodyHeight - 2, 1F,
                    ColorUtil.withAlpha(theme.text(), 18));
            r.roundedRect(x + w - 3.5, barY, 2, barHeight, 1F, ColorUtil.withAlpha(theme.accent(), 190));
        }
    }

    private boolean matches(ModuleButton button, String filter) {
        if (filter == null || filter.isEmpty()) return true;
        return button.module().name().toLowerCase().contains(filter.toLowerCase());
    }

    public void mouseDown(int mouseX, int mouseY, int button) {
        double w = width();
        if (MathUtil.inside(mouseX, mouseY, x, y, w, HEADER)) {
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
        if (!MathUtil.inside(mouseX, mouseY, x, y + HEADER, w, visibleHeight())) return;
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
        if (!MathUtil.inside(mouseX, mouseY, x, y, width(), HEADER + visibleHeight())) return;
        double max = Math.max(0, contentHeight() + 4 - bodyLimit());
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
