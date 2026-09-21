package xyz.stormclient.ui.menu;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.ui.theme.Theme;
import xyz.stormclient.util.MathUtil;

/**
 * One view inside the menu window.
 *
 * <p>A page owns the area to the right of the sidebar. The frame gives it that
 * rectangle and handles the chrome; the page fills the body and says how tall
 * its content is, which is all the scroll bar needs to know.
 */
public abstract class Page {

    protected double x, y, width, height;

    /** Where the wheel put us, and where the body is actually drawn. */
    private double scrollTarget;
    private double scroll;
    private double contentHeight;
    private long lastFrame = System.nanoTime();

    /** Shown as the page heading. */
    public abstract String title();
    /** The line under the heading, or null for none. */
    public String subtitle() { return null; }
    /** Whether the page wants the search field in the header. */
    public boolean searchable() { return false; }

    public void setBounds(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** Draws the body. Implementations start at {@link #bodyTop()} and go down. */
    protected abstract void renderBody(int mouseX, int mouseY, String search);

    /** How tall the body wants to be, set by the last render. */
    protected void setContentHeight(double contentHeight) { this.contentHeight = contentHeight; }

    public void render(int mouseX, int mouseY, String search) {
        IRenderer r = r();
        stepScroll();
        r.scissorBegin(x, y, width, height);
        renderBody(mouseX, mouseY, search);
        r.scissorEnd();
        renderScrollbar(r);
    }

    /**
     * Eases towards where the wheel asked to be. A list that snaps loses the
     * reader's place; one that glides keeps it.
     */
    private void stepScroll() {
        long now = System.nanoTime();
        float delta = Math.min(0.25F, (now - lastFrame) / 1_000_000_000F);
        lastFrame = now;

        scrollTarget = MathUtil.clamp(scrollTarget, 0, maxScroll());
        double remaining = scrollTarget - scroll;
        if (Math.abs(remaining) < 0.35) { scroll = scrollTarget; return; }
        scroll += remaining * Math.min(1.0, delta * 14);
    }

    private void renderScrollbar(IRenderer r) {
        double max = maxScroll();
        if (max <= 0) return;
        double barHeight = Math.max(18, height * (height / contentHeight));
        double barY = y + (height - barHeight) * (scroll / max);
        r.roundedRect(x + width - 3, y, 2, height, 1F,
                ColorUtils.withAlpha(theme().text(), 16));
        r.roundedRect(x + width - 3, barY, 2, barHeight, 1F,
                ColorUtils.withAlpha(theme().text(), 70));
    }

    /** Where the body starts once the page has scrolled. */
    protected double bodyTop() { return y - scroll; }

    public double maxScroll() { return Math.max(0, contentHeight - height); }

    public void scroll(int amount, int mouseX, int mouseY) {
        if (!inside(mouseX, mouseY)) return;
        scrollTarget = MathUtil.clamp(scrollTarget - amount * 34, 0, maxScroll());
    }

    public void resetScroll() { scroll = scrollTarget = 0; }

    /** Replays whatever entrance the page has. Called when it is selected. */
    public void onShown() { }

    protected boolean inside(int mouseX, int mouseY) {
        return MathUtil.inside(mouseX, mouseY, x, y, width, height);
    }

    public void mouseDown(int mouseX, int mouseY, int button) { }
    public void mouseUp(int mouseX, int mouseY, int button)   { }
    public void mouseDragged(int mouseX, int mouseY)          { }
    public void keyDown(int key, char typed)                  { }
    /** True while the page is waiting for a raw key, so escape must not close. */
    public boolean capturingInput()                           { return false; }

    protected IRenderer r()    { return Bridge.mc().renderer(); }
    protected Theme theme()    { return Storm.get().theme(); }
    protected IFontRenderer font(int size) { return Bridge.mc().font(theme().font(), size); }

    /** Small local alias so pages do not all import the colour helper. */
    static final class ColorUtils {
        static int withAlpha(int argb, int alpha) {
            return xyz.stormclient.util.ColorUtil.withAlpha(argb, alpha);
        }
    }
}
