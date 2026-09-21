package xyz.stormclient.ui.menu;

import java.util.ArrayList;
import java.util.List;

import xyz.stormclient.Storm;
import xyz.stormclient.StormInfo;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.Setting;
import xyz.stormclient.ui.UiScale;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.MathUtil;

/** The pages that are not a module category. */
public final class MenuPages {

    private MenuPages() { }

    // ==================================================================
    /** A plain list of setting rows, used for the theme and client options. */
    public static class SettingsPage extends Page {

        private final String title;
        private final String subtitle;
        private final List<Setting<?>> settings = new ArrayList<Setting<?>>();
        private final List<MenuControls.Row> rows = new ArrayList<MenuControls.Row>();

        public SettingsPage(String title, String subtitle, Iterable<Setting<?>> settings) {
            this.title = title;
            this.subtitle = subtitle;
            for (Setting<?> setting : settings) {
                MenuControls.Row row = MenuControls.of(setting);
                if (row == null) continue;
                this.settings.add(setting);
                rows.add(row);
            }
        }

        @Override public String title() { return title; }
        @Override public String subtitle() { return subtitle; }

        @Override protected void renderBody(int mouseX, int mouseY, String search) {
            IRenderer r = r();
            double y = bodyTop();
            double total = 8;
            for (MenuControls.Row row : rows) total += row.height();

            r.roundedRect(x, y, width - 6, total, 6F, ColorUtil.withAlpha(theme().panelLight(), 235));
            r.roundedRectOutline(x, y, width - 6, total, 6F, 1F, theme().outline());

            double ry = y + 4;
            for (int i = 0; i < rows.size(); i++) {
                if (!settings.get(i).visible()) continue;
                MenuControls.Row row = rows.get(i);
                row.position(x, ry, width - 6);
                row.render(mouseX, mouseY);
                ry += row.height();
            }
            setContentHeight(total + 8);
        }

        @Override public void mouseDown(int mouseX, int mouseY, int button) {
            if (!inside(mouseX, mouseY)) return;
            for (MenuControls.Row row : rows) row.mouseDown(mouseX, mouseY, button);
        }
        @Override public void mouseUp(int mouseX, int mouseY, int button) {
            for (MenuControls.Row row : rows) row.mouseUp(mouseX, mouseY, button);
        }
        @Override public void mouseDragged(int mouseX, int mouseY) {
            for (MenuControls.Row row : rows) row.mouseDragged(mouseX, mouseY);
        }
        @Override public void keyDown(int key, char typed) {
            for (MenuControls.Row row : rows) row.keyDown(key, typed);
        }
        @Override public boolean capturingInput() {
            for (MenuControls.Row row : rows) if (row.capturingInput()) return true;
            return false;
        }
    }

    // ==================================================================
    /** Every module's key, in one list. */
    public static final class KeybindsPage extends Page {

        private final List<Module> modules = new ArrayList<Module>();
        private final List<MenuControls.Row> rows = new ArrayList<MenuControls.Row>();

        public KeybindsPage() {
            for (Module module : Storm.get().modules().all()) {
                if (module.hidden()) continue;
                modules.add(module);
                // the row's own "Keybind" label would land on the module name
                rows.add(MenuControls.of(module.keybindSetting()).withoutLabel());
            }
        }

        @Override public String title() { return "Keybinds"; }
        @Override public String subtitle() { return modules.size() + " modules"; }
        @Override public boolean searchable() { return true; }

        @Override protected void renderBody(int mouseX, int mouseY, String search) {
            IFontRenderer font = font(UiScale.ROW_FONT);
            IFontRenderer small = font(UiScale.COMPONENT_FONT);
            double y = bodyTop();

            for (int i = 0; i < modules.size(); i++) {
                Module module = modules.get(i);
                if (!matches(module, search)) continue;
                MenuControls.Row row = rows.get(i);

                double h = 24;
                r().roundedRect(x, y, width - 6, h, 5F, ColorUtil.withAlpha(theme().panelLight(), 215));
                font.draw(module.name(), x + MenuControls.PAD, y + 4, theme().text());
                small.draw(module.category().label(), x + MenuControls.PAD,
                        y + 4 + font.height(), theme().textFaint());

                row.position(x, y + (h - 22) / 2, width - 6);
                row.render(mouseX, mouseY);
                y += h + 4;
            }
            setContentHeight(y - bodyTop());
        }

        private boolean matches(Module module, String search) {
            if (search == null || search.isEmpty()) return true;
            return module.name().toLowerCase().contains(search.toLowerCase());
        }

        @Override public void mouseDown(int mouseX, int mouseY, int button) {
            if (!inside(mouseX, mouseY)) return;
            for (MenuControls.Row row : rows) row.mouseDown(mouseX, mouseY, button);
        }
        @Override public void keyDown(int key, char typed) {
            for (MenuControls.Row row : rows) row.keyDown(key, typed);
        }
        @Override public boolean capturingInput() {
            for (MenuControls.Row row : rows) if (row.capturingInput()) return true;
            return false;
        }
    }

    // ==================================================================
    /** Load, save and create config profiles. */
    public static final class ConfigsPage extends Page {

        private static final double ROW = 28;

        private String pending = "";
        private boolean naming;
        private String status = "";

        @Override public String title() { return "Configs"; }
        @Override public String subtitle() {
            return "current: " + Storm.get().config().currentName();
        }

        @Override protected void renderBody(int mouseX, int mouseY, String search) {
            IFontRenderer font = font(UiScale.ROW_FONT);
            IFontRenderer small = font(UiScale.COMPONENT_FONT);
            double y = bodyTop();

            // new profile field
            r().roundedRect(x, y, width - 6, ROW, 5F, ColorUtil.withAlpha(theme().panelLight(), 225));
            r().roundedRectOutline(x, y, width - 6, ROW, 5F, 1F,
                    naming ? theme().accent() : theme().outline());
            String shown = pending.isEmpty() && !naming ? "new profile name" : pending + (naming ? "|" : "");
            small.draw(shown, x + MenuControls.PAD, y + (ROW - small.height()) / 2,
                    pending.isEmpty() && !naming ? theme().textFaint() : theme().text());
            button(mouseX, mouseY, x + width - 6 - MenuControls.PAD - 44, y + (ROW - 16) / 2, 44, "Create");
            y += ROW + 8;

            if (!status.isEmpty()) {
                small.draw(status, x + 2, y - 5, theme().accent());
            }

            for (String profile : Storm.get().config().profiles()) {
                boolean active = profile.equals(Storm.get().config().currentName());
                r().roundedRect(x, y, width - 6, ROW, 5F,
                        ColorUtil.withAlpha(theme().panelLight(), active ? 255 : 215));
                if (active) r().roundedRect(x, y + 6, 2, ROW - 12, 1F, theme().accent());
                font.draw(profile, x + MenuControls.PAD, y + (ROW - font.height()) / 2,
                        active ? theme().text() : theme().textDim());

                double bx = x + width - 6 - MenuControls.PAD;
                button(mouseX, mouseY, bx - 40, y + (ROW - 16) / 2, 40, "Delete");
                button(mouseX, mouseY, bx - 40 - 4 - 36, y + (ROW - 16) / 2, 36, "Save");
                button(mouseX, mouseY, bx - 40 - 4 - 36 - 4 - 36, y + (ROW - 16) / 2, 36, "Load");
                y += ROW + 4;
            }
            setContentHeight(y - bodyTop());
        }

        private void button(int mouseX, int mouseY, double bx, double by, double bw, String label) {
            IFontRenderer font = font(UiScale.COMPONENT_FONT);
            boolean hover = MathUtil.inside(mouseX, mouseY, bx, by, bw, 16);
            r().roundedRect(bx, by, bw, 16, 4F, hover
                    ? ColorUtil.withAlpha(theme().accent(), 60)
                    : ColorUtil.withAlpha(0xFF000000, 80));
            r().roundedRectOutline(bx, by, bw, 16, 4F, 1F, hover ? theme().accent() : theme().outline());
            font.draw(label, bx + (bw - font.width(label)) / 2, by + (16 - font.height()) / 2.0,
                    hover ? theme().accent() : theme().textDim());
        }

        @Override public void mouseDown(int mouseX, int mouseY, int button) {
            if (!inside(mouseX, mouseY) || button != 0) return;
            double y = bodyTop();

            naming = MathUtil.inside(mouseX, mouseY, x, y, width - 6 - 60, ROW);
            double cx = x + width - 6 - MenuControls.PAD - 44;
            if (MathUtil.inside(mouseX, mouseY, cx, y + (ROW - 16) / 2, 44, 16)) {
                if (!pending.trim().isEmpty()) {
                    status = Storm.get().config().create(pending.trim())
                            ? "created " + pending.trim() : "could not create " + pending.trim();
                    pending = "";
                }
                return;
            }
            y += ROW + 8;

            for (String profile : new ArrayList<String>(Storm.get().config().profiles())) {
                double bx = x + width - 6 - MenuControls.PAD;
                double by = y + (ROW - 16) / 2;
                if (MathUtil.inside(mouseX, mouseY, bx - 40, by, 40, 16)) {
                    status = Storm.get().config().delete(profile) ? "deleted " + profile : "could not delete";
                    return;
                }
                if (MathUtil.inside(mouseX, mouseY, bx - 80, by, 36, 16)) {
                    status = Storm.get().config().save(profile) ? "saved " + profile : "could not save";
                    return;
                }
                if (MathUtil.inside(mouseX, mouseY, bx - 120, by, 36, 16)) {
                    status = Storm.get().config().load(profile) ? "loaded " + profile : "could not load";
                    return;
                }
                y += ROW + 4;
            }
        }

        @Override public void keyDown(int key, char typed) {
            if (!naming) return;
            if (key == xyz.stormclient.util.Keyboard.KEY_BACK) {
                if (!pending.isEmpty()) pending = pending.substring(0, pending.length() - 1);
            } else if (key == xyz.stormclient.util.Keyboard.KEY_RETURN) {
                naming = false;
            } else if (typed >= 32 && typed != 127) {
                pending += typed;
            }
        }

        @Override public boolean capturingInput() { return naming; }
    }

    // ==================================================================
    /** What Storm knows about the licence it was started with. */
    public static final class LicencePage extends Page {

        @Override public String title() { return "Licence"; }
        @Override public String subtitle() { return StormInfo.FULL_NAME; }

        @Override protected void renderBody(int mouseX, int mouseY, String search) {
            IFontRenderer font = font(UiScale.ROW_FONT);
            IFontRenderer small = font(UiScale.COMPONENT_FONT);
            xyz.stormclient.licence.Licence licence = Storm.get().licence();
            boolean enforced = xyz.stormclient.licence.LicenceVerifier.enforced();
            boolean ok = licence.valid();
            int tint = !enforced ? theme().textDim() : ok ? theme().accent() : 0xFFE05A5A;

            double y = bodyTop();
            double cardHeight = enforced ? 78 : 44;
            r().roundedRect(x, y, width - 6, cardHeight, 6F,
                    ColorUtil.withAlpha(theme().panelLight(), 235));
            r().roundedRectOutline(x, y, width - 6, cardHeight, 6F, 1F,
                    ColorUtil.withAlpha(tint, 120));

            xyz.stormclient.ui.Glyphs.key(r(), x + MenuControls.PAD, y + 12, 14, tint);

            double tx = x + MenuControls.PAD + 22;
            String heading = !enforced ? "No key required" : ok ? "Licensed" : "Not licensed";
            font.draw(heading, tx, y + 12, theme().text());
            small.draw(licence.statusLine(), tx, y + 12 + font.height() + 2, theme().textFaint());

            if (enforced) {
                double ry = y + 40;
                ry = field(small, tx, ry, "Holder", licence.holder());
                ry = field(small, tx, ry, "Plan", licence.plan());
                field(small, tx, ry, "Expires", licence.expiryText());
            }

            y += cardHeight + 10;
            for (String line : enforced
                    ? new String[] {
                        "The key is entered in the Storm launcher, on its Licence page.",
                        "It is checked against the signing key this build was compiled with." }
                    : new String[] {
                        "This build was compiled without a signing key, so every copy runs.",
                        "Generate one with tools/licence/LicenceTool.java to hand out keys." }) {
                small.draw(line, x + 2, y, theme().textFaint());
                y += small.height() + 2;
            }
            setContentHeight(y - bodyTop() + 12);
        }

        private double field(IFontRenderer font, double fx, double fy, String label, String value) {
            font.draw(label, fx, fy, theme().textFaint());
            font.draw(value, fx + 52, fy, theme().textDim());
            return fy + font.height() + 1;
        }
    }
}
