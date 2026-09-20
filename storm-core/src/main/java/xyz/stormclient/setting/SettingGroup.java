package xyz.stormclient.setting;

import java.util.ArrayList;
import java.util.List;

/** Optional grouping so big modules can fold their settings in the GUI. */
public class SettingGroup extends Setting<Boolean> {

    private final List<Setting<?>> children = new ArrayList<Setting<?>>();

    public SettingGroup(String name, boolean expanded) { super(name, expanded); }

    public SettingGroup add(Setting<?>... settings) {
        for (Setting<?> s : settings) children.add(s);
        return this;
    }

    public List<Setting<?>> children() { return children; }
    public boolean expanded() { return get(); }
    public void toggle() { set(!get()); }

    @Override public String serialize() { return Boolean.toString(get()); }
    @Override public void deserialize(String raw) { set(Boolean.parseBoolean(raw)); }
}
