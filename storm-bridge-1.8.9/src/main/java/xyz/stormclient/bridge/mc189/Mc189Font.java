package xyz.stormclient.bridge.mc189;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.gui.FontRenderer;

import xyz.stormclient.bridge.IFontRenderer;

/**
 * Font access. Storm ships its own TrueType renderer for the UI and falls back
 * to the vanilla bitmap font when the custom font is not available, so nothing
 * ever renders as an empty box.
 */
public final class Mc189Font implements IFontRenderer {

    private static final Map<String, Mc189Font> CACHE = new HashMap<String, Mc189Font>();

    private final FontRenderer vanilla;
    private final StormFontRenderer custom;

    private Mc189Font(FontRenderer vanilla, StormFontRenderer custom) {
        this.vanilla = vanilla;
        this.custom = custom;
    }

    public static Mc189Font vanilla(FontRenderer renderer) {
        return new Mc189Font(renderer, null);
    }

    public static Mc189Font storm(String name, int size, FontRenderer fallback) {
        String key = name + "-" + size;
        Mc189Font cached = CACHE.get(key);
        if (cached != null) return cached;

        StormFontRenderer custom = StormFontRenderer.load(name, size);
        Mc189Font font = new Mc189Font(fallback, custom);
        CACHE.put(key, font);
        return font;
    }

    @Override public int width(String text) {
        return custom != null ? custom.width(text) : vanilla.getStringWidth(text);
    }

    @Override public int height() {
        return custom != null ? custom.height() : vanilla.FONT_HEIGHT;
    }

    @Override public void draw(String text, double x, double y, int argb) {
        if (custom != null) custom.draw(text, x, y, argb);
        else vanilla.drawString(text, (float) x, (float) y, argb, false);
    }

    @Override public void drawShadow(String text, double x, double y, int argb) {
        if (custom != null) {
            custom.draw(text, x + 0.6, y + 0.6, (argb & 0xFF000000) | 0x000000);
            custom.draw(text, x, y, argb);
        } else {
            vanilla.drawString(text, (float) x, (float) y, argb, true);
        }
    }

    @Override public void drawCentered(String text, double cx, double y, int argb) {
        draw(text, cx - width(text) / 2.0, y, argb);
    }

    @Override public void drawCenteredShadow(String text, double cx, double y, int argb) {
        drawShadow(text, cx - width(text) / 2.0, y, argb);
    }

    @Override public String trim(String text, int maxWidth) {
        if (width(text) <= maxWidth) return text;
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (width(sb.toString() + c + "...") > maxWidth) break;
            sb.append(c);
        }
        return sb.append("...").toString();
    }
}
