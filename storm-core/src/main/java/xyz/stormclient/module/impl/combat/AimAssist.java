package xyz.stormclient.module.impl.combat;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.MotionEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.rotation.Rotation;
import xyz.stormclient.rotation.RotationUtil;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.target.TargetFilter;
import xyz.stormclient.target.TargetSelector;
import xyz.stormclient.target.TargetSort;

/** Gently pulls the real view towards the nearest target while you hold attack. */
public class AimAssist extends Module {

    private final NumberSetting range   = add(new NumberSetting("Range", 4.0, 1.0, 8.0, 0.1).suffix("m"));
    private final NumberSetting fov     = add(new NumberSetting("FOV", 60, 5, 180, 5).suffix("\u00b0"));
    private final NumberSetting strength= add(new NumberSetting("Strength", 30, 1, 100, 1).suffix("%"));
    private final BooleanSetting pitch  = add(new BooleanSetting("Vertical", false));
    private final BooleanSetting onlyWhenClicking = add(new BooleanSetting("Only while clicking", true));

    public AimAssist() {
        super("AimAssist", "Softly guides your aim towards targets", Category.COMBAT);
    }

    @Subscribe
    public void onMotion(MotionEvent event) {
        if (nullCheck() || !event.isPre()) return;
        if (onlyWhenClicking.get() && !mc().input().attack()) return;

        TargetFilter filter = new TargetFilter().range(range.get());
        IEntity target = TargetSelector.best(filter, TargetSort.ANGLE);
        if (target == null) return;
        if (TargetSelector.angleTo(target) > fov.get() / 2F) return;

        Rotation wanted = RotationUtil.toEntity(target, 0.7, 0.1);
        float factor = strength.getFloat() / 100F;
        float step = 1F + 18F * factor;

        Rotation now = new Rotation(player().yaw(), player().pitch());
        Rotation next = RotationUtil.step(now, wanted, step, pitch.get() ? step : 0F);
        player().setRotation(next.yaw, pitch.get() ? next.pitch : player().pitch());
        Storm.get().rotations().clear();
    }
}
