package xyz.stormclient.module.impl.render;

import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ColorSetting;
import xyz.stormclient.setting.ModeSetting;

/**
 * Renders entity models through walls. The bridge reads these values in its
 * entity render hook because the depth state has to be changed around the model.
 */
public class Chams extends Module {

    private final ModeSetting    mode    = add(new ModeSetting("Mode", "Color", "Color", "Texture", "Wireframe"));
    private final ColorSetting   visible = add(new ColorSetting("Visible", 0x9935C4FF));
    private final ColorSetting   hidden  = add(new ColorSetting("Through walls", 0x99FF4E6E));
    private final BooleanSetting players = add(new BooleanSetting("Players", true));
    private final BooleanSetting mobs    = add(new BooleanSetting("Mobs", false));
    private final BooleanSetting items   = add(new BooleanSetting("Held items", false));

    public Chams() {
        super("Chams", "See entity models through walls", Category.RENDER);
    }

    @Override public String tag() { return mode.get(); }

    public String  mode()    { return mode.get(); }
    public int     visibleColor(){ return visible.rgb(); }
    public int     wallColor(){ return hidden.rgb(); }
    public boolean players() { return players.get(); }
    public boolean mobs()    { return mobs.get(); }
    public boolean items()   { return items.get(); }
}
