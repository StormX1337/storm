package xyz.stormclient.setting;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, boolean value) { super(name, value); }

    public void toggle() { set(!get()); }

    @Override public String serialize() { return Boolean.toString(get()); }

    @Override public void deserialize(String raw) {
        set(Boolean.parseBoolean(raw));
    }

    @Override public String display() { return get() ? "on" : "off"; }
}
