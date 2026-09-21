package xyz.stormclient.ui.menu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.Setting;
import xyz.stormclient.ui.Glyphs;
import xyz.stormclient.ui.UiScale;
import xyz.stormclient.util.Animation;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.MathUtil;

/**
 * The modules of one category, as a list of cards.
 *
 * <p>A card carries the module's name, what it does and its switch. Clicking it
 * unfolds that module's settings inside the same card, so a user never loses
 * sight of what they are configuring.
 */
public final class ModulesPage extends Page {

    private static final double CARD_GAP = 6;
    private static final double CARD_HEAD = 34;

    private final Category category;
    private final List<Card> cards = new ArrayList<Card>();

    public ModulesPage(Category category) {
        this.category = category;
        for (Module module : Storm.get().modules().byCategory(category)) {
            cards.add(new Card(module));
        }
    }

    @Override public String title() { return category.label() + " Modules"; }

    @Override public String subtitle() {
        int enabled = 0;
        for (Card card : cards) if (card.module.isEnabled()) enabled++;
        return cards.size() + " modules  -  " + enabled + " enabled";
    }

    @Override public boolean searchable() { return true; }

    @Override protected void renderBody(int mouseX, int mouseY, String search) {
        double y = bodyTop();
        for (Card card : cards) {
            if (!card.matches(search)) continue;
            card.position(x, y, width - 6);
            if (y + card.height() >= this.y && y <= this.y + height) card.render(mouseX, mouseY);
            y += card.height() + CARD_GAP;
        }
        setContentHeight(y - bodyTop());
    }

    @Override public void mouseDown(int mouseX, int mouseY, int button) {
        if (!inside(mouseX, mouseY)) return;
        for (Card card : cards) card.mouseDown(mouseX, mouseY, button);
    }

    @Override public void mouseUp(int mouseX, int mouseY, int button) {
        for (Card card : cards) card.mouseUp(mouseX, mouseY, button);
    }

    @Override public void mouseDragged(int mouseX, int mouseY) {
        for (Card card : cards) card.mouseDragged(mouseX, mouseY);
    }

    @Override public void keyDown(int key, char typed) {
        for (Card card : cards) card.keyDown(key, typed);
    }

    @Override public boolean capturingInput() {
        for (Card card : cards) if (card.capturingInput()) return true;
        return false;
    }

    // ==================================================================
    private final class Card {

        private final Module module;
        private final Map<Setting<?>, MenuControls.Row> rows =
                new LinkedHashMap<Setting<?>, MenuControls.Row>();
        private final Animation open = new Animation(9F);
        private final Animation toggleAnim = new Animation(11F);
        private final Animation hoverAnim = new Animation(10F);

        private boolean expanded;
        private double x, y, width;

        Card(Module module) {
            this.module = module;
            for (Setting<?> setting : module.settings()) {
                MenuControls.Row row = MenuControls.of(setting);
                if (row != null) rows.put(setting, row);
            }
            toggleAnim.snap(module.isEnabled() ? 1F : 0F);
        }

        boolean matches(String search) {
            if (search == null || search.isEmpty()) return true;
            return module.name().toLowerCase().contains(search.toLowerCase())
                    || module.description().toLowerCase().contains(search.toLowerCase());
        }

        void position(double x, double y, double width) {
            this.x = x;
            this.y = y;
            this.width = width;
        }

        double settingsHeight() {
            double h = 4;
            for (Map.Entry<Setting<?>, MenuControls.Row> entry : rows.entrySet()) {
                if (!entry.getKey().visible()) continue;
                h += entry.getValue().height();
            }
            return h + 4;
        }

        double height() {
            float t = open.eased();
            return CARD_HEAD + (t > 0.001F ? settingsHeight() * t : 0);
        }

        void render(int mouseX, int mouseY) {
            IRenderer r = r();
            IFontRenderer name = font(UiScale.HEADER_FONT);
            IFontRenderer small = font(UiScale.COMPONENT_FONT);

            double h = height();
            boolean hover = MathUtil.inside(mouseX, mouseY, x, y, width, CARD_HEAD);
            hoverAnim.set(hover);
            toggleAnim.set(module.isEnabled());
            float hoverT = hoverAnim.eased();
            float onT = toggleAnim.eased();

            r.roundedRect(x, y, width, h, 6F, ColorUtil.mix(
                    ColorUtil.withAlpha(theme().panelLight(), 225),
                    ColorUtil.withAlpha(theme().panelLight(), 255), hoverT));
            r.roundedRectOutline(x, y, width, h, 6F, 1F,
                    ColorUtil.mix(theme().outline(), ColorUtil.withAlpha(theme().accent(), 110), onT));

            double textX = x + MenuControls.PAD;
            name.draw(module.name(), textX, y + 8, theme().text());
            small.draw(small.trim(module.description(), (int) (width - MenuControls.PAD * 2 - 44)),
                    textX, y + 8 + name.height() + 1, theme().textFaint());

            // switch
            double sw = 26, sh = 14;
            double sx = x + width - MenuControls.PAD - sw;
            double sy = y + (CARD_HEAD - sh) / 2;
            r.roundedRect(sx, sy, sw, sh, (float) (sh / 2),
                    ColorUtil.mix(ColorUtil.withAlpha(theme().text(), 28), theme().accent(), onT));
            r.circle(sx + sh / 2 + (sw - sh) * onT, sy + sh / 2, sh / 2 - 1.8, 0xFFFFFFFF);

            float t = open.eased();
            if (t <= 0.001F) return;

            r.scissorBegin(x, y + CARD_HEAD, width, h - CARD_HEAD);
            r.rect(x + MenuControls.PAD, y + CARD_HEAD - 1, width - MenuControls.PAD * 2, 1,
                    ColorUtil.withAlpha(theme().text(), 18));
            double ry = y + CARD_HEAD + 4;
            for (Map.Entry<Setting<?>, MenuControls.Row> entry : rows.entrySet()) {
                if (!entry.getKey().visible()) continue;
                MenuControls.Row row = entry.getValue();
                row.position(x, ry, width);
                row.render(mouseX, mouseY);
                ry += row.height();
            }
            r.scissorEnd();
        }

        void mouseDown(int mouseX, int mouseY, int button) {
            if (MathUtil.inside(mouseX, mouseY, x, y, width, CARD_HEAD)) {
                double sw = 26, sh = 14;
                double sx = x + width - MenuControls.PAD - sw;
                double sy = y + (CARD_HEAD - sh) / 2;
                if (button == 0 && MathUtil.inside(mouseX, mouseY, sx - 3, sy - 3, sw + 6, sh + 6)) {
                    module.toggle();
                } else if (button == 0) {
                    expanded = !expanded;
                    open.set(expanded);
                } else if (button == 1) {
                    module.toggle();
                }
                return;
            }
            if (!expanded) return;
            for (Map.Entry<Setting<?>, MenuControls.Row> entry : rows.entrySet()) {
                if (entry.getKey().visible()) entry.getValue().mouseDown(mouseX, mouseY, button);
            }
        }

        void mouseUp(int mouseX, int mouseY, int button) {
            for (MenuControls.Row row : rows.values()) row.mouseUp(mouseX, mouseY, button);
        }

        void mouseDragged(int mouseX, int mouseY) {
            if (!expanded) return;
            for (MenuControls.Row row : rows.values()) row.mouseDragged(mouseX, mouseY);
        }

        void keyDown(int key, char typed) {
            if (!expanded) return;
            for (MenuControls.Row row : rows.values()) row.keyDown(key, typed);
        }

        boolean capturingInput() {
            for (MenuControls.Row row : rows.values()) if (row.capturingInput()) return true;
            return false;
        }
    }

    /** Unfolds one module's settings, used by the headless preview. */
    public void expand(String moduleName) {
        for (Card card : cards) {
            if (card.module.name().equalsIgnoreCase(moduleName)) {
                card.expanded = true;
                card.open.snap(1F);
                return;
            }
        }
    }

    /** Drawn by the sidebar next to the category name. */
    public static int count(Category category) {
        return Storm.get().modules().byCategory(category).size();
    }

    /** Drawn by the sidebar when the category has something running. */
    public static int enabledCount(Category category) {
        int enabled = 0;
        for (Module module : Storm.get().modules().byCategory(category)) {
            if (module.isEnabled()) enabled++;
        }
        return enabled;
    }

    /** The icon the sidebar shows for this page. */
    public void drawIcon(IRenderer r, double x, double y, double size, int color) {
        Glyphs.category(r, category, x, y, size, color);
    }
}
