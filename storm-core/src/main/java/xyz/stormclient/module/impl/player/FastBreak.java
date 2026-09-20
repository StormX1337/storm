package xyz.stormclient.module.impl.player;

import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;

/** Speeds up block breaking by scaling the damage the client accumulates. */
public class FastBreak extends Module {

    private final NumberSetting  speed    = add(new NumberSetting("Speed", 1.4, 1.0, 5.0, 0.1).suffix("x"));
    private final BooleanSetting noDelay  = add(new BooleanSetting("No break delay", true));
    private final BooleanSetting instant  = add(new BooleanSetting("Instant on low hardness", false));

    public FastBreak() {
        super("FastBreak", "Break blocks faster", Category.PLAYER);
    }

    @Override public String tag() { return String.format("%.1fx", speed.get()); }

    public float multiplier() { return speed.getFloat(); }
    public boolean noDelay()  { return noDelay.get(); }
    public boolean instant()  { return instant.get(); }
}
