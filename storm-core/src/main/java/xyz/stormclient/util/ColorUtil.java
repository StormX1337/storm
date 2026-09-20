package xyz.stormclient.util;

public final class ColorUtil {

    private ColorUtil() { }

    public static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int rgb(int r, int g, int b) { return argb(255, r, g, b); }

    public static int alpha(int argb) { return (argb >> 24) & 0xFF; }
    public static int red(int argb)   { return (argb >> 16) & 0xFF; }
    public static int green(int argb) { return (argb >> 8) & 0xFF; }
    public static int blue(int argb)  { return argb & 0xFF; }

    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    public static int withAlpha(int argb, float alpha) {
        return withAlpha(argb, (int) (MathUtil.clamp(alpha, 0F, 1F) * 255));
    }

    /** Multiplies the existing alpha, used for fading whole panels. */
    public static int fade(int argb, float factor) {
        return withAlpha(argb, (int) (alpha(argb) * MathUtil.clamp(factor, 0F, 1F)));
    }

    public static int mix(int a, int b, float t) {
        t = MathUtil.clamp(t, 0F, 1F);
        return argb(
                (int) (alpha(a) + (alpha(b) - alpha(a)) * t),
                (int) (red(a)   + (red(b)   - red(a))   * t),
                (int) (green(a) + (green(b) - green(a)) * t),
                (int) (blue(a)  + (blue(b)  - blue(a))  * t));
    }

    public static int darker(int argb, float factor) {
        return argb(alpha(argb), (int) (red(argb) * factor), (int) (green(argb) * factor), (int) (blue(argb) * factor));
    }

    public static int brighter(int argb, float factor) {
        return argb(alpha(argb),
                Math.min(255, (int) (red(argb) * factor)),
                Math.min(255, (int) (green(argb) * factor)),
                Math.min(255, (int) (blue(argb) * factor)));
    }

    public static int rainbow(int offsetMillis, float speed, float alpha) {
        float hue = ((System.currentTimeMillis() + offsetMillis) % (long) (4000 / Math.max(0.05F, speed)))
                / (4000F / Math.max(0.05F, speed));
        return fromHsb(hue, 0.75F, 1.0F, (int) (MathUtil.clamp(alpha, 0F, 1F) * 255));
    }

    /** Colour ramp from green (full) to red (empty), used for health and durability. */
    public static int health(float fraction) {
        fraction = MathUtil.clamp(fraction, 0F, 1F);
        return fromHsb(fraction / 3F, 0.85F, 1.0F, 255);
    }

    public static int fromHsb(float h, float s, float b, int alpha) {
        h = h - (float) Math.floor(h);
        int r = 0, g = 0, bl = 0;
        if (s == 0) {
            r = g = bl = (int) (b * 255 + 0.5F);
        } else {
            float hh = (h - (float) Math.floor(h)) * 6.0F;
            float f = hh - (float) Math.floor(hh);
            float p = b * (1.0F - s);
            float q = b * (1.0F - s * f);
            float t = b * (1.0F - (s * (1.0F - f)));
            switch ((int) hh) {
                case 0: r = (int) (b * 255 + 0.5F); g = (int) (t * 255 + 0.5F); bl = (int) (p * 255 + 0.5F); break;
                case 1: r = (int) (q * 255 + 0.5F); g = (int) (b * 255 + 0.5F); bl = (int) (p * 255 + 0.5F); break;
                case 2: r = (int) (p * 255 + 0.5F); g = (int) (b * 255 + 0.5F); bl = (int) (t * 255 + 0.5F); break;
                case 3: r = (int) (p * 255 + 0.5F); g = (int) (q * 255 + 0.5F); bl = (int) (b * 255 + 0.5F); break;
                case 4: r = (int) (t * 255 + 0.5F); g = (int) (p * 255 + 0.5F); bl = (int) (b * 255 + 0.5F); break;
                case 5: r = (int) (b * 255 + 0.5F); g = (int) (p * 255 + 0.5F); bl = (int) (q * 255 + 0.5F); break;
                default: break;
            }
        }
        return argb(alpha, r, g, bl);
    }

    public static float[] toHsb(int argb) {
        int r = red(argb), g = green(argb), b = blue(argb);
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        float brightness = max / 255F;
        float saturation = max == 0 ? 0 : (max - min) / (float) max;
        float hue = 0;
        if (saturation != 0) {
            float rc = (max - r) / (float) (max - min);
            float gc = (max - g) / (float) (max - min);
            float bc = (max - b) / (float) (max - min);
            if (r == max)      hue = bc - gc;
            else if (g == max) hue = 2.0F + rc - bc;
            else               hue = 4.0F + gc - rc;
            hue /= 6.0F;
            if (hue < 0) hue += 1.0F;
        }
        return new float[] { hue, saturation, brightness };
    }

    public static int parseHex(String hex) {
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        if (s.length() == 6) s = "FF" + s;
        try { return (int) Long.parseLong(s, 16); } catch (NumberFormatException e) { return 0xFFFFFFFF; }
    }
}
