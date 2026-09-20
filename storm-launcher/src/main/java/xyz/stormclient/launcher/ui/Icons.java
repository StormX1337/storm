package xyz.stormclient.launcher.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Icons drawn as geometry.
 *
 * <p>Unicode symbols looked fine locally and rendered as empty boxes on a
 * machine whose UI font had no glyph for them, so nothing here depends on a
 * font being installed.
 */
public final class Icons {

    public enum Kind { PLAY, INJECT, SETTINGS, CONSOLE, INFO, FOLDER, REFRESH, CHECK }

    private Icons() { }

    public static void draw(Graphics2D graphics, Kind kind, double x, double y, double size, Color color) {
        Graphics2D g = (Graphics2D) graphics.create();
        UiKit.prepare(g);
        g.translate(x, y);
        g.setColor(color);
        g.setStroke(new BasicStroke((float) Math.max(1.3, size * 0.09),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (kind) {
            case PLAY:     play(g, size); break;
            case INJECT:   inject(g, size); break;
            case SETTINGS: settings(g, size); break;
            case CONSOLE:  console(g, size); break;
            case INFO:     info(g, size); break;
            case FOLDER:   folder(g, size); break;
            case REFRESH:  refresh(g, size); break;
            case CHECK:    check(g, size); break;
        }
        g.dispose();
    }

    private static void play(Graphics2D g, double s) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(s * 0.24, s * 0.14);
        path.lineTo(s * 0.84, s * 0.50);
        path.lineTo(s * 0.24, s * 0.86);
        path.closePath();
        g.fill(path);
    }

    /** An arrow pointing down into a tray: the agent going into a running game. */
    private static void inject(Graphics2D g, double s) {
        g.draw(new java.awt.geom.Line2D.Double(s * 0.5, s * 0.10, s * 0.5, s * 0.58));
        Path2D.Double head = new Path2D.Double();
        head.moveTo(s * 0.28, s * 0.40);
        head.lineTo(s * 0.5, s * 0.63);
        head.lineTo(s * 0.72, s * 0.40);
        g.draw(head);

        Path2D.Double tray = new Path2D.Double();
        tray.moveTo(s * 0.16, s * 0.72);
        tray.lineTo(s * 0.16, s * 0.88);
        tray.lineTo(s * 0.84, s * 0.88);
        tray.lineTo(s * 0.84, s * 0.72);
        g.draw(tray);
    }

    private static void settings(Graphics2D g, double s) {
        double cx = s * 0.5, cy = s * 0.5;
        double outer = s * 0.40, inner = s * 0.27;

        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            g.draw(new java.awt.geom.Line2D.Double(
                    cx + Math.cos(angle) * inner, cy + Math.sin(angle) * inner,
                    cx + Math.cos(angle) * outer, cy + Math.sin(angle) * outer));
        }
        g.draw(new Ellipse2D.Double(cx - inner * 0.72, cy - inner * 0.72, inner * 1.44, inner * 1.44));
    }

    private static void console(Graphics2D g, double s) {
        g.draw(new RoundRectangle2D.Double(s * 0.10, s * 0.18, s * 0.80, s * 0.64, s * 0.16, s * 0.16));
        Path2D.Double prompt = new Path2D.Double();
        prompt.moveTo(s * 0.28, s * 0.38);
        prompt.lineTo(s * 0.44, s * 0.50);
        prompt.lineTo(s * 0.28, s * 0.62);
        g.draw(prompt);
        g.draw(new java.awt.geom.Line2D.Double(s * 0.54, s * 0.63, s * 0.72, s * 0.63));
    }

    private static void info(Graphics2D g, double s) {
        g.draw(new Ellipse2D.Double(s * 0.12, s * 0.12, s * 0.76, s * 0.76));
        g.fill(new Ellipse2D.Double(s * 0.44, s * 0.28, s * 0.12, s * 0.12));
        g.draw(new java.awt.geom.Line2D.Double(s * 0.5, s * 0.46, s * 0.5, s * 0.72));
    }

    private static void folder(Graphics2D g, double s) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(s * 0.12, s * 0.78);
        path.lineTo(s * 0.12, s * 0.24);
        path.lineTo(s * 0.42, s * 0.24);
        path.lineTo(s * 0.52, s * 0.36);
        path.lineTo(s * 0.88, s * 0.36);
        path.lineTo(s * 0.88, s * 0.78);
        path.closePath();
        g.draw(path);
    }

    private static void refresh(Graphics2D g, double s) {
        g.draw(new java.awt.geom.Arc2D.Double(s * 0.16, s * 0.16, s * 0.68, s * 0.68,
                40, 280, java.awt.geom.Arc2D.OPEN));
        Path2D.Double head = new Path2D.Double();
        head.moveTo(s * 0.62, s * 0.18);
        head.lineTo(s * 0.86, s * 0.28);
        head.lineTo(s * 0.72, s * 0.48);
        g.draw(head);
    }

    private static void check(Graphics2D g, double s) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(s * 0.20, s * 0.52);
        path.lineTo(s * 0.42, s * 0.74);
        path.lineTo(s * 0.80, s * 0.26);
        g.draw(path);
    }

    /** Spinner used while something is loading. Angle comes from the clock. */
    public static void spinner(Graphics2D graphics, double x, double y, double size, Color color) {
        Graphics2D g = (Graphics2D) graphics.create();
        UiKit.prepare(g);
        double start = (System.currentTimeMillis() / 3.0) % 360;

        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(start + i * 45);
            int alpha = 30 + i * 28;
            g.setColor(StormTheme.alpha(color, Math.min(255, alpha)));
            double dotSize = size * 0.18;
            double cx = x + size / 2 + Math.cos(angle) * size * 0.34 - dotSize / 2;
            double cy = y + size / 2 + Math.sin(angle) * size * 0.34 - dotSize / 2;
            g.fill(new Ellipse2D.Double(cx, cy, dotSize, dotSize));
        }
        g.dispose();
    }
}
