package xyz.stormclient.module.impl.combat;

import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;

/** Grows the hitbox the client uses when raytracing for an attack target. */
public class HitBoxes extends Module {

    private final NumberSetting expand  = add(new NumberSetting("Expand", 0.1, 0.0, 1.0, 0.01).suffix("m"));
    private final BooleanSetting playersOnly = add(new BooleanSetting("Players only", true));
    private final BooleanSetting render  = add(new BooleanSetting("Render", false));

    public HitBoxes() {
        super("HitBoxes", "Slightly larger entity hitboxes", Category.COMBAT);
    }

    @Override public String tag() { return String.format("%.2f", expand.get()); }

    public double expand()      { return expand.get(); }
    public boolean playersOnly(){ return playersOnly.get(); }
    public boolean render()     { return render.get(); }
}
