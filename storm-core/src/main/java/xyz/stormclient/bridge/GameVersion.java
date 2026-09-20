package xyz.stormclient.bridge;

/**
 * Every Minecraft release Storm knows how to drive.
 * The protocol number lets modules branch on behaviour instead of on a name
 * (1.9+ combat, 1.13+ flattening, 1.17+ height limits, ...).
 */
public enum GameVersion {

    V1_7_10 ("1.7.10",  5),
    V1_8_9  ("1.8.9",  47),
    V1_12_2 ("1.12.2", 340),
    V1_16_5 ("1.16.5", 754),
    V1_18_2 ("1.18.2", 758),
    V1_19_4 ("1.19.4", 762),
    V1_20_4 ("1.20.4", 765),
    V1_20_6 ("1.20.6", 766),
    V1_21_4 ("1.21.4", 769),
    UNKNOWN ("unknown", -1);

    private final String id;
    private final int protocol;

    GameVersion(String id, int protocol) {
        this.id = id;
        this.protocol = protocol;
    }

    public String id()      { return id; }
    public int protocol()   { return protocol; }

    /** Legacy combat: no attack cooldown, blocking with a sword, 1.8 hit registration. */
    public boolean isLegacyCombat() { return protocol <= 47 && protocol > 0; }

    /** Flattened block/item ids (1.13+). */
    public boolean isFlattened()    { return protocol >= 393; }

    public boolean atLeast(GameVersion other) { return this.protocol >= other.protocol; }

    public static GameVersion fromId(String id) {
        for (GameVersion v : values()) if (v.id.equalsIgnoreCase(id)) return v;
        return UNKNOWN;
    }

    public static GameVersion fromProtocol(int protocol) {
        for (GameVersion v : values()) if (v.protocol == protocol) return v;
        return UNKNOWN;
    }
}
