package xyz.stormclient.ui.click;

import xyz.stormclient.ui.UiScale;
import java.util.ArrayList;
import java.util.List;

import xyz.stormclient.Storm;
import xyz.stormclient.StormInfo;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.ui.Glyphs;
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

    private static final double TOP_BAR = 22;
    private static final double BOTTOM_BAR = 15;
    private static final double GAP = 6;
    private static final double MARGIN = 10;
    private static final double SEARCH_WIDTH = 132;

    private final List<Panel> panels = new ArrayList<Panel>();
    private final Animation fade = new Animation(7F);
    private final Animation searchAnim = new Animation(10F);

    private String search = "";
    private boolean searchFocused;

    public ClickGuiScreen() {
        for (Category category : Category.values()) {
            panels.add(new Panel(category, MARGIN, TOP_BAR + GAP));
        }
    }

    public List<Panel> panels() { return panels; }

    @Override public void onOpen(int width, int height) {
        super.onOpen(width, height);
        fade.set(true);
        layout(true);
    }

    /**
     * Places the panels in rows, wrapping when they run out of width.
     *
     * <p>A second pass shares the remaining height between the rows, so at a
     * large GUI scale the later categories still land on screen instead of
     * below it; whatever does not fit scrolls inside its panel.
     */
    private void layout(boolean force) {
        double available = height - TOP_BAR - BOTTOM_BAR - GAP * 2;

        // pass one: which panel goes in which row
        List<List<Panel>> rows = new ArrayList<List<Panel>>();
        List<Panel> row = new ArrayList<Panel>();
        double x = MARGIN;
        for (Panel panel : panels) {
            double w = panel.width();
            if (!row.isEmpty() && x + w > width - MARGIN) {
                rows.add(row);
                row = new ArrayList<Panel>();
                x = MARGIN;
            }
            row.add(panel);
            x += w + GAP;
        }
        if (!row.isEmpty()) rows.add(row);

        // pass two: every row gets an equal share of what is left
        double share = (available - (rows.size() - 1) * GAP) / rows.size();
        double y = TOP_BAR + GAP;
        for (List<Panel> current : rows) {
            double tallest = 0;
            x = MARGIN;
            for (Panel panel : current) {
                panel.setMaxHeight(share - Panel.HEADER);
                boolean offScreen = panel.x() + panel.width() > width || panel.x() < 0
                        || panel.y() < TOP_BAR || panel.y() > height - BOTTOM_BAR - Panel.HEADER;
                if (force || offScreen) panel.setPosition(x, y);
                tallest = Math.max(tallest, Math.min(share, Panel.HEADER + panel.contentHeight()));
                x += panel.width() + GAP;
            }
            y += tallest + GAP;
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

        // the world stays readable behind the menu: a dark wash rather than a lid,
        // with a hint of the accent bleeding down from the top bar
        r.gradientRect(0, 0, width, height,
                ColorUtil.withAlpha(theme().background(), (int) (185 * t)),
                ColorUtil.withAlpha(theme().background(), (int) (105 * t)));
        r.gradientRect(0, TOP_BAR, width, 90,
                ColorUtil.withAlpha(theme().accent(), (int) (26 * t)),
                ColorUtil.withAlpha(theme().accent(), 0));

        renderTopBar(r, mouseX, mouseY, t);

        for (Panel panel : panels) panel.render(mouseX, mouseY, search);

        renderBottomBar(r, t);
    }

    private void renderTopBar(IRenderer r, int mouseX, int mouseY, float t) {
        IFontRenderer title = font(UiScale.TITLE_FONT);
        IFontRenderer small = font(UiScale.SMALL_FONT);

        r.gradientRect(0, 0, width, TOP_BAR,
                ColorUtil.fade(theme().panelLight(), t * 0.98F),
                ColorUtil.fade(theme().panel(), t * 0.98F));
        r.gradientRectH(0, TOP_BAR - 1, width, 1,
                ColorUtil.withAlpha(theme().accent(), (int) (200 * t)),
                ColorUtil.withAlpha(theme().accent(), (int) (30 * t)));

        Glyphs.bolt(r, MARGIN, (TOP_BAR - 14) / 2, 1.0, theme().accent());
        double textX = MARGIN + 12;
        title.draw(StormInfo.NAME.toUpperCase(), textX, (TOP_BAR - title.height()) / 2.0, theme().text());
        textX += title.width(StormInfo.NAME.toUpperCase()) + 5;
        small.draw("v" + StormInfo.VERSION, textX,
                (TOP_BAR - small.height()) / 2.0 + 1, theme().textFaint());

        // search field
        double sx = width - SEARCH_WIDTH - MARGIN;
        double sh = 14;
        double sy = (TOP_BAR - sh) / 2;
        boolean hover = MathUtil.inside(mouseX, mouseY, sx, sy, SEARCH_WIDTH, sh);
        searchAnim.set(searchFocused || hover);
        float focus = searchAnim.eased();

        r.roundedRect(sx, sy, SEARCH_WIDTH, sh, (float) (sh / 2), ColorUtil.withAlpha(theme().panelDark(), 235));
        r.roundedRectOutline(sx, sy, SEARCH_WIDTH, sh, (float) (sh / 2), 1F,
                ColorUtil.mix(theme().outline(), theme().accent(), focus));
        Glyphs.search(r, sx + 5, sy + (sh - 7) / 2, 7,
                ColorUtil.mix(theme().textFaint(), theme().accent(), focus));

        boolean empty = search.isEmpty() && !searchFocused;
        String shown = empty ? "Search modules" : search + (searchFocused && blink() ? "|" : "");
        small.draw(small.trim(shown, (int) SEARCH_WIDTH - 22), sx + 15,
                sy + (sh - small.height()) / 2.0, empty ? theme().textFaint() : theme().text());

        // enabled counter, as a pill so it reads as a badge and not as a label
        int enabled = Storm.get().modules().enabled().size();
        String stats = enabled + " enabled";
        double pw = small.width(stats) + 12;
        double px = sx - pw - 8;
        r.roundedRect(px, sy, pw, sh, (float) (sh / 2), ColorUtil.withAlpha(theme().accent(), 38));
        small.draw(stats, px + 6, sy + (sh - small.height()) / 2.0, theme().accent());
    }

    private boolean blink() {
        return (System.currentTimeMillis() / 500) % 2 == 0;
    }

    private void renderBottomBar(IRenderer r, float t) {
        IFontRenderer small = font(UiScale.SMALL_FONT);
        double y = height - BOTTOM_BAR;

        r.rect(0, y, width, BOTTOM_BAR, ColorUtil.fade(theme().panel(), t * 0.92F));
        r.gradientRectH(0, y, width, 1,
                ColorUtil.withAlpha(theme().accent(), (int) (30 * t)),
                ColorUtil.withAlpha(theme().accent(), (int) (150 * t)));

        double ty = y + (BOTTOM_BAR - small.height()) / 2.0;
        String[] hints = { "left click toggle", "right click settings", "middle click bind" };
        double hx = MARGIN;
        for (int i = 0; i < hints.length; i++) {
            if (i > 0) {
                r.rect(hx, y + BOTTOM_BAR / 2 - 1, 2, 2, theme().textFaint());
                hx += 8;
            }
            small.draw(hints[i], hx, ty, theme().textFaint());
            hx += small.width(hints[i]) + 6;
        }

        String right = StormInfo.FULL_NAME;
        small.draw(right, width - small.width(right) - MARGIN, ty, theme().textDim());

        String config = Storm.get().config().currentName();
        small.draw(config, width - small.width(right) - small.width(config) - MARGIN - 12, ty,
                theme().accent());
    }

    @Override public void mouseDown(int mouseX, int mouseY, int button) {
        double sh = 14;
        searchFocused = MathUtil.inside(mouseX, mouseY,
                width - SEARCH_WIDTH - MARGIN, (TOP_BAR - sh) / 2, SEARCH_WIDTH, sh);
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
