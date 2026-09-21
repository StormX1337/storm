package xyz.stormclient.ui.menu;

import java.util.ArrayList;
import java.util.List;

import xyz.stormclient.Storm;
import xyz.stormclient.StormInfo;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.module.Category;
import xyz.stormclient.ui.Glyphs;
import xyz.stormclient.ui.Screen;
import xyz.stormclient.ui.UiScale;
import xyz.stormclient.util.Animation;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.Keyboard;
import xyz.stormclient.util.MathUtil;

/**
 * Storm's menu: one window with a sidebar on the left and a page on the right.
 *
 * <p>Unlike the draggable panels, everything lives in one place here, so a long
 * settings list has room to show what each option actually does instead of
 * squeezing a name and a value onto one line.
 */
public final class MenuScreen extends Screen {

    private static final double SIDEBAR = 112;
    private static final double HEADER = 44;
    private static final double PAD = 14;
    private static final double ITEM = 19;

    private final List<Item> items = new ArrayList<Item>();
    private final Animation fade = new Animation(7F);
    private final Animation searchAnim = new Animation(10F);

    private Item selected;
    private String search = "";
    private boolean searchFocused;

    private double windowX, windowY, windowW, windowH;

    /** Where the sidebar's selection pill is, and where it is heading. */
    private double pillY = -1;
    private double pillTarget;
    private long lastFrame = System.nanoTime();
    /** Fades the content while a new page takes over. */
    private final Animation pageFade = new Animation(14F);

    public MenuScreen() {
        items.add(Item.label("MODULES"));
        for (Category category : Category.values()) {
            items.add(Item.category(category, new ModulesPage(category)));
        }
        items.add(Item.label("GENERAL"));
        items.add(Item.page("Settings", Icon.GEAR, new MenuPages.SettingsPage(
                "Client Settings", "how Storm behaves", clientSettings())));
        items.add(Item.page("Theme", Icon.PALETTE, new MenuPages.SettingsPage(
                "Theme", "how Storm looks", themeSettings())));
        items.add(Item.page("Configs", Icon.FOLDER, new MenuPages.ConfigsPage()));
        items.add(Item.page("Keybinds", Icon.KEYBOARD, new MenuPages.KeybindsPage()));
        items.add(Item.page("Licence", Icon.KEY, new MenuPages.LicencePage()));

        for (Item item : items) {
            if (item.page != null) { selected = item; break; }
        }
        pageFade.snap(1F);
    }

    private static Iterable<xyz.stormclient.setting.Setting<?>> themeSettings() {
        xyz.stormclient.module.Module gui = Storm.get().modules().byName("ClickGUI");
        return gui == null
                ? new ArrayList<xyz.stormclient.setting.Setting<?>>()
                : gui.settings();
    }

    private static Iterable<xyz.stormclient.setting.Setting<?>> clientSettings() {
        return Storm.get().settings();
    }

    @Override public void onOpen(int width, int height) {
        super.onOpen(width, height);
        fade.set(true);
        pageFade.snap(0F);
        pageFade.set(true);
        pillY = -1;
        if (selected != null && selected.page != null) selected.page.onShown();
        layout();
    }

    private void layout() {
        windowW = MathUtil.clamp(width - 60, 360, 560);
        windowH = MathUtil.clamp(height - 50, 200, 340);
        windowX = (width - windowW) / 2;
        windowY = (height - windowH) / 2;

        if (selected != null && selected.page != null) {
            selected.page.setBounds(
                    windowX + SIDEBAR + PAD,
                    windowY + HEADER,
                    windowW - SIDEBAR - PAD * 2,
                    windowH - HEADER - PAD);
        }
    }

    @Override public void onClose() {
        fade.set(false);
        Storm.get().config().saveCurrent();
    }

    @Override public void render(int mouseX, int mouseY, float partialTicks) {
        IRenderer r = r();
        layout();
        float t = fade.eased();
        if (t < 0.01F) t = 0.01F;

        r.gradientRect(0, 0, width, height,
                ColorUtil.withAlpha(0xFF000000, (int) (150 * t)),
                ColorUtil.withAlpha(theme().background(), (int) (120 * t)));

        // the window itself, lifted a little on the way in
        double slide = (1 - t) * 10;
        double wx = windowX, wy = windowY + slide;

        if (theme().blur()) r.blur(wx, wy, windowW, windowH, 8F);
        if (theme().shadows()) r.shadow(wx, wy, windowW, windowH, 10F,
                ColorUtil.withAlpha(0xFF000000, (int) (150 * t)));
        r.roundedRect(wx, wy, windowW, windowH, 10F, ColorUtil.fade(theme().panel(), t));
        r.roundedRectOutline(wx, wy, windowW, windowH, 10F, 1F, ColorUtil.fade(theme().outline(), t));

        renderSidebar(r, wx, wy, mouseX, mouseY);
        renderHeader(r, wx, wy, mouseX, mouseY);

        if (selected != null && selected.page != null) {
            float page = pageFade.easedOut();
            r.push();
            r.translate((1 - page) * 12, 0, 0);
            selected.page.setBounds(wx + SIDEBAR + PAD, wy + HEADER,
                    windowW - SIDEBAR - PAD * 2, windowH - HEADER - PAD);
            selected.page.render(mouseX, mouseY, selected.page.searchable() ? search : "");
            r.pop();
        }
    }

    /** Eases the sidebar pill towards the selected row. */
    private void stepPill(double target) {
        long now = System.nanoTime();
        float delta = Math.min(0.25F, (now - lastFrame) / 1_000_000_000F);
        lastFrame = now;
        pillTarget = target;
        if (pillY < 0) { pillY = target; return; }
        pillY += (pillTarget - pillY) * Math.min(1.0, delta * 18);
    }

    private void renderSidebar(IRenderer r, double wx, double wy, int mouseX, int mouseY) {
        IFontRenderer name = font(UiScale.HEADER_FONT);
        IFontRenderer small = font(UiScale.COMPONENT_FONT);

        r.roundedRect(wx, wy, SIDEBAR, windowH, 10F, ColorUtil.withAlpha(theme().panelDark(), 190));
        r.rect(wx + SIDEBAR - 10, wy, 10, windowH, ColorUtil.withAlpha(theme().panelDark(), 190));
        r.rect(wx + SIDEBAR, wy + 8, 1, windowH - 16, ColorUtil.withAlpha(theme().text(), 16));

        Glyphs.bolt(r, wx + 12, wy + 11, 1.0, theme().accent());
        name.draw(StormInfo.NAME + " Client", wx + 26, wy + 11, theme().text());
        small.draw("v" + StormInfo.VERSION, wx + 26, wy + 11 + name.height(), theme().textFaint());

        // the pill glides between rows, so the eye follows the selection
        double selectedY = wy + 36;
        double y = wy + 36;
        for (Item item : items) {
            if (item == selected) selectedY = y;
            y += item.page == null ? 16 : ITEM;
        }
        stepPill(selectedY);
        r.roundedRect(wx + 6, pillY, SIDEBAR - 12, ITEM, 5F,
                ColorUtil.withAlpha(theme().accent(), 42));
        r.roundedRect(wx + 6, pillY + 4, 2, ITEM - 8, 1F, theme().accent());

        y = wy + 36;
        for (Item item : items) {
            if (item.page == null) {
                small.draw(item.label, wx + 14, y + 5, theme().textFaint());
                y += 16;
                continue;
            }

            boolean active = item == selected;
            boolean hover = MathUtil.inside(mouseX, mouseY, wx + 6, y, SIDEBAR - 12, ITEM);
            item.hover.set(hover);
            float h = item.hover.eased();

            if (h > 0.01F && !active) {
                r.roundedRect(wx + 6, y, SIDEBAR - 12, ITEM, 5F,
                        ColorUtil.withAlpha(theme().text(), (int) (16 * h)));
            }

            // hovering nudges the row towards its label, which reads as a press
            double nudge = h * 1.5;
            int tint = active ? theme().accent() : ColorUtil.mix(theme().textDim(), theme().text(), h);
            item.drawIcon(r, wx + 14 + nudge, y + (ITEM - 9) / 2, 9, tint);
            small.draw(item.label, wx + 28 + nudge, y + (ITEM - small.height()) / 2.0,
                    active ? theme().text() : ColorUtil.mix(theme().textDim(), theme().text(), h));

            if (item.category != null) {
                String count = String.valueOf(ModulesPage.count(item.category));
                int on = ModulesPage.enabledCount(item.category);
                small.draw(count, wx + SIDEBAR - 14 - small.width(count),
                        y + (ITEM - small.height()) / 2.0,
                        on > 0 ? theme().accent() : theme().textFaint());
            }
            y += ITEM;
        }
    }

    private void renderHeader(IRenderer r, double wx, double wy, int mouseX, int mouseY) {
        if (selected == null || selected.page == null) return;
        IFontRenderer title = font(UiScale.TITLE_FONT);
        IFontRenderer small = font(UiScale.COMPONENT_FONT);

        double tx = wx + SIDEBAR + PAD;
        title.draw(selected.page.title(), tx, wy + 13, theme().text());
        String subtitle = selected.page.subtitle();
        if (subtitle != null) {
            small.draw(subtitle, tx, wy + 13 + title.height() + 1, theme().textFaint());
        }

        if (!selected.page.searchable()) return;
        double sw = 118, sh = 15;
        double sx = wx + windowW - PAD - sw;
        double sy = wy + 15;
        boolean hover = MathUtil.inside(mouseX, mouseY, sx, sy, sw, sh);
        searchAnim.set(searchFocused || hover);
        float focus = searchAnim.eased();

        r.roundedRect(sx, sy, sw, sh, (float) (sh / 2), ColorUtil.withAlpha(0xFF000000, 95));
        r.roundedRectOutline(sx, sy, sw, sh, (float) (sh / 2), 1F,
                ColorUtil.mix(theme().outline(), theme().accent(), focus));
        Glyphs.search(r, sx + 6, sy + (sh - 7) / 2, 7,
                ColorUtil.mix(theme().textFaint(), theme().accent(), focus));
        boolean empty = search.isEmpty() && !searchFocused;
        String shown = empty ? "Search modules" : search + (searchFocused ? "|" : "");
        small.draw(small.trim(shown, (int) sw - 24), sx + 16, sy + (sh - small.height()) / 2.0,
                empty ? theme().textFaint() : theme().text());
    }

    // ------------------------------------------------------------------
    @Override public void mouseDown(int mouseX, int mouseY, int button) {
        double sw = 118, sh = 15;
        searchFocused = MathUtil.inside(mouseX, mouseY,
                windowX + windowW - PAD - sw, windowY + 15, sw, sh);
        if (searchFocused) {
            if (button == 1) search = "";
            return;
        }

        double y = windowY + 36;
        for (Item item : items) {
            if (item.page == null) { y += 16; continue; }
            if (MathUtil.inside(mouseX, mouseY, windowX + 6, y, SIDEBAR - 12, ITEM)) {
                if (item != selected) {
                    selected = item;
                    selected.page.resetScroll();
                    selected.page.onShown();
                    pageFade.snap(0F);
                    pageFade.set(true);
                    search = "";
                }
                return;
            }
            y += ITEM;
        }

        if (selected != null) selected.page.mouseDown(mouseX, mouseY, button);
    }

    @Override public void mouseUp(int mouseX, int mouseY, int button) {
        if (selected != null) selected.page.mouseUp(mouseX, mouseY, button);
    }

    @Override public void mouseDragged(int mouseX, int mouseY, int button) {
        if (selected != null) selected.page.mouseDragged(mouseX, mouseY);
    }

    @Override public void mouseScroll(int amount) {
        if (selected == null) return;
        selected.page.scroll(amount, mc().input().mouseX(), mc().input().mouseY());
    }

    @Override public void keyDown(int keyCode, char typed) {
        if (selected != null && selected.page.capturingInput()) {
            selected.page.keyDown(keyCode, typed);
            return;
        }
        if (searchFocused) {
            if (keyCode == Keyboard.KEY_BACK) {
                if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            } else if (keyCode == Keyboard.KEY_RETURN) {
                searchFocused = false;
            } else if (typed >= 32 && typed != 127) {
                search += typed;
            }
            return;
        }
        if (selected != null) selected.page.keyDown(keyCode, typed);
    }

    @Override public boolean closeOnEscape() {
        if (selected != null && selected.page.capturingInput()) return false;
        return !searchFocused || search.isEmpty();
    }

    /** Switches to the named sidebar entry. Used by the headless preview. */
    public Page select(String label) {
        for (Item item : items) {
            if (item.page != null && item.label.equalsIgnoreCase(label)) {
                selected = item;
                selected.page.resetScroll();
                selected.page.onShown();
                pageFade.snap(1F);
                return selected.page;
            }
        }
        return null;
    }

    // ==================================================================
    private enum Icon { GEAR, PALETTE, FOLDER, KEYBOARD, KEY }

    private static final class Item {

        final String label;
        final Page page;
        final Category category;
        final Icon icon;
        final Animation hover = new Animation(10F);

        private Item(String label, Page page, Category category, Icon icon) {
            this.label = label;
            this.page = page;
            this.category = category;
            this.icon = icon;
        }

        static Item label(String text)  { return new Item(text, null, null, null); }
        static Item category(Category category, Page page) {
            return new Item(category.label(), page, category, null);
        }
        static Item page(String label, Icon icon, Page page) {
            return new Item(label, page, null, icon);
        }

        void drawIcon(IRenderer r, double x, double y, double size, int color) {
            if (category != null) { Glyphs.category(r, category, x, y, size, color); return; }
            if (icon == null) return;
            switch (icon) {
                case GEAR:     Glyphs.gear(r, x, y, size, color); break;
                case PALETTE:  Glyphs.palette(r, x, y, size, color); break;
                case FOLDER:   Glyphs.folder(r, x, y, size, color); break;
                case KEYBOARD: Glyphs.keyboard(r, x, y, size, color); break;
                case KEY:      Glyphs.key(r, x, y, size, color); break;
                default: break;
            }
        }
    }
}
