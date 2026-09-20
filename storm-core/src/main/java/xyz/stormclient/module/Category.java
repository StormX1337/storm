package xyz.stormclient.module;

public enum Category {

    COMBAT   ("Combat",   "\u2694"),
    MOVEMENT ("Movement", "\u21c9"),
    PLAYER   ("Player",   "\u263a"),
    RENDER   ("Render",   "\u25c9"),
    WORLD    ("World",    "\u25a6"),
    HUD      ("HUD",      "\u25a4"),
    MISC     ("Misc",     "\u2699");

    private final String label;
    private final String icon;

    Category(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String label() { return label; }
    public String icon()  { return icon; }
}
