package xyz.stormclient.module;

public enum Category {

    COMBAT   ("Combat",   "C"),
    MOVEMENT ("Movement", "M"),
    PLAYER   ("Player",   "P"),
    RENDER   ("Render",   "R"),
    WORLD    ("World",    "W"),
    HUD      ("HUD",      "H"),
    MISC     ("Misc",     "X");

    private final String label;
    private final String icon;

    Category(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String label() { return label; }

    /**
     * A one letter stand in. The click GUI draws a pictogram instead
     * ({@link xyz.stormclient.ui.Glyphs#category}); this is only what text-only
     * surfaces such as the chat fall back to.
     */
    public String icon()  { return icon; }
}
