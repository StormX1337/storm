package xyz.stormclient.ui.click;

import java.util.ArrayList;
import java.util.List;

import xyz.stormclient.Storm;
import xyz.stormclient.StormInfo;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.ui.Screen;
import xyz.stormclient.util.Animation;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.Keyboard;
import xyz.stormclient.util.MathUtil;

/**
 * Storm's main menu: one draggable panel per category, a live search bar and a
 * status strip along the bottom.
 */
public final class ClickGuiScreen extends Screen {

    private final List<Panel> panels = new ArrayList<Panel>();
    private final Animation fade = new Animation(7F);

    private String search = "";
    private boolean searchFocused;

    public ClickGuiScreen() {
        double x = 12;
        for (Category category : Category.values()) {
            panels.add(new Panel(category, x, 34));
            x += Panel.WIDTH + 8;
        }
    }

    public List<Panel> panels() { return panels; }

    @Override public void onOpen(int width, int height) {
        super.onOpen(width, height);
        fade.set(true);
        layoutIfNeeded();
    }

    private void layoutIfNeeded() {
        double x = 12;
        double y = 34;
        for (Panel panel : panels) {
            if (panel.x() + Panel.WIDTH > width || panel.x() < 0) {
                panel.setPosition(x, y);
            }
            x += Panel.WIDTH + 8;
            if (x + Panel.WIDTH > width) { x = 12; y += 60; }
        }
    }

    @Override public void onClose() {
        fade.set(false);
        Storm.get().config().saveCurrent();
    }

    @Override public void render(int mouseX, int mouseY, float partialTicks) {
        IRenderer r = r();
        float t = fade.eased();
        if (t < 0.01F) t = 0.01F;

        r.rect(0, 0, width, height, ColorUtil.withAlpha(theme().overlay(), (int) (140 * t)));

        renderHeader(r, mouseX, mouseY, t);

        for (Panel panel : panels) panel.render(mouseX, mouseY, search);

        renderFooter(r, t);
    }

    private void renderHeader(IRenderer r, int mouseX, int mouseY, float t) {
        IFontRenderer title = font(24);
        IFontRenderer small = font(15);

        r.rect(0, 0, width, 24, ColorUtil.fade(theme().panel(), t * 0.95F));
        r.rect(0, 24, width, 1, ColorUtil.withAlpha(theme().accent(), (int) (160 * t)));

        // logo: a stylised bolt drawn from two triangles' worth of rectangles
        drawBolt(r, 10, 6, 1.0, theme().accent());
        title.draw(StormInfo.NAME.toUpperCase(), 24, 4, theme().text());
        small.draw("v" + StormInfo.VERSION, 24 + title.width(StormInfo.NAME.toUpperCase()) + 4, 9, theme().textFaint());

        // search bar
        double sw = 150;
        double sx = width - sw - 10;
        boolean hover = MathUtil.inside(mouseX, mouseY, sx, 4, sw, 16);
        r.roundedRect(sx, 4, sw, 16, 5F, ColorUtil.withAlpha(theme().panelDark(), 220));
        if (searchFocused || hover) r.roundedRectOutline(sx, 4, sw, 16, 5F, 1F, theme().accent());
        String shown = search.isEmpty() && !searchFocused ? "Search modules..." : search + (searchFocused ? "_" : "");
        small.draw(shown, sx + 7, 8, search.isEmpty() && !searchFocused ? theme().textFaint() : theme().text());

        int enabled = Storm.get().modules().enabled().size();
        String stats = enabled + " enabled";
        small.draw(stats, sx - small.width(stats) - 12, 8, theme().textDim());
    }

    private void renderFooter(IRenderer r, float t) {
        IFontRenderer small = font(15);
        double y = height - 16;
        r.rect(0, y, width, 16, ColorUtil.fade(theme().panel(), t * 0.9F));
        r.rect(0, y, width, 1, ColorUtil.withAlpha(theme().accent(), (int) (90 * t)));

        String left = "left click toggle  \u00b7  right click settings  \u00b7  middle click bind";
        small.draw(left, 8, y + 4, theme().textFaint());

        String right = "config: " + Storm.get().config().currentName()
                + "   \u00b7   " + StormInfo.FULL_NAME;
        small.draw(right, width - small.width(right) - 8, y + 4, theme().textDim());
    }

    /** The Storm bolt, drawn as geometry so the client needs no logo texture. */
    public static void drawBolt(IRenderer r, double x, double y, double scale, int color) {
        double[][] rows = {
                { 6, 0, 4, 2 }, { 4, 2, 5, 2 }, { 3, 4, 5, 2 }, { 2, 6, 7, 2 },
                { 4, 8, 4, 2 }, { 3, 10, 4, 2 }, { 2, 12, 3, 2 }
        };
        for (double[] row : rows) {
            r.rect(x + row[0] * scale, y + row[1] * scale, row[2] * scale, row[3] * scale, color);
        }
    }

    @Override public void mouseDown(int mouseX, int mouseY, int button) {
        double sw = 150;
        searchFocused = MathUtil.inside(mouseX, mouseY, width - sw - 10, 4, sw, 16);
        if (searchFocused && button == 1) search = "";

        for (int i = panels.size() - 1; i >= 0; i--) panels.get(i).mouseDown(mouseX, mouseY, button);
    }

    @Override public void mouseUp(int mouseX, int mouseY, int button) {
        for (Panel panel : panels) panel.mouseUp(mouseX, mouseY, button);
    }

    @Override public void mouseDragged(int mouseX, int mouseY, int button) {
        for (Panel panel : panels) panel.mouseDragged(mouseX, mouseY);
    }

    @Override public void mouseScroll(int amount) {
        int mouseX = mc().input().mouseX();
        int mouseY = mc().input().mouseY();
        for (Panel panel : panels) panel.scroll(amount, mouseX, mouseY);
    }

    @Override public void keyDown(int keyCode, char typed) {
        for (Panel panel : panels) {
            if (panel.capturingInput()) { panel.keyDown(keyCode, typed); return; }
        }

        if (searchFocused) {
            if (keyCode == Keyboard.KEY_BACK) {
                if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            } else if (keyCode == Keyboard.KEY_RETURN) {
                toggleFirstMatch();
            } else if (typed >= 32 && typed != 127) {
                search += typed;
            }
            return;
        }
        for (Panel panel : panels) panel.keyDown(keyCode, typed);
    }

    private void toggleFirstMatch() {
        List<Module> found = Storm.get().modules().search(search);
        if (!found.isEmpty()) found.get(0).toggle();
        search = "";
    }

    @Override public boolean closeOnEscape() {
        for (Panel panel : panels) if (panel.capturingInput()) return false;
        return !searchFocused || search.isEmpty();
    }
}
