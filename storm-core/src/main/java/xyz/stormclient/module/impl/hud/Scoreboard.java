package xyz.stormclient.module.impl.hud;

import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.util.ColorUtil;

/** A cleaner scoreboard. The bridge pushes the rows in, the core just draws them. */
public class Scoreboard extends HudModule {

    private final BooleanSetting numbers    = add(new BooleanSetting("Red numbers", false));
    private final BooleanSetting background = add(new BooleanSetting("Background", true));

    private static String title = "";
    private static String[] lines = new String[0];

    public Scoreboard() {
        super("Scoreboard", "Cleaner sidebar scoreboard", 0.98, 0.35);
    }

    public static void setContent(String newTitle, String[] newLines) {
        title = newTitle == null ? "" : newTitle;
        lines = newLines == null ? new String[0] : newLines;
    }

    @Override public double width() {
        return 110;
    }

    @Override public double height() { return 14 + lines.length * 10; }

    @Override public void renderElement(IRenderer r, IFontRenderer font) {
        if (lines.length == 0) return;
        double w = width(), h = height();
        double x = -w;        // anchored to the right edge

        if (background.get()) {
            r.roundedRect(x, 0, w, h, theme().radius(), ColorUtil.withAlpha(theme().panel(), 190));
            r.rect(x, 12, w, 1, ColorUtil.withAlpha(theme().accent(), 120));
        }
        font.drawCentered(title, x + w / 2, 2, theme().text());

        double y = 15;
        for (String line : lines) {
            font.draw(font.trim(line, (int) w - 8), x + 4, y, theme().textDim());
            y += 10;
        }
    }

    public boolean showNumbers() { return numbers.get(); }
}
