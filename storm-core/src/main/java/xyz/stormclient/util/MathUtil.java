package xyz.stormclient.util;

public final class MathUtil {

    private MathUtil() { }

    public static float clamp(float v, float min, float max) { return v < min ? min : (v > max ? max : v); }
    public static double clamp(double v, double min, double max) { return v < min ? min : (v > max ? max : v); }
    public static int clamp(int v, int min, int max) { return v < min ? min : (v > max ? max : v); }

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    public static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    /** Shortest signed difference between two yaw angles. */
    public static float wrapDegrees(float angle) {
        angle %= 360.0F;
        if (angle >= 180.0F) angle -= 360.0F;
        if (angle < -180.0F) angle += 360.0F;
        return angle;
    }

    public static float angleDiff(float from, float to) { return wrapDegrees(to - from); }

    public static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    public static boolean inside(double x, double y, double left, double top, double width, double height) {
        return x >= left && x <= left + width && y >= top && y <= top + height;
    }

    /** Smooth 0..1 ease used all over the UI. */
    public static float easeOut(float t) { return 1F - (float) Math.pow(1F - clamp(t, 0F, 1F), 3); }
    public static float easeInOut(float t) {
        t = clamp(t, 0F, 1F);
        return t < 0.5F ? 4F * t * t * t : 1F - (float) Math.pow(-2F * t + 2F, 3) / 2F;
    }

    public static double horizontalDistance(double dx, double dz) { return Math.sqrt(dx * dx + dz * dz); }
}
