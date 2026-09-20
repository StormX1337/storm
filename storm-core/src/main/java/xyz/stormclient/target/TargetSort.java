package xyz.stormclient.target;

public enum TargetSort {
    DISTANCE,
    HEALTH,
    ANGLE,
    ARMOR,
    HURT_TIME;

    public static String[] names() {
        String[] out = new String[values().length];
        for (int i = 0; i < values().length; i++) out[i] = values()[i].name().toLowerCase().replace('_', ' ');
        return out;
    }

    public static TargetSort of(String name) {
        for (TargetSort s : values()) {
            if (s.name().equalsIgnoreCase(name.replace(' ', '_'))) return s;
        }
        return DISTANCE;
    }
}
