package xyz.stormclient.module.impl.combat;

import java.util.ArrayList;
import java.util.List;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.bridge.IPlayer;
import xyz.stormclient.bridge.ItemType;
import xyz.stormclient.event.Priority;
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
import xyz.stormclient.target.TargetFilter;
import xyz.stormclient.target.TargetSelector;
import xyz.stormclient.target.TargetSort;
import xyz.stormclient.util.EntityUtil;
import xyz.stormclient.util.MathUtil;
import xyz.stormclient.util.RandomUtil;
import xyz.stormclient.util.TimerUtil;

/**
 * The combat centrepiece. Picks a target, asks the rotation manager to look at
 * it and paces the attacks with a randomised click interval.
 */
public class KillAura extends Module {

    private final ModeSetting   mode       = add(new ModeSetting("Mode", "Single", "Single", "Switch", "Multi"));
    private final NumberSetting range      = add(new NumberSetting("Range", 3.0, 1.0, 6.0, 0.05).suffix("m"));
    private final NumberSetting wallRange  = add(new NumberSetting("Wall range", 3.0, 0.0, 6.0, 0.05).suffix("m"));
    private final NumberSetting fov        = add(new NumberSetting("FOV", 180, 30, 360, 5).suffix("\u00b0"));
    private final NumberSetting minCps     = add(new NumberSetting("Min CPS", 8, 1, 20, 1));
    private final NumberSetting maxCps     = add(new NumberSetting("Max CPS", 12, 1, 20, 1));
    private final ModeSetting   sort       = add(new ModeSetting("Sort", "distance", "distance", "health", "angle", "armor", "hurt time"));
    private final ModeSetting   rotations  = add(new ModeSetting("Rotations", "Silent", "None", "Silent", "Lock", "Smooth"));
    private final NumberSetting rotSpeed   = add(new NumberSetting("Rotation speed", 55, 1, 180, 1)
            .suffix("\u00b0").visibleWhen(() -> true));
    private final NumberSetting randomness = add(new NumberSetting("Randomization", 0.25, 0, 1, 0.05));
    private final ModeSetting   aimPoint   = add(new ModeSetting("Aim point", "Nearest", "Head", "Body", "Feet", "Nearest"));
    private final BooleanSetting rayTrace  = add(new BooleanSetting("Raytrace", false)
            .describe("Only attack when the aimed rotation actually hits the target"));
    private final BooleanSetting keepSprint= add(new BooleanSetting("Keep sprint", true));
    private final BooleanSetting swing     = add(new BooleanSetting("Swing", true));
    private final BooleanSetting weaponOnly= add(new BooleanSetting("Weapon only", false));
    private final NumberSetting maxTargets = add(new NumberSetting("Multi targets", 3, 2, 8, 1)
            .visibleWhen(() -> false));

    private final BooleanSetting players   = add(new BooleanSetting("Players", true));
    private final BooleanSetting mobs      = add(new BooleanSetting("Mobs", false));
    private final BooleanSetting animals   = add(new BooleanSetting("Animals", false));
    private final BooleanSetting invisible = add(new BooleanSetting("Invisible", false));
    private final BooleanSetting teammates = add(new BooleanSetting("Teammates", false));

    private final TimerUtil attackTimer = new TimerUtil();
    private final List<IEntity> targets = new ArrayList<IEntity>();

    private IEntity target;
    private long nextDelay;
    private int switchIndex;

    public KillAura() {
        super("KillAura", "Attacks entities around you", Category.COMBAT);
        maxTargets.visibleWhen(() -> mode.is("Multi"));
        rotSpeed.visibleWhen(() -> !rotations.is("None") && !rotations.is("Lock"));
        wallRange.visibleWhen(() -> true);
    }

    @Override public String tag() { return mode.get(); }

    @Override public void onDisable() {
        target = null;
        targets.clear();
        Storm.get().rotations().clear();
    }

    private TargetFilter filter() {
        TargetFilter f = new TargetFilter();
        f.players = players.get();
        f.mobs = mobs.get();
        f.animals = animals.get();
        f.invisible = invisible.get();
        f.teammates = teammates.get();
        f.range = Math.max(range.get(), wallRange.get());
        f.wallRange = wallRange.get();
        return f;
    }

    /** Target selection runs on the tick so it does not depend on the motion hook. */
    @Subscribe
    public void onTickPre(TickEvent.Pre event) {
        if (nullCheck()) { target = null; return; }
        updateTargets();
    }

    @Subscribe(priority = Priority.HIGH)
    public void onMotion(MotionEvent event) {
        if (nullCheck() || !event.isPre()) return;

        if (target == null) {
            Storm.get().rotations().clear();
            return;
        }
        if (rotations.is("None")) return;

        Rotation wanted = RotationUtil.toEntity(target, aimFactor(), randomness.get());
        float step = rotations.is("Lock") ? 180F : rotSpeed.getFloat();
        boolean silent = !rotations.is("Lock");
        Storm.get().rotations().request(wanted, 100, 2, step, step, silent);
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (nullCheck() || target == null) return;

        if (attackTimer.elapsed() < nextDelay) return;

        if (mode.is("Multi")) {
            int hits = 0;
            for (IEntity e : targets) {
                if (hits >= maxTargets.getInt()) break;
                if (attack(e)) hits++;
            }
        } else {
            attack(target);
        }

        attackTimer.reset();
        nextDelay = clickDelay();
    }

    private boolean attack(IEntity entity) {
        IPlayer self = player();
        if (self == null || entity == null) return false;

        double distance = EntityUtil.hitboxDistance(entity);
        if (distance > range.get()) return false;

        if (weaponOnly.get()) {
            ItemType held = self.inventory().slot(self.inventory().heldSlot()).type();
            if (held != ItemType.SWORD && held != ItemType.AXE) return false;
        }
        if (rayTrace.get() && !facingTarget(entity)) return false;
        if (self.attackStrength() < 0.92F) return false;          // 1.9+ cooldown

        boolean wasSprinting = self.isSprinting();
        if (swing.get()) self.swingArm();
        self.attack(entity);
        if (keepSprint.get() && wasSprinting) self.setSprinting(true);

        Storm.get().onAttack(entity);
        return true;
    }

    private boolean facingTarget(IEntity entity) {
        Rotation current = Storm.get().rotations().active()
                ? Storm.get().rotations().current()
                : new Rotation(player().yaw(), player().pitch());
        Rotation needed = RotationUtil.toEntity(entity, aimFactor(), 0);
        return current.distanceTo(needed) < 10F;
    }

    private void updateTargets() {
        targets.clear();
        List<IEntity> found = TargetSelector.candidates(filter());

        float maxAngle = fov.getFloat() / 2F;
        for (IEntity e : found) {
            if (maxAngle < 180F && TargetSelector.angleTo(e) > maxAngle) continue;
            targets.add(e);
        }
        targets.sort(TargetSelector.comparator(TargetSort.of(sort.get())));

        if (targets.isEmpty()) {
            target = null;
            switchIndex = 0;
            return;
        }

        if (mode.is("Switch")) {
            switchIndex = (switchIndex + 1) % targets.size();
            target = targets.get(switchIndex);
        } else {
            target = targets.get(0);
        }
    }

    private double aimFactor() {
        if (aimPoint.is("Head")) return 0.92;
        if (aimPoint.is("Body")) return 0.5;
        if (aimPoint.is("Feet")) return 0.1;

        // Nearest: aim at the part of the hitbox closest to the current pitch.
        IPlayer self = player();
        if (self == null || target == null) return 0.5;
        double[] box = target.boundingBox();
        double eyeY = self.y() + self.eyeHeight();
        double clamped = MathUtil.clamp(eyeY, box[1] + 0.1, box[4] - 0.1);
        double height = Math.max(0.1, box[4] - box[1]);
        return MathUtil.clamp((clamped - box[1]) / height, 0.05, 0.95);
    }

    private long clickDelay() {
        double min = Math.min(minCps.get(), maxCps.get());
        double max = Math.max(minCps.get(), maxCps.get());
        double cps = RandomUtil.range(min, max);
        double base = 1000.0 / Math.max(1.0, cps);
        return (long) (base + RandomUtil.gaussian(0F, 8F));
    }

    public IEntity target() { return target; }
}
