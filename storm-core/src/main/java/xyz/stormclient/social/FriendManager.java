package xyz.stormclient.social;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Names Storm will never attack, with an optional nickname for the HUD. */
public final class FriendManager {

    private final Map<String, String> friends = new LinkedHashMap<String, String>();

    public boolean isFriend(String name) {
        return name != null && friends.containsKey(name.toLowerCase());
    }

    public String alias(String name) {
        if (name == null) return "";
        String alias = friends.get(name.toLowerCase());
        return alias == null || alias.isEmpty() ? name : alias;
    }

    public boolean add(String name, String alias) {
        if (name == null || name.isEmpty()) return false;
        return friends.put(name.toLowerCase(), alias == null ? "" : alias) == null;
    }

    public boolean add(String name) { return add(name, ""); }

    public boolean remove(String name) {
        return name != null && friends.remove(name.toLowerCase()) != null;
    }

    public boolean toggle(String name) {
        if (isFriend(name)) { remove(name); return false; }
        add(name);
        return true;
    }

    public List<String> names() { return new ArrayList<String>(friends.keySet()); }

    public Map<String, String> entries() { return friends; }

    public void clear() { friends.clear(); }

    public int size() { return friends.size(); }
}
