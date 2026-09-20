package xyz.stormclient.setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {

    private final List<String> modes;

    public ModeSetting(String name, String value, String... modes) {
        super(name, value);
        this.modes = new ArrayList<String>(Arrays.asList(modes));
    }

    public List<String> modes() { return modes; }
    public int index() { return Math.max(0, modes.indexOf(get())); }

    public boolean is(String mode) { return get().equalsIgnoreCase(mode); }

    public void next() { set(modes.get((index() + 1) % modes.size())); }
    public void prev() { set(modes.get((index() - 1 + modes.size()) % modes.size())); }

    @Override public void set(String value) {
        for (String m : modes) {
            if (m.equalsIgnoreCase(value)) { super.set(m); return; }
        }
    }

    @Override public String serialize() { return get(); }
    @Override public void deserialize(String raw) { set(raw); }
}
