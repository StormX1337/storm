package xyz.stormclient.module;

import java.util.ArrayList;
import java.util.List;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.bridge.IMinecraft;
import xyz.stormclient.bridge.IPlayer;
import xyz.stormclient.bridge.IWorld;
import xyz.stormclient.event.events.ModuleEvent;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.KeybindSetting;
import xyz.stormclient.setting.Setting;
import xyz.stormclient.util.Animation;
import xyz.stormclient.util.Keyboard;

/** Base class of every feature in Storm. */
public abstract class Module {

    private final String name;
    private final String description;
    private final Category category;

    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();

    private final KeybindSetting keybind = new KeybindSetting("Keybind", Keyboard.KEY_NONE);
    private final BooleanSetting drawn   = new BooleanSetting("Show in list", true);

    private boolean enabled;
    private boolean hidden;                       // never shown in the GUI (internal modules)

    /** Animation used by the GUI and the array list. */
    public final Animation animation = new Animation(6F);

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        addSettings(keybind, drawn);
    }

    // ------------------------------------------------------------------
    //  lifecycle
    // ------------------------------------------------------------------
    public void onEnable()  { }
    public void onDisable() { }
    /** Called once after all modules exist, safe place to look up other modules. */
    public void onInit()    { }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean value) {
        if (this.enabled == value) return;
        this.enabled = value;
        animation.set(value);

        if (value) {
            Storm.get().bus().register(this);
            try { onEnable(); } catch (Throwable t) { crash(t); }
        } else {
            try { onDisable(); } catch (Throwable t) { crash(t); }
            Storm.get().bus().unregister(this);
        }
        Storm.get().bus().post(new ModuleEvent(this, value));
        Storm.get().onModuleToggled(this);
    }

    /**
     * A module that throws on the way up is switched back off. That used to be
     * silent apart from a log line, which reads from the outside as a switch
     * that does nothing, so say it on screen too.
     */
    private void crash(Throwable t) {
        this.enabled = false;
        animation.set(false);
        Storm.get().bus().unregister(this);
        xyz.stormclient.util.StormLogger.error("module " + name + " threw, disabling it", t);
        Storm.get().notifications().error(name,
                "failed: " + (t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage()));
    }

    /** Re-applies the enabled state, used after a config load. */
    public void refresh() {
        if (enabled) {
            Storm.get().bus().register(this);
            onEnable();
        }
    }

    // ------------------------------------------------------------------
    //  settings
    // ------------------------------------------------------------------
    protected void addSettings(Setting<?>... values) {
        for (Setting<?> s : values) settings.add(s);
    }

    protected <T extends Setting<?>> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting<?>> settings() { return settings; }

    public Setting<?> setting(String name) {
        for (Setting<?> s : settings) {
            if (s.name().equalsIgnoreCase(name)) return s;
        }
        return null;
    }

    // ------------------------------------------------------------------
    //  accessors
    // ------------------------------------------------------------------
    public String name()        { return name; }
    public String description() { return description; }
    public Category category()  { return category; }
    public boolean isEnabled()  { return enabled; }

    public KeybindSetting keybindSetting() { return keybind; }
    public int  keybind()           { return keybind.get(); }
    public void setKeybind(int key) { keybind.set(key); }

    public boolean drawn()  { return drawn.get() && !hidden; }
    public boolean hidden() { return hidden; }
    protected void setHidden(boolean hidden) {
        this.hidden = hidden;
        settings.remove(drawn);
    }

    /** Extra text behind the name in the array list, e.g. the current mode. */
    public String tag() { return ""; }

    public String displayName() {
        String tag = tag();
        return tag == null || tag.isEmpty() ? name : name + " \u00a77" + tag;
    }

    // ------------------------------------------------------------------
    //  convenience shortcuts, every module uses these
    // ------------------------------------------------------------------
    protected IMinecraft mc()  { return Bridge.mc(); }
    protected IPlayer player() { return Bridge.mc().player(); }
    protected IWorld world()   { return Bridge.mc().world(); }
    protected boolean nullCheck() {
        return !Bridge.installed() || Bridge.mc().player() == null || Bridge.mc().world() == null;
    }
    protected void print(String message) { Bridge.mc().chat().print(message); }
}
