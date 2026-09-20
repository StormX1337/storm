package xyz.stormclient.bridge.mc189;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;

import org.lwjgl.opengl.GL11;

import xyz.stormclient.util.StormLogger;

/**
 * Storm's own text renderer: the font is baked into one texture atlas at load
 * time and drawn as textured quads. That keeps the client independent of the
 * vanilla bitmap font and makes the UI look the same on every version.
 *
 * <p>Fonts are loaded from {@code assets/storm/fonts/<name>.ttf} inside the jar,
 * falling back to a system sans serif when that file is missing.
 */
public final class StormFontRenderer {

    private static final int FIRST_CHAR = 32;
    private static final int LAST_CHAR = 255;
    private static final int PADDING = 2;

    private final int[] charWidth = new int[LAST_CHAR + 1];
    private final float[] charU = new float[LAST_CHAR + 1];
    private final float[] charV = new float[LAST_CHAR + 1];

    private final int textureWidth;
    private final int textureHeight;
    private final int lineHeight;
    private final int ascent;

    private DynamicTexture texture;

    private StormFontRenderer(Font font) {
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D probeGraphics = probe.createGraphics();
        probeGraphics.setFont(font);
        FontMetrics metrics = probeGraphics.getFontMetrics();
        this.lineHeight = metrics.getHeight();
        this.ascent = metrics.getAscent();

        int columns = 16;
        int cell = 0;
        for (int c = FIRST_CHAR; c <= LAST_CHAR; c++) {
            charWidth[c] = metrics.charWidth((char) c);
            cell = Math.max(cell, charWidth[c]);
        }
        cell = Math.max(cell, lineHeight) + PADDING * 2;
        probeGraphics.dispose();

        this.textureWidth = columns * cell;
        this.textureHeight = ((LAST_CHAR - FIRST_CHAR) / columns + 1) * cell;

        BufferedImage atlas = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = atlas.createGraphics();
        g.setFont(font);
        g.setColor(Color.WHITE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        for (int c = FIRST_CHAR; c <= LAST_CHAR; c++) {
            int index = c - FIRST_CHAR;
            int x = (index % columns) * cell;
            int y = (index / columns) * cell;
            charU[c] = x + PADDING;
            charV[c] = y + PADDING;
            g.drawString(String.valueOf((char) c), x + PADDING, y + PADDING + ascent);
        }
        g.dispose();

        try {
            this.texture = new DynamicTexture(atlas);
        } catch (Throwable t) {
            StormLogger.error("could not upload the font atlas", t);
        }
    }

    public static StormFontRenderer load(String name, int size) {
        Font font = null;
        InputStream stream = StormFontRenderer.class
                .getResourceAsStream("/assets/storm/fonts/" + name + ".ttf");
        if (stream != null) {
            try {
                font = Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(Font.PLAIN, (float) size);
            } catch (Exception e) {
                StormLogger.warn("could not read the " + name + " font, using a system font instead");
            } finally {
                try { stream.close(); } catch (Exception ignored) { }
            }
        }
        if (font == null) font = new Font("SansSerif", Font.PLAIN, size);

        try {
            return new StormFontRenderer(font);
        } catch (Throwable t) {
            StormLogger.error("font renderer failed, falling back to the vanilla font", t);
            return null;
        }
    }

    public int height() { return lineHeight; }

    public int width(String text) {
        if (text == null) return 0;
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) { i++; continue; }   // colour code
            if (c > LAST_CHAR) c = '?';
            width += charWidth[c];
        }
        return width;
    }

    public void draw(String text, double x, double y, int argb) {
        if (text == null || texture == null) return;

        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.bindTexture(texture.getGlTextureId());

        int color = argb;
        double cursor = x;

        GL11.glBegin(GL11.GL_QUADS);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\u00a7' && i + 1 < text.length()) {
                color = colorCode(text.charAt(++i), argb);
                continue;
            }
            if (c > LAST_CHAR) c = '?';
            if (c < FIRST_CHAR) continue;

            GlStateManager.color(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F,
                                 (color & 0xFF) / 255F, ((color >> 24) & 0xFF) / 255F);
            quad(cursor, y, c);
            cursor += charWidth[c];
        }
        GL11.glEnd();

        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.disableBlend();
    }

    private void quad(double x, double y, char c) {
        float u = charU[c] / textureWidth;
        float v = charV[c] / textureHeight;
        float u2 = (charU[c] + charWidth[c]) / textureWidth;
        float v2 = (charV[c] + lineHeight) / textureHeight;

        GL11.glTexCoord2f(u, v);   GL11.glVertex2d(x, y);
        GL11.glTexCoord2f(u, v2);  GL11.glVertex2d(x, y + lineHeight);
        GL11.glTexCoord2f(u2, v2); GL11.glVertex2d(x + charWidth[c], y + lineHeight);
        GL11.glTexCoord2f(u2, v);  GL11.glVertex2d(x + charWidth[c], y);
    }

    /** Keeps the alpha of the requested colour so faded text stays faded. */
    private int colorCode(char code, int original) {
        int alpha = original & 0xFF000000;
        String codes = "0123456789abcdef";
        int index = codes.indexOf(Character.toLowerCase(code));
        if (index < 0) return original;

        int base = index >= 8 ? index - 8 : index;
        int brightness = index >= 8 ? 255 : 170;
        int r = (base & 4) != 0 ? brightness : 0;
        int g = (base & 2) != 0 ? brightness : 0;
        int b = (base & 1) != 0 ? brightness : 0;
        if (index == 6) { r = 255; g = 170; b = 0; }
        if (index == 7 || index == 15) { r = g = b = index == 15 ? 255 : 170; }
        return alpha | (r << 16) | (g << 8) | b;
    }
}
