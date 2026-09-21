package xyz.stormclient.ui.theme;

import xyz.stormclient.util.ColorUtil;

/**
 * Storm's look. One accent colour drives the whole UI, the rest of the palette
 * is derived from it so a user only ever has to pick one value.
 */
public final class Theme {

    public enum Preset {
        STORM   (0xFF4C7DFF, 0xFF070912, 0xFF10131E),   // the default electric blue
        MIDNIGHT(0xFF7C6CFF, 0xFF06060E, 0xFF0F0F1A),
        EMBER   (0xFFFF6B3D, 0xFF100A09, 0xFF1A1211),
        MINT    (0xFF2FE0A6, 0xFF06120F, 0xFF0E1C18),
        ROSE    (0xFFFF5C8A, 0xFF100810, 0xFF1B111A),
        LIGHT   (0xFF2F6BED, 0xFFEDF0F6, 0xFFFFFFFF);

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
    private float radius = 5F;
    private float animationSpeed = 6F;
    private boolean shadows = true;
    private boolean blur = true;
    private String font = "storm";
    private int fontSize = 10;

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
    public int panelLight()  { return ColorUtil.brighter(preset.panel, 1.45F); }
    public int panelDark()   { return ColorUtil.darker(preset.panel, 0.55F); }

    /** A hairline that reads as an edge without turning into a hard border. */
    public int outline()     { return ColorUtil.withAlpha(text(), preset == Preset.LIGHT ? 26 : 22); }

    public int text()        { return preset == Preset.LIGHT ? 0xFF14161C : 0xFFF2F4FA; }
    public int textDim()     { return ColorUtil.withAlpha(text(), 165); }
    public int textFaint()   { return ColorUtil.withAlpha(text(), 105); }
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
