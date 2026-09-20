package xyz.stormclient.ui.theme;

import xyz.stormclient.util.ColorUtil;

/**
 * Storm's look. One accent colour drives the whole UI, the rest of the palette
 * is derived from it so a user only ever has to pick one value.
 */
public final class Theme {

    public enum Preset {
        STORM   (0xFF35C4FF, 0xFF0E1116, 0xFF151A21),   // the default blue
        MIDNIGHT(0xFF7A5CFF, 0xFF0B0B12, 0xFF141420),
        EMBER   (0xFFFF7A3D, 0xFF141010, 0xFF1E1615),
        MINT    (0xFF45E0A0, 0xFF0C1412, 0xFF14201C),
        LIGHT   (0xFF2F80ED, 0xFFF2F4F8, 0xFFFFFFFF);

        public final int accent;
        public final int background;
        public final int panel;

        Preset(int accent, int background, int panel) {
            this.accent = accent;
            this.background = background;
            this.panel = panel;
        }
    }

    private Preset preset = Preset.STORM;
    private int accent = Preset.STORM.accent;
    private boolean rainbow;
    private float radius = 6F;
    private float animationSpeed = 6F;
    private boolean shadows = true;
    private boolean blur = true;
    private String font = "storm";
    private int fontSize = 18;

    public Preset preset() { return preset; }

    public void setPreset(Preset preset) {
        this.preset = preset;
        this.accent = preset.accent;
    }

    public int accent()          { return rainbow ? ColorUtil.rainbow(0, 0.6F, 1F) : accent; }
    public int accent(int offset){ return rainbow ? ColorUtil.rainbow(offset, 0.6F, 1F) : accent; }
    public void setAccent(int accent) { this.accent = accent; }

    public boolean rainbow() { return rainbow; }
    public void setRainbow(boolean rainbow) { this.rainbow = rainbow; }

    public int background()  { return preset.background; }
    public int panel()       { return preset.panel; }
    public int panelLight()  { return ColorUtil.brighter(preset.panel, 1.35F); }
    public int panelDark()   { return ColorUtil.darker(preset.panel, 0.7F); }
    public int outline()     { return ColorUtil.withAlpha(ColorUtil.brighter(preset.panel, 2.2F), 90); }
    public int text()        { return preset == Preset.LIGHT ? 0xFF16181D : 0xFFE9EDF4; }
    public int textDim()     { return ColorUtil.withAlpha(text(), 150); }
    public int textFaint()   { return ColorUtil.withAlpha(text(), 90); }
    public int overlay()     { return 0x66000000; }

    public float radius()    { return radius; }
    public void setRadius(float radius) { this.radius = radius; }

    public float animationSpeed() { return animationSpeed; }
    public void setAnimationSpeed(float speed) { this.animationSpeed = speed; }

    public boolean shadows() { return shadows; }
    public void setShadows(boolean shadows) { this.shadows = shadows; }

    public boolean blur()    { return blur; }
    public void setBlur(boolean blur) { this.blur = blur; }

    public String font()     { return font; }
    public void setFont(String font) { this.font = font; }

    public int fontSize()    { return fontSize; }
    public void setFontSize(int size) { this.fontSize = size; }
}
