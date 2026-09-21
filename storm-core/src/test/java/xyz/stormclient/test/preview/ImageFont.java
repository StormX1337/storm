package xyz.stormclient.test.preview;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

import xyz.stormclient.bridge.IFontRenderer;

/**
 * Mirrors what the 1.8.9 bridge does: one AWT font, drawn from the top left,
 * so the headless picture shows the same metrics the game will use.
 */
public class ImageFont implements IFontRenderer {

    private final Graphics2D g;
    private final Font font;
    private final FontMetrics metrics;

    public ImageFont(Graphics2D g, int size) {
        this.g = g;
        this.font = new Font("DejaVu Sans", Font.PLAIN, size);
        this.metrics = g.getFontMetrics(font);
    }

    public int width(String text) {
        if (text == null) return 0;
        return metrics.stringWidth(strip(text));
    }

    public int height() { return metrics.getHeight(); }

    public void draw(String text, double x, double y, int argb) {
        if (text == null) return;
        g.setFont(font);
        g.setColor(new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF));
        g.drawString(strip(text), (float) x, (float) (y + metrics.getAscent()));
    }

    public void drawShadow(String text, double x, double y, int argb) { draw(text, x, y, argb); }

    public void drawCentered(String text, double cx, double y, int argb) {
        draw(text, cx - width(text) / 2.0, y, argb);
    }

    public void drawCenteredShadow(String text, double cx, double y, int argb) {
        drawCentered(text, cx, y, argb);
    }

    public String trim(String text, int maxWidth) {
        if (width(text) <= maxWidth) return text;
        StringBuilder sb = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (width(sb.toString() + ch + "...") > maxWidth) break;
            sb.append(ch);
        }
        return sb.append("...").toString();
    }

    private static String strip(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '§' && i + 1 < text.length()) { i++; continue; }
            sb.append(ch);
        }
        return sb.toString();
    }
}
