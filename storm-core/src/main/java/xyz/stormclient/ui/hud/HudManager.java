package xyz.stormclient.ui.hud;

import java.util.ArrayList;
import java.util.List;

import xyz.stormclient.Storm;
import xyz.stormclient.module.Module;
import xyz.stormclient.module.impl.hud.HudModule;

/** Keeps track of every HUD element and of whether the editor currently owns them. */
public final class HudManager {

    private boolean editing;

    public List<HudModule> elements() {
        List<HudModule> out = new ArrayList<HudModule>();
        for (Module module : Storm.get().modules().all()) {
            if (module instanceof HudModule) out.add((HudModule) module);
        }
        return out;
    }

    public List<HudModule> visibleElements() {
        List<HudModule> out = new ArrayList<HudModule>();
        for (HudModule element : elements()) {
            if (element.isEnabled()) out.add(element);
        }
        return out;
    }

    public boolean editing() { return editing; }
    public void setEditing(boolean editing) { this.editing = editing; }

    public void openEditor() {
        Storm.get().openScreen(new HudEditorScreen());
    }

    public void resetPositions() {
        for (HudModule element : elements()) {
            element.setting("X").reset();
            element.setting("Y").reset();
            element.setting("Scale").reset();
        }
    }
}
