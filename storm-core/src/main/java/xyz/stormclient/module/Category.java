package xyz.stormclient.module;

public enum Category {

    COMBAT   ("Combat",   "⚔"),
    MOVEMENT ("Movement", "⇉"),
    PLAYER   ("Player",   "☺"),
    RENDER   ("Render",   "◉"),
    WORLD    ("World",    "▦"),
    HUD      ("HUD",      "▤"),
    MISC     ("Misc",     "⚙");

    private final String label;
    private final String icon;

    Category(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String label() { return label; }
    public String icon()  { return icon; }
}
