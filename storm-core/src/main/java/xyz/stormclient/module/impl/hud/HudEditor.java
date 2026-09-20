package xyz.stormclient.module.impl.hud;

import xyz.stormclient.Storm;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.util.Keyboard;

/** Opens the drag and drop HUD editor. */
public class HudEditor extends Module {

    public HudEditor() {
        super("HudEditor", "Move and scale your HUD elements", Category.HUD);
        setKeybind(Keyboard.code("H"));
    }

    @Override public void onEnable() {
        Storm.get().hud().openEditor();
        setEnabled(false);
    }
}
