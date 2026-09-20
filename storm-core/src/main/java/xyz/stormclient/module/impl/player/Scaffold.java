package xyz.stormclient.module.impl.player;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.IInventory;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.MotionEvent;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.rotation.Rotation;
import xyz.stormclient.rotation.RotationUtil;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.util.TimerUtil;

/** Bridges blocks under your feet while you walk. */
public class Scaffold extends Module {

    private final ModeSetting    mode     = add(new ModeSetting("Mode", "Normal", "Normal", "Telly", "Expand"));
    private final NumberSetting  delay    = add(new NumberSetting("Delay", 60, 0, 300, 5).suffix("ms"));
    private final NumberSetting  expand   = add(new NumberSetting("Expand", 0, 0, 5, 1).suffix("m"));
    private final BooleanSetting rotate   = add(new BooleanSetting("Rotations", true));
    private final BooleanSetting sprint   = add(new BooleanSetting("Keep sprint", false));
    private final BooleanSetting swing    = add(new BooleanSetting("Swing", true));
    private final BooleanSetting autoSwap = add(new BooleanSetting("Auto swap", true));
    private final BooleanSetting safeWalk = add(new BooleanSetting("Safe walk", true));

    private final TimerUtil timer = new TimerUtil();
    private int[] placeTarget;
    private int previousSlot = -1;

    public Scaffold() {
        super("Scaffold", "Places blocks under your feet", Category.PLAYER);
        expand.visibleWhen(() -> mode.is("Expand"));
    }

    @Override public String tag() { return mode.get(); }

    @Override public void onDisable() {
        if (!nullCheck() && previousSlot >= 0) player().inventory().setHeldSlot(previousSlot);
        previousSlot = -1;
        placeTarget = null;
        Storm.get().rotations().clear();
    }

    @Subscribe
    public void onMotion(MotionEvent event) {
        if (nullCheck() || !event.isPre()) return;

        placeTarget = findTarget();
        if (placeTarget == null || !rotate.get()) return;

        Rotation wanted = RotationUtil.toPosition(placeTarget[0] + 0.5, placeTarget[1] + 0.5, placeTarget[2] + 0.5);
        Storm.get().rotations().request(new Rotation(wanted.yaw, Math.max(wanted.pitch, 65F)), 80, 2, 180F, 180F, true);
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (nullCheck() || placeTarget == null) return;
        if (!timer.passedAndReset(delay.getLong())) return;
        if (!sprint.get()) player().setSprinting(false);

        IInventory inv = player().inventory();
        if (autoSwap.get()) {
            int slot = inv.findBlockSlot();
            if (slot < 0) return;
            if (slot != inv.heldSlot()) {
                if (previousSlot < 0) previousSlot = inv.heldSlot();
                inv.setHeldSlot(slot);
            }
        }

        if (swing.get()) player().swingArm();
        player().useItem();
    }

    /** The first free spot below the player, optionally extended forward. */
    private int[] findTarget() {
        double x = player().x();
        double y = player().y() - 1;
        double z = player().z();

        if (mode.is("Expand") && expand.get() > 0) {
            double yaw = Math.toRadians(player().yaw());
            x += -Math.sin(yaw) * expand.get();
            z += Math.cos(yaw) * expand.get();
        }
        if (mode.is("Telly") && !player().isOnGround() && player().motionY() > 0) return null;

        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);
        return world().isReplaceable(bx, by, bz) ? new int[] { bx, by, bz } : null;
    }

    public boolean safeWalk() { return isEnabled() && safeWalk.get(); }
}
