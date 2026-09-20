package xyz.stormclient.module.impl.combat;

import xyz.stormclient.bridge.PacketType;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.PacketEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.setting.NumberSetting;

/** Reduces the knockback taken from hits and explosions. */
public class Velocity extends Module {

    private final ModeSetting   mode       = add(new ModeSetting("Mode", "Simple", "Simple", "Cancel", "Jump reset"));
    private final NumberSetting horizontal = add(new NumberSetting("Horizontal", 0, 0, 100, 1).suffix("%"));
    private final NumberSetting vertical   = add(new NumberSetting("Vertical", 0, 0, 100, 1).suffix("%"));
    private final NumberSetting chance     = add(new NumberSetting("Chance", 100, 1, 100, 1).suffix("%"));

    public Velocity() {
        super("Velocity", "Take less knockback", Category.COMBAT);
        horizontal.visibleWhen(() -> mode.is("Simple"));
        vertical.visibleWhen(() -> mode.is("Simple"));
    }

    @Override public String tag() { return mode.get(); }

    @Subscribe
    public void onPacket(PacketEvent.Receive event) {
        if (nullCheck()) return;
        if (event.type() != PacketType.S_ENTITY_VELOCITY && event.type() != PacketType.S_EXPLOSION) return;
        if (event.type() == PacketType.S_ENTITY_VELOCITY
                && event.packet().getInt("entityId") != player().id()) return;
        if (Math.random() * 100 > chance.get()) return;

        if (mode.is("Cancel")) {
            event.cancel();
            return;
        }
        if (mode.is("Jump reset")) {
            if (player().isOnGround()) player().jump();
            return;
        }

        double h = horizontal.get() / 100.0;
        double v = vertical.get() / 100.0;
        event.packet().setDouble("motionX", event.packet().getDouble("motionX") * h);
        event.packet().setDouble("motionY", event.packet().getDouble("motionY") * v);
        event.packet().setDouble("motionZ", event.packet().getDouble("motionZ") * h);
        if (h == 0 && v == 0) event.cancel();
    }
}
