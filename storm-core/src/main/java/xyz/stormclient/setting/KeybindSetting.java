package xyz.stormclient.setting;

import xyz.stormclient.util.Keyboard;

public class KeybindSetting extends Setting<Integer> {

    private boolean listening;

    public KeybindSetting(String name, int key) { super(name, key); }

    public boolean listening() { return listening; }
    public void setListening(boolean listening) { this.listening = listening; }

    public boolean isBound() { return get() != Keyboard.KEY_NONE; }

    @Override public String serialize() { return Integer.toString(get()); }

    @Override public void deserialize(String raw) {
        try { set(Integer.parseInt(raw)); } catch (NumberFormatException ignored) { }
    }

    @Override public String display() {
        if (listening) return "...";
        return isBound() ? Keyboard.name(get()) : "none";
    }
}
