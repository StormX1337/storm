package xyz.stormclient.test.preview;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

import xyz.stormclient.bridge.IRenderer;

/** Draws Storm's UI into a BufferedImage so the layout can be checked headless. */
public class ImageRenderer implements IRenderer {

    public final BufferedImage image;
    private final Graphics2D g;
    private final Deque<AffineTransform> stack = new ArrayDeque<AffineTransform>();
    private final Deque<Shape> clips = new ArrayDeque<Shape>();

    public ImageRenderer(int w, int h) {
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static Color c(int argb) {
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
    }

    public Graphics2D graphics() { return g; }

    public void push() { stack.push(g.getTransform()); }
    public void pop()  { if (!stack.isEmpty()) g.setTransform(stack.pop()); }
    public void translate(double x, double y, double z) { g.translate(x, y); }
    public void scale(double x, double y, double z) { g.scale(x, y); }
    public void rotate(float a, float x, float y, float z) { g.rotate(Math.toRadians(a)); }
    public void color(int argb) { g.setColor(c(argb)); }
    public void enableBlend() { } public void disableBlend() { }
    public void enableDepth() { } public void disableDepth() { }
    public void lineWidth(float w) { g.setStroke(new BasicStroke(w)); }

    public void rect(double x, double y, double w, double h, int argb) {
        g.setColor(c(argb));
        g.fill(new Rectangle2D.Double(x, y, w, h));
    }

    public void rectOutline(double x, double y, double w, double h, float t, int argb) {
        g.setColor(c(argb));
        g.setStroke(new BasicStroke(t));
        g.draw(new Rectangle2D.Double(x, y, w, h));
    }

    public void roundedRect(double x, double y, double w, double h, float r, int argb) {
        g.setColor(c(argb));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2));
    }

    public void roundedRectOutline(double x, double y, double w, double h, float r, float t, int argb) {
        g.setColor(c(argb));
        g.setStroke(new BasicStroke(t));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2));
    }

    public void gradientRect(double x, double y, double w, double h, int top, int bottom) {
        if (h <= 0 || w <= 0) return;
        g.setPaint(new GradientPaint((float) x, (float) y, c(top), (float) x, (float) (y + h), c(bottom)));
        g.fill(new Rectangle2D.Double(x, y, w, h));
        g.setPaint(null);
        g.setColor(Color.WHITE);
    }

    public void gradientRectH(double x, double y, double w, double h, int left, int right) {
        if (h <= 0 || w <= 0) return;
        g.setPaint(new GradientPaint((float) x, (float) y, c(left), (float) (x + w), (float) y, c(right)));
        g.fill(new Rectangle2D.Double(x, y, w, h));
        g.setPaint(null);
        g.setColor(Color.WHITE);
    }

    public void circle(double cx, double cy, double r, int argb) {
        g.setColor(c(argb));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    public void arc(double cx, double cy, double r, float s, float e, float t, int argb) {
        g.setColor(c(argb));
        g.setStroke(new BasicStroke(t));
        g.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, -s, -(e - s), Arc2D.OPEN));
    }

    public void shadow(double x, double y, double w, double h, float r, int argb) {
        int alpha = argb >>> 24;
        for (int i = 6; i > 0; i--) {
            roundedRect(x - i, y - i + 1, w + i * 2, h + i * 2, r + i,
                    (argb & 0x00FFFFFF) | ((alpha / (i + 2)) << 24));
        }
    }

    public void blur(double x, double y, double w, double h, float strength) { }

    public void image(String res, double x, double y, double w, double h, int argb) { }

    /** ArrayDeque rejects nulls, and "no clip" is exactly what getClip returns. */
    private static final Shape NO_CLIP = new Rectangle2D.Double(-1e9, -1e9, 2e9, 2e9);

    public void scissorBegin(double x, double y, double w, double h) {
        Shape current = g.getClip();
        clips.push(current == null ? NO_CLIP : current);
        g.clip(new Rectangle2D.Double(x, y, w, h));
    }

    public void scissorEnd() {
        if (clips.isEmpty()) return;
        Shape previous = clips.pop();
        g.setClip(previous == NO_CLIP ? null : previous);
    }

    public void box3D(double a, double b, double c2, double d, double e, double f, int argb, boolean filled) { }
    public void line3D(double a, double b, double c2, double d, double e, double f, int argb, float w) { }
    public double[] project(double x, double y, double z) { return null; }
    public void beginEntityOutline() { }
    public void endEntityOutline(int argb) { }
}
