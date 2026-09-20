package xyz.stormclient.launcher.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** Small painting helpers so every panel looks like it belongs to the same app. */
public final class UiKit {

    private UiKit() { }

    public static Graphics2D prepare(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }

    public static void fillRound(Graphics2D g, double x, double y, double w, double h, double radius, Color color) {
        g.setColor(color);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, radius, radius));
    }

    public static void drawRound(Graphics2D g, double x, double y, double w, double h,
                                 double radius, float stroke, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(stroke));
        g.draw(new RoundRectangle2D.Double(x, y, w - stroke, h - stroke, radius, radius));
    }

    public static void gradient(Graphics2D g, double x, double y, double w, double h,
                                double radius, Color top, Color bottom) {
        g.setPaint(new GradientPaint((float) x, (float) y, top, (float) x, (float) (y + h), bottom));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, radius, radius));
        g.setPaint(null);
    }

    /** Cheap soft shadow: a few translucent rounded rectangles stacked outwards. */
    public static void shadow(Graphics2D g, double x, double y, double w, double h, double radius, int strength) {
        for (int i = strength; i > 0; i--) {
            g.setColor(new Color(0, 0, 0, Math.max(3, 26 / i)));
            g.fill(new RoundRectangle2D.Double(x - i, y - i + 1, w + i * 2, h + i * 2, radius + i, radius + i));
        }
    }

    /** The Storm bolt, the launcher's logo. */
    public static void bolt(Graphics2D g, double x, double y, double size, Color color) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x + size * 0.58, y);
        path.lineTo(x + size * 0.12, y + size * 0.56);
        path.lineTo(x + size * 0.44, y + size * 0.56);
        path.lineTo(x + size * 0.34, y + size);
        path.lineTo(x + size * 0.88, y + size * 0.40);
        path.lineTo(x + size * 0.54, y + size * 0.40);
        path.closePath();

        g.setPaint(new GradientPaint((float) x, (float) y, color,
                (float) (x + size), (float) (y + size), StormTheme.mix(color, Color.WHITE, 0.35F)));
        g.fill(path);
        g.setPaint(null);
    }

    public static void statusDot(Graphics2D g, double x, double y, double size, Color color) {
        g.setColor(StormTheme.alpha(color, 60));
        g.fillOval((int) (x - size * 0.6), (int) (y - size * 0.6), (int) (size * 2.2), (int) (size * 2.2));
        g.setColor(color);
        g.fillOval((int) x, (int) y, (int) size, (int) size);
    }

    /** Draws text and returns the width it took. */
    public static int text(Graphics2D g, String value, double x, double y, Color color) {
        g.setColor(color);
        g.drawString(value, (float) x, (float) y);
        return g.getFontMetrics().stringWidth(value);
    }

    public static void textRight(Graphics2D g, String value, double right, double y, Color color) {
        int width = g.getFontMetrics().stringWidth(value);
        text(g, value, right - width, y, color);
    }

    public static void textCenter(Graphics2D g, String value, double centerX, double y, Color color) {
        int width = g.getFontMetrics().stringWidth(value);
        text(g, value, centerX - width / 2.0, y, color);
    }
}
