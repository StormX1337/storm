package xyz.stormclient.setting;

public class StringSetting extends Setting<String> {

    private int maxLength = 128;

    public StringSetting(String name, String value) { super(name, value); }

    public StringSetting maxLength(int maxLength) { this.maxLength = maxLength; return this; }
    public int maxLength() { return maxLength; }

    @Override public void set(String value) {
        super.set(value == null ? "" : (value.length() > maxLength ? value.substring(0, maxLength) : value));
    }

    @Override public String serialize() { return get(); }
    @Override public void deserialize(String raw) { set(raw); }
}
