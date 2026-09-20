package xyz.stormclient.setting;

import xyz.stormclient.util.ColorUtil;

/** ARGB colour with an optional rainbow mode, stored as a hex string. */
public class ColorSetting extends Setting<Integer> {

    private boolean rainbow;
    private float rainbowSpeed = 1.0F;

    public ColorSetting(String name, int argb) { super(name, argb); }

    public boolean rainbow() { return rainbow; }
    public void setRainbow(boolean rainbow) { this.rainbow = rainbow; }
    public float rainbowSpeed() { return rainbowSpeed; }
    public void setRainbowSpeed(float speed) { this.rainbowSpeed = speed; }

    /** The colour to actually draw with, resolves rainbow over time. */
    public int rgb() {
        return rainbow ? ColorUtil.rainbow(0, rainbowSpeed, alpha() / 255F) : get();
    }

    public int rgb(int offsetMillis) {
        return rainbow ? ColorUtil.rainbow(offsetMillis, rainbowSpeed, alpha() / 255F) : get();
    }

    public int alpha() { return (get() >> 24) & 0xFF; }
    public void setAlpha(int alpha) { set((get() & 0x00FFFFFF) | ((alpha & 0xFF) << 24)); }

    public float[] hsb() { return ColorUtil.toHsb(get()); }

    public void setHsb(float h, float s, float b) {
        set(ColorUtil.fromHsb(h, s, b, alpha()));
    }

    @Override public String serialize() {
        return String.format("%08X", get()) + (rainbow ? ":rainbow:" + rainbowSpeed : "");
    }

    @Override public void deserialize(String raw) {
        String[] parts = raw.split(":");
        try { set((int) Long.parseLong(parts[0], 16)); } catch (NumberFormatException ignored) { }
        rainbow = parts.length > 1 && "rainbow".equals(parts[1]);
        if (parts.length > 2) {
            try { rainbowSpeed = Float.parseFloat(parts[2]); } catch (NumberFormatException ignored) { }
        }
    }

    @Override public String display() {
        return rainbow ? "rainbow" : String.format("#%06X", get() & 0xFFFFFF);
    }
}
