package xyz.stormclient.module.impl.movement;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.SlowDownEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;

/** Keeps your speed while eating, drinking, blocking or pulling a bow. */
public class NoSlow extends Module {

    private final NumberSetting multiplier = add(new NumberSetting("Multiplier", 100, 20, 100, 5).suffix("%"));
    private final BooleanSetting items  = add(new BooleanSetting("Items", true));
    private final BooleanSetting blocks = add(new BooleanSetting("Blocking", true));
    private final BooleanSetting soulSand = add(new BooleanSetting("Soul sand", false));

    public NoSlow() {
        super("NoSlow", "No movement penalty while using items", Category.MOVEMENT);
    }

    @Subscribe
    public void onSlowDown(SlowDownEvent event) {
        if (nullCheck()) return;
        boolean blocking = player().isBlocking();
        if (blocking && !blocks.get()) return;
        if (!blocking && !items.get()) return;

        event.set(multiplier.getFloat() / 100F);
    }

    public boolean soulSand() { return isEnabled() && soulSand.get(); }
}
