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
import xyz.stormclient.util.Stagger;

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
    private final Stagger reveal = new Stagger(0.28F, 0.035F, 14);

    public ModulesPage(Category category) {
        this.category = category;
        for (Module module : Storm.get().modules().byCategory(category)) {
            cards.add(new Card(module));
        }
    }

    @Override public void onShown() { reveal.restart(); }

    @Override public String title() { return category.label() + " Modules"; }

    @Override public String subtitle() {
        int enabled = 0;
        for (Card card : cards) if (card.module.isEnabled()) enabled++;
        return cards.size() + " modules  -  " + enabled + " enabled";
    }

    @Override public boolean searchable() { return true; }

    @Override protected void renderBody(int mouseX, int mouseY, String search) {
        double y = bodyTop();
        int shown = 0;
        for (Card card : cards) {
            if (!card.matches(search)) continue;
            card.position(x, y, width - 6);
            if (y + card.height() >= this.y && y <= this.y + height) {
                // each card slides up into place a moment after the one above it
                card.render(mouseX, mouseY, reveal.progress(shown));
            }
            y += card.height() + CARD_GAP;
            shown++;
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

        /** {x, y, w, h} of the switch. Drawing and hit testing both read this. */
        double[] switchBounds() {
            double sw = 26, sh = 14;
            return new double[] { x + width - MenuControls.PAD - sw,
                                  y + (CARD_HEAD - sh) / 2, sw, sh };
        }

        void render(int mouseX, int mouseY) { render(mouseX, mouseY, 1F); }

        void render(int mouseX, int mouseY, float entrance) {
            if (entrance <= 0.001F) return;
            IRenderer r = r();
            IFontRenderer name = font(UiScale.HEADER_FONT);
            IFontRenderer small = font(UiScale.COMPONENT_FONT);

            double h = height();
            boolean hover = MathUtil.inside(mouseX, mouseY, x, y, width, CARD_HEAD);
            hoverAnim.set(hover);
            toggleAnim.set(module.isEnabled());
            float hoverT = hoverAnim.eased();
            float onT = toggleAnim.eased();

            // the entrance moves the card and fades it, the hover lifts it
            double slide = (1 - entrance) * 14;
            double lift = hoverT * 1.5;
            r.push();
            r.translate(slide, -lift, 0);

            if (hoverT > 0.02F && theme().shadows()) {
                r.shadow(x, y, width, h, 6F,
                        ColorUtil.withAlpha(0xFF000000, (int) (70 * hoverT * entrance)));
            }
            r.roundedRect(x, y, width, h, 6F, ColorUtil.withAlpha(ColorUtil.mix(
                    theme().panelLight(),
                    ColorUtil.brighter(theme().panelLight(), 1.3F), hoverT), (int) (238 * entrance)));
            r.roundedRectOutline(x, y, width, h, 6F, 1F, ColorUtil.withAlpha(
                    ColorUtil.mix(theme().outline(),
                            ColorUtil.withAlpha(theme().accent(), 150), Math.max(onT, hoverT * 0.6F)),
                    (int) (255 * entrance)));

            // a module that is on carries the accent down its left edge
            if (onT > 0.01F) {
                r.roundedRect(x, y + 6 + (1 - onT) * (CARD_HEAD / 2 - 6), 2,
                        (CARD_HEAD - 12) * onT + 1, 1F,
                        ColorUtil.fade(theme().accent(), onT * entrance));
            }

            double textX = x + MenuControls.PAD + (onT > 0.01F ? 3 * onT : 0);
            name.draw(module.name(), textX, y + 8,
                    ColorUtil.fade(ColorUtil.mix(theme().text(), 0xFFFFFFFF, onT), entrance));
            small.draw(small.trim(module.description(), (int) (width - MenuControls.PAD * 2 - 44)),
                    textX, y + 8 + name.height() + 1, ColorUtil.fade(theme().textFaint(), entrance));

            // switch
            double[] box = switchBounds();
            double sx = box[0], sy = box[1], sw = box[2], sh = box[3];
            r.roundedRect(sx, sy, sw, sh, (float) (sh / 2), ColorUtil.fade(
                    ColorUtil.mix(ColorUtil.withAlpha(theme().text(), 40), theme().accent(), onT),
                    entrance));
            // the knob grows a touch as it crosses, which sells the throw
            double knob = sh / 2 - 2.0 + Math.sin(onT * Math.PI) * 0.6;
            r.circle(sx + sh / 2 + (sw - sh) * onT, sy + sh / 2, knob,
                    ColorUtil.fade(0xFFFFFFFF, entrance));

            float t = open.eased();
            if (t <= 0.001F) { r.pop(); return; }

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
            r.pop();
        }

        void mouseDown(int mouseX, int mouseY, int button) {
            if (MathUtil.inside(mouseX, mouseY, x, y, width, CARD_HEAD)) {
                double[] box = switchBounds();
                if (button == 0 && MathUtil.inside(mouseX, mouseY,
                        box[0] - 4, box[1] - 4, box[2] + 8, box[3] + 8)) {
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

    /**
     * Where a module's switch ended up on the last frame: {x, y, w, h}, or null
     * when that module is not on this page. The click test uses it to press
     * exactly what the renderer drew.
     */
    public double[] switchBounds(String moduleName) {
        Card card = find(moduleName);
        return card == null ? null : card.switchBounds();
    }

    /** Where a module's card header ended up on the last frame. */
    public double[] headerBounds(String moduleName) {
        Card card = find(moduleName);
        return card == null ? null : new double[] { card.x, card.y, card.width, CARD_HEAD };
    }

    /** Whether that module's settings are unfolded. */
    public boolean expanded(String moduleName) {
        Card card = find(moduleName);
        return card != null && card.expanded;
    }

    private Card find(String moduleName) {
        for (Card card : cards) {
            if (card.module.name().equalsIgnoreCase(moduleName)) return card;
        }
        return null;
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
