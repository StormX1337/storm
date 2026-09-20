package xyz.stormclient.module.impl.world;

import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.StringSetting;

/** Fills sign edit screens automatically. */
public class AutoSign extends Module {

    private final StringSetting  line1 = add(new StringSetting("Line 1", "Storm"));
    private final StringSetting  line2 = add(new StringSetting("Line 2", ""));
    private final StringSetting  line3 = add(new StringSetting("Line 3", ""));
    private final StringSetting  line4 = add(new StringSetting("Line 4", ""));
    private final BooleanSetting instant = add(new BooleanSetting("Close instantly", true));

    public AutoSign() {
        super("AutoSign", "Fills signs for you", Category.WORLD);
    }

    public String[] lines()  { return new String[] { line1.get(), line2.get(), line3.get(), line4.get() }; }
    public boolean instant() { return instant.get(); }
}
