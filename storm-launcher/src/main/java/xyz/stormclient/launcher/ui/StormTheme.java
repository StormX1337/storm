package xyz.stormclient.launcher.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;

/** Launcher palette and fonts. Same accent as the in game client. */
public final class StormTheme {

    public static final Color BACKGROUND = new Color(0x0E1116);
    public static final Color SIDEBAR    = new Color(0x11151C);
    public static final Color PANEL      = new Color(0x151A21);
    public static final Color PANEL_HI   = new Color(0x1C232C);
    public static final Color OUTLINE    = new Color(0x263040);
    public static final Color ACCENT     = new Color(0x35C4FF);
    public static final Color ACCENT_DIM = new Color(0x1E7FA8);
    public static final Color TEXT       = new Color(0xE9EDF4);
    public static final Color TEXT_DIM   = new Color(0x8A97A8);
    public static final Color TEXT_FAINT = new Color(0x5A6676);
    public static final Color GREEN      = new Color(0x45E08A);
    public static final Color AMBER      = new Color(0xFFB648);
    public static final Color RED        = new Color(0xFF4E6E);

    private static final List<String> PREFERRED = Arrays.asList(
            "Inter", "Segoe UI Variable", "Segoe UI", "SF Pro Display", "Roboto", "Ubuntu", "DejaVu Sans");

    private static final String FAMILY = pickFamily();

    private StormTheme() { }

    private static String pickFamily() {
        String forced = System.getProperty("storm.launcher.font", "");
        if (!forced.isEmpty()) return forced;

        List<String> available = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String name : PREFERRED) {
            if (available.contains(name)) return name;
        }
        return Font.SANS_SERIF;
    }

    public static Font font(int size)      { return new Font(FAMILY, Font.PLAIN, size); }
    public static Font bold(int size)      { return new Font(FAMILY, Font.BOLD, size); }

    public static Color alpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Color mix(Color a, Color b, float t) {
        t = Math.max(0F, Math.min(1F, t));
        return new Color(
                (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    public static Color fromRgb(int rgb) { return new Color(rgb, true); }
}
