package xyz.stormclient.module.impl.hud;

import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.util.ColorUtil;

/**
 * Active potion effects. The bridge fills {@link #setEffects} every tick, so
 * the core never has to know a single potion id.
 */
public class PotionHud extends HudModule {

    private final BooleanSetting background = add(new BooleanSetting("Background", true));
    private final BooleanSetting seconds    = add(new BooleanSetting("Seconds left", true));

    private static String[] names = new String[0];
    private static int[] durations = new int[0];
    private static int[] colors = new int[0];

    public PotionHud() {
        super("PotionHUD", "Lists your active potion effects", 0.86, 0.06);
    }

    /** Called by the bridge each tick. */
    public static void setEffects(String[] effectNames, int[] ticksLeft, int[] effectColors) {
        names = effectNames;
        durations = ticksLeft;
        colors = effectColors;
    }

    @Override public double width()  { return 108; }
    @Override public double height() { return Math.max(10, names.length * 12); }

    @Override public void renderElement(IRenderer r, IFontRenderer font) {
        if (names.length == 0) return;
        if (background.get()) {
            r.roundedRect(0, 0, width(), height() + 4, theme().radius(), ColorUtil.withAlpha(theme().panel(), 200));
        }
        double y = 2;
        for (int i = 0; i < names.length; i++) {
            int color = i < colors.length ? colors[i] : theme().accent();
            r.roundedRect(3, y + 2, 2, 8, 1F, ColorUtil.withAlpha(color, 255));
            font.draw(names[i], 9, y, theme().text());

            if (seconds.get() && i < durations.length) {
                String time = format(durations[i]);
                font.draw(time, width() - font.width(time) - 5, y, theme().textDim());
            }
            y += 12;
        }
    }

    private String format(int ticks) {
        int total = ticks / 20;
        return String.format("%d:%02d", total / 60, total % 60);
    }
}
