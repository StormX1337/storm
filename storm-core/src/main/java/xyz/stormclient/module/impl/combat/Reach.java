package xyz.stormclient.module.impl.combat;

import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;

/**
 * Extends the combat reach the client uses when picking an attack target.
 * The bridge reads {@link #reach()} in its entity pick hook.
 */
public class Reach extends Module {

    private final NumberSetting combat = add(new NumberSetting("Combat", 3.3, 3.0, 6.0, 0.05).suffix("m"));
    private final NumberSetting block  = add(new NumberSetting("Blocks", 4.5, 4.5, 6.0, 0.05).suffix("m"));
    private final BooleanSetting onlyHit = add(new BooleanSetting("Only when hitting", true));

    public Reach() {
        super("Reach", "Extends how far you can hit", Category.COMBAT);
    }

    @Override public String tag() { return String.format("%.1f", combat.get()); }

    public double reach()      { return combat.get(); }
    public double blockReach() { return block.get(); }
    public boolean onlyHit()   { return onlyHit.get(); }
}
