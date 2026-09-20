package xyz.stormclient.module.impl.movement;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.StepEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;

/** Walk up blocks taller than half a block without jumping. */
public class Step extends Module {

    private final NumberSetting height = add(new NumberSetting("Height", 1.0, 0.6, 2.5, 0.1).suffix("m"));
    private final BooleanSetting onlyOnGround = add(new BooleanSetting("Only on ground", true));

    public Step() {
        super("Step", "Step up full blocks", Category.MOVEMENT);
    }

    @Override public String tag() { return String.format("%.1f", height.get()); }

    @Subscribe
    public void onStep(StepEvent event) {
        if (nullCheck()) return;
        if (onlyOnGround.get() && !player().isOnGround()) return;
        event.setHeight(height.getFloat());
    }
}
