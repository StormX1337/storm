package xyz.stormclient.ui;

import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.module.Category;

/**
 * Every symbol Storm draws in its UI, built from rectangles and circles.
 *
 * <p>The font atlas only bakes the first 256 characters, so arrows and category
 * icons taken from the Unicode symbol blocks used to come out as question
 * marks. Drawing them keeps the UI readable whatever font the user picks, and
 * the shapes stay sharp at any GUI scale.
 */
public final class Glyphs {

    private Glyphs() { }

    /** A chevron pointing down, or up when {@code up} is set. Fits in size x size/2. */
    public static void chevron(IRenderer r, double x, double y, double size, boolean up, int color) {
        double step = size / 6.0;
        for (int i = 0; i < 3; i++) {
            double h = step;
            double offset = up ? (2 - i) * step : i * step;
            r.rect(x + i * step,            y + offset, step, h, color);
            r.rect(x + size - (i + 1) * step, y + offset, step, h, color);
        }
    }

    /** A chevron pointing right, or left when {@code left} is set. */
    public static void chevronSide(IRenderer r, double x, double y, double size, boolean left, int color) {
        double step = size / 6.0;
        for (int i = 0; i < 3; i++) {
            double offset = left ? (2 - i) * step : i * step;
            r.rect(x + offset, y + i * step,                step, step, color);
            r.rect(x + offset, y + size - (i + 1) * step,   step, step, color);
        }
    }

    /** The plus / minus a collapsed or open panel shows. */
    public static void collapse(IRenderer r, double x, double y, double size, boolean open, int color) {
        double bar = Math.max(1, size / 7.0);
        r.rect(x, y + (size - bar) / 2, size, bar, color);
        if (!open) r.rect(x + (size - bar) / 2, y, bar, size, color);
    }

    /** A magnifier for the search field. */
    public static void search(IRenderer r, double x, double y, double size, int color) {
        double ring = size * 0.62;
        r.arc(x + ring / 2, y + ring / 2, ring / 2, 0F, 360F, 1F, color);
        double t = Math.max(1, size / 8.0);
        for (int i = 0; i < 3; i++) {
            r.rect(x + ring * 0.78 + i * t * 0.7, y + ring * 0.78 + i * t * 0.7, t, t, color);
        }
    }

    /** The Storm bolt, so the client needs no logo texture. */
    public static void bolt(IRenderer r, double x, double y, double scale, int color) {
        double[][] rows = {
                { 6, 0, 4, 2 }, { 4, 2, 5, 2 }, { 3, 4, 5, 2 }, { 2, 6, 7, 2 },
                { 4, 8, 4, 2 }, { 3, 10, 4, 2 }, { 2, 12, 3, 2 }
        };
        for (double[] row : rows) {
            r.rect(x + row[0] * scale, y + row[1] * scale, row[2] * scale, row[3] * scale, color);
        }
    }

    /**
     * The icon in a category header. Each one is a small pictogram rather than
     * a letter, so the headers read at a glance.
     */
    public static void category(IRenderer r, Category category, double x, double y, double size, int color) {
        double u = size / 8.0;          // one grid unit of an 8x8 pictogram
        switch (category) {
            case COMBAT:                // a sword on its diagonal
                for (int i = 0; i < 5; i++) r.rect(x + (3 + i) * u, y + (4 - i) * u, u, u, color);
                r.rect(x + u, y + 5 * u, 3 * u, u, color);
                r.rect(x + 2 * u, y + 4 * u, u, 3 * u, color);
                break;
            case MOVEMENT:              // two arrows pointing right
                chevronSide(r, x + u, y + u, size * 0.72, false, color);
                chevronSide(r, x + 3.4 * u, y + u, size * 0.72, false, color);
                break;
            case PLAYER:                // head and shoulders
                r.circle(x + 4 * u, y + 2.6 * u, 1.7 * u, color);
                r.rect(x + 1.6 * u, y + 5.4 * u, 4.8 * u, 2.2 * u, color);
                break;
            case RENDER:                // an eye
                r.arc(x + 4 * u, y + 4 * u, 3.0 * u, 0F, 360F, 1F, color);
                r.circle(x + 4 * u, y + 4 * u, 1.3 * u, color);
                break;
            case WORLD:                 // a stack of blocks
                r.rect(x + u, y + 4 * u, 2.6 * u, 2.6 * u, color);
                r.rect(x + 4.4 * u, y + 4 * u, 2.6 * u, 2.6 * u, color);
                r.rect(x + 2.7 * u, y + 1.4 * u, 2.6 * u, 2.6 * u, color);
                break;
            case HUD:                   // a screen with two bars
                r.rectOutline(x + u, y + 1.6 * u, 6 * u, 4.8 * u, (float) Math.max(1, u * 0.8), color);
                r.rect(x + 2 * u, y + 3 * u, 3.4 * u, 0.9 * u, color);
                r.rect(x + 2 * u, y + 4.6 * u, 2.2 * u, 0.9 * u, color);
                break;
            case MISC:                  // a gear
            default:
                r.arc(x + 4 * u, y + 4 * u, 2.4 * u, 0F, 360F, 1.6F, color);
                for (int i = 0; i < 4; i++) {
                    double a = Math.PI / 4 + i * Math.PI / 2;
                    r.rect(x + 4 * u + Math.cos(a) * 2.9 * u - 0.7 * u,
                           y + 4 * u + Math.sin(a) * 2.9 * u - 0.7 * u, 1.4 * u, 1.4 * u, color);
                }
                break;
        }
    }
}
