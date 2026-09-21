package xyz.stormclient;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.bridge.IMinecraft;
import xyz.stormclient.bridge.IScreen;
import xyz.stormclient.command.CommandManager;
import xyz.stormclient.config.ConfigManager;
import xyz.stormclient.event.EventBus;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.AttackEvent;
import xyz.stormclient.event.events.MouseEvent;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.event.events.WorldEvent;
import xyz.stormclient.licence.Licence;
import xyz.stormclient.licence.LicenceVerifier;
import xyz.stormclient.module.Module;
import xyz.stormclient.module.ModuleManager;
import xyz.stormclient.module.impl.combat.AntiBot;
import xyz.stormclient.rotation.RotationManager;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.setting.Setting;
import xyz.stormclient.setting.StringSetting;
import xyz.stormclient.social.FriendManager;
import xyz.stormclient.ui.hud.HudManager;
import xyz.stormclient.ui.notify.Notification;
import xyz.stormclient.ui.notify.NotificationManager;
import xyz.stormclient.ui.theme.Theme;
import xyz.stormclient.util.StormLogger;

/**
 * The client itself. One instance, created once the version bridge is in
 * place, owning every manager Storm has.
 */
public final class Storm {

    private static Storm instance;

    private final EventBus bus = new EventBus();
    private final ModuleManager modules = new ModuleManager();
    private final CommandManager commands = new CommandManager();
    private final FriendManager friends = new FriendManager();
    private final NotificationManager notifications = new NotificationManager();
    private final HudManager hud = new HudManager();
    private final RotationManager rotations = new RotationManager();
    private final Theme theme = new Theme();

    private ConfigManager config;
    private Licence licence = LicenceVerifier.verify(null);

    // client wide options, shown on the menu's settings page
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();
    private final StringSetting  prefix        = new StringSetting("Command prefix", ".")
            .describe("What a chat message has to start with to reach Storm");
    private final BooleanSetting notifications0 = new BooleanSetting("Notifications", true)
            .describe("Show a card when a module is toggled");
    private final NumberSetting  notifyTime    = new NumberSetting("Notification time", 2500, 500, 8000, 250)
            .describe("How long a card stays on screen");
    private final BooleanSetting hudInMenu     = new BooleanSetting("HUD in menus", true)
            .describe("Keep drawing the HUD while a Storm screen is open");

    private final Deque<Long> leftClicks = new ArrayDeque<Long>();
    private final Deque<Long> rightClicks = new ArrayDeque<Long>();

    private IEntity lastAttacked;
    private long lastAttackTime;
    private boolean initialised;

    private Storm() {
        settings.add(prefix);
        settings.add(notifications0);
        settings.add(notifyTime);
        settings.add(hudInMenu);
        prefix.onChange(() -> commands.setPrefix(prefix.get()));
        notifications0.onChange(() -> notifications.setEnabled(notifications0.get()));
        notifyTime.onChange(() -> notifications.setDefaultDuration(notifyTime.getLong()));
    }

    public static Storm get() {
        if (instance == null) instance = new Storm();
        return instance;
    }

    // ------------------------------------------------------------------
    //  lifecycle
    // ------------------------------------------------------------------
    /** Called by a bridge once {@link Bridge#install} has run. */
    public void start() {
        if (initialised) {
            StormLogger.warn("start() called twice, ignoring");
            return;
        }
        long began = System.currentTimeMillis();
        initialised = true;

        // the launcher hands the blob down as a system property, so both the
        // agent path and a plain mod install end up here
        licence = LicenceVerifier.verify(System.getProperty("storm.licence"));

        IMinecraft mc = Bridge.mc();
        StormLogger.info(StormInfo.FULL_NAME + " " + StormInfo.VERSION + " starting on " + mc.version().id());

        File gameDirectory = mc.gameDirectory();
        config = new ConfigManager(gameDirectory);

        if (!licence.valid()) {
            StormLogger.error("licence rejected: " + licence.reason());
            commands.init();
            bus.register(this);
            bus.register(commands);
            bus.register(notifications);
            notifications.push(new Notification(StormInfo.NAME,
                    licence.reason(), Notification.Type.ERROR, 10000));
            StormLogger.info("running without modules until a valid key is supplied");
            return;
        }
        if (LicenceVerifier.enforced()) {
            StormLogger.info("licence ok for " + licence.holder() + " (" + licence.statusLine() + ")");
        }

        modules.init();
        commands.init();

        bus.register(this);
        bus.register(modules);
        bus.register(commands);
        bus.register(rotations);
        bus.register(notifications);

        for (Module module : modules.all()) {
            if (module.isEnabled()) module.refresh();
        }

        config.loadOrCreateDefault();

        StormLogger.info("ready in " + (System.currentTimeMillis() - began) + " ms");
        notifications.push(new Notification(StormInfo.NAME,
                "v" + StormInfo.VERSION + " loaded on " + mc.version().id(),
                Notification.Type.SUCCESS, 4000));
    }

    public void shutdown() {
        if (!initialised) return;
        StormLogger.info("shutting down");
        if (config != null) config.saveCurrent();
        for (Module module : modules.enabled()) module.setEnabled(false);
        bus.clear();
        initialised = false;
    }

    public boolean initialised() { return initialised; }

    // ------------------------------------------------------------------
    //  managers
    // ------------------------------------------------------------------
    public EventBus bus()                     { return bus; }
    public ModuleManager modules()            { return modules; }
    public CommandManager commands()          { return commands; }
    public FriendManager friends()            { return friends; }
    public NotificationManager notifications(){ return notifications; }
    public HudManager hud()                   { return hud; }
    public RotationManager rotations()        { return rotations; }
    public Theme theme()                      { return theme; }
    public ConfigManager config()             { return config; }
    public Licence licence()                  { return licence; }
    public List<Setting<?>> settings()        { return settings; }
    public boolean drawHudInMenus()           { return hudInMenu.get(); }

    public AntiBot antiBot() { return modules.get(AntiBot.class); }

    // ------------------------------------------------------------------
    //  shared state
    // ------------------------------------------------------------------
    public void openScreen(IScreen screen) {
        Bridge.mc().gui().open(screen);
    }

    public void onModuleToggled(Module module) {
        if (!initialised || module.hidden()) return;
        notifications.push(new Notification(module.name(),
                module.isEnabled() ? "enabled" : "disabled",
                module.isEnabled() ? Notification.Type.SUCCESS : Notification.Type.INFO, 1500));
    }

    public void onAttack(IEntity target) {
        lastAttacked = target;
        lastAttackTime = System.currentTimeMillis();
        bus.post(new AttackEvent(target));
    }

    public IEntity lastAttacked() { return lastAttacked; }

    public long sinceLastAttack() { return System.currentTimeMillis() - lastAttackTime; }

    /** Clicks in the last second for the given mouse button. */
    public int cps(int button) {
        Deque<Long> clicks = button == 0 ? leftClicks : rightClicks;
        long now = System.currentTimeMillis();
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.pollFirst();
        return clicks.size();
    }

    // ------------------------------------------------------------------
    //  core listeners
    // ------------------------------------------------------------------
    @Subscribe
    public void onMouse(MouseEvent event) {
        if (!event.down()) return;
        if (event.button() == 0) leftClicks.addLast(System.currentTimeMillis());
        else if (event.button() == 1) rightClicks.addLast(System.currentTimeMillis());
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        notifications.prune();
        cps(0);
        cps(1);
    }

    @Subscribe
    public void onWorld(WorldEvent.Load event) {
        lastAttacked = null;
        rotations.clear();
    }
}
