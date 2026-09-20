package xyz.stormclient.module.impl.combat;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.target.TargetFilter;
import xyz.stormclient.target.TargetSelector;
import xyz.stormclient.util.RandomUtil;
import xyz.stormclient.util.TimerUtil;

/** Attacks whatever the crosshair is already on. No rotations at all. */
public class TriggerBot extends Module {

    private final NumberSetting delay   = add(new NumberSetting("Delay", 90, 0, 500, 5).suffix("ms"));
    private final NumberSetting jitter  = add(new NumberSetting("Jitter", 25, 0, 200, 5).suffix("ms"));
    private final NumberSetting range   = add(new NumberSetting("Range", 3.0, 1.0, 6.0, 0.05).suffix("m"));
    private final BooleanSetting mobs   = add(new BooleanSetting("Mobs", false));
    private final BooleanSetting teammates = add(new BooleanSetting("Teammates", false));

    private final TimerUtil timer = new TimerUtil();
    private long next;

    public TriggerBot() {
        super("TriggerBot", "Hits whatever you are already looking at", Category.COMBAT);
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (nullCheck() || timer.elapsed() < next) return;

        TargetFilter filter = new TargetFilter();
        filter.mobs = mobs.get();
        filter.teammates = teammates.get();
        filter.range = range.get();

        IEntity hovered = null;
        float best = 4F;
        for (IEntity e : TargetSelector.candidates(filter)) {
            float angle = TargetSelector.angleTo(e);
            if (angle < best) { best = angle; hovered = e; }
        }
        if (hovered == null) return;

        player().swingArm();
        player().attack(hovered);
        Storm.get().onAttack(hovered);

        timer.reset();
        next = (long) (delay.get() + RandomUtil.range(-jitter.get(), jitter.get()));
    }
}
