package xyz.stormclient.module.impl.player;

import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;

/** Removes the vanilla 4 tick cooldown between right clicks. */
public class FastPlace extends Module {

    private final NumberSetting delay = add(new NumberSetting("Delay", 0, 0, 4, 1).suffix(" ticks"));
    private final BooleanSetting blocksOnly = add(new BooleanSetting("Blocks only", false));

    public FastPlace() {
        super("FastPlace", "Place and use items without the vanilla delay", Category.PLAYER);
    }

    public int delay()       { return delay.getInt(); }
    public boolean blocksOnly() { return blocksOnly.get(); }
}
