package xyz.stormclient.module.impl.combat;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.IPlayer;
import xyz.stormclient.bridge.ItemType;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ModeSetting;

/** Legacy 1.8 sword blocking between hits. Does nothing on 1.9+ where blocking is a shield. */
public class AutoBlock extends Module {

    private final ModeSetting   mode    = add(new ModeSetting("Mode", "Vanilla", "Vanilla", "Interact", "Fake"));
    private final BooleanSetting onlyAura = add(new BooleanSetting("Only with KillAura", true));
    private final BooleanSetting swordOnly = add(new BooleanSetting("Sword only", true));

    private boolean blocking;

    public AutoBlock() {
        super("AutoBlock", "Blocks with your sword between hits", Category.COMBAT);
    }

    @Override public String tag() { return mode.get(); }

    @Override public void onDisable() { release(); }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (nullCheck()) return;
        if (!mc().version().isLegacyCombat()) return;

        IPlayer self = player();
        KillAura aura = Storm.get().modules().get(KillAura.class);
        boolean hasTarget = aura != null && aura.isEnabled() && aura.target() != null;

        if (onlyAura.get() && !hasTarget) { release(); return; }
        if (swordOnly.get() && self.inventory().slot(self.inventory().heldSlot()).type() != ItemType.SWORD) {
            release();
            return;
        }

        if (mode.is("Fake")) {          // client side only, purely visual
            blocking = true;
            return;
        }
        if (!blocking) {
            self.useItem();
            blocking = true;
        }
    }

    private void release() {
        if (!blocking) return;
        blocking = false;
        if (!nullCheck()) player().stopUsingItem();
    }

    public boolean blocking() { return blocking; }
}
