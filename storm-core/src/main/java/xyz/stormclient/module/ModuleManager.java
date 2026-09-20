package xyz.stormclient.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.KeyEvent;
import xyz.stormclient.module.impl.combat.*;
import xyz.stormclient.module.impl.hud.*;
import xyz.stormclient.module.impl.misc.*;
import xyz.stormclient.module.impl.movement.*;
import xyz.stormclient.module.impl.player.*;
import xyz.stormclient.module.impl.render.*;
import xyz.stormclient.module.impl.world.*;
import xyz.stormclient.util.Keyboard;
import xyz.stormclient.util.StormLogger;

public final class ModuleManager {

    private final List<Module> modules = new ArrayList<Module>();
    private final Map<Class<? extends Module>, Module> byClass = new LinkedHashMap<Class<? extends Module>, Module>();

    public void init() {
        register(
                // ---- combat ----
                new KillAura(), new AutoClicker(), new Criticals(), new Reach(),
                new Velocity(), new AntiBot(), new HitBoxes(), new AutoBlock(),
                new TriggerBot(), new AimAssist(), new WTap(),

                // ---- movement ----
                new Sprint(), new Speed(), new NoSlow(), new Fly(), new Step(),
                new LongJump(), new InventoryMove(), new Jesus(), new NoWeb(),
                new Sneak(), new AntiVoid(), new Spider(),

                // ---- player ----
                new AutoTool(), new ChestStealer(), new InventoryManager(), new FastPlace(),
                new NoFall(), new Blink(), new Freecam(), new AutoRespawn(), new Scaffold(),
                new AutoArmor(), new FastBreak(),

                // ---- render ----
                new Esp(), new Tracers(), new Chams(), new Nametags(), new Fullbright(),
                new NoHurtCam(), new ViewClip(), new Animations(), new Trajectories(),
                new BlockHighlight(), new CustomFov(), new Zoom(), new Shaders(), new ClickGuiModule(),

                // ---- world ----
                new Timer(), new Nuker(), new AutoSign(), new BedProtect(),

                // ---- hud ----
                new Hud(), new Keystrokes(), new TargetHud(), new ArmorHud(), new PotionHud(),
                new Crosshair(), new Scoreboard(), new BlockCounter(), new HudEditor(),

                // ---- misc ----
                new AutoGg(), new AntiAfk(), new ChatSuffix(), new NameProtect(),
                new DiscordRpc(), new AutoReconnect(), new Notifier()
        );

        for (Module m : modules) {
            try { m.onInit(); } catch (Throwable t) { StormLogger.error("init of " + m.name() + " failed", t); }
        }
        sort();
        StormLogger.info("registered " + modules.size() + " modules");
    }

    private void register(Module... values) {
        for (Module m : values) {
            modules.add(m);
            byClass.put(m.getClass(), m);
        }
    }

    private void sort() {
        Collections.sort(modules, new Comparator<Module>() {
            public int compare(Module a, Module b) {
                int cat = a.category().compareTo(b.category());
                return cat != 0 ? cat : a.name().compareToIgnoreCase(b.name());
            }
        });
    }

    public List<Module> all() { return modules; }

    public List<Module> byCategory(Category category) {
        List<Module> out = new ArrayList<Module>();
        for (Module m : modules) if (m.category() == category && !m.hidden()) out.add(m);
        return out;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) { return (T) byClass.get(type); }

    public Module byName(String name) {
        for (Module m : modules) if (m.name().equalsIgnoreCase(name)) return m;
        return null;
    }

    public List<Module> enabled() {
        List<Module> out = new ArrayList<Module>();
        for (Module m : modules) if (m.isEnabled()) out.add(m);
        return out;
    }

    public boolean isEnabled(Class<? extends Module> type) {
        Module m = byClass.get(type);
        return m != null && m.isEnabled();
    }

    /** Fuzzy search used by the click GUI search bar and the toggle command. */
    public List<Module> search(String query) {
        List<Module> out = new ArrayList<Module>();
        if (query == null || query.isEmpty()) return out;
        String q = query.toLowerCase();
        for (Module m : modules) {
            if (m.hidden()) continue;
            if (m.name().toLowerCase().contains(q) || m.description().toLowerCase().contains(q)) out.add(m);
        }
        return out;
    }

    @Subscribe
    public void onKey(KeyEvent event) {
        if (event.key() == Keyboard.KEY_NONE) return;
        for (Module m : modules) {
            if (m.keybind() == event.key()) m.toggle();
        }
    }
}
