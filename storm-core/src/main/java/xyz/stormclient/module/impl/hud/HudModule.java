package xyz.stormclient.module.impl.hud;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.RenderEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.ui.theme.Theme;

/**
 * A HUD element that also happens to be a module, so it toggles, binds and
 * saves exactly like everything else. Position is stored in fractions of the
 * screen so elements stay put when the window is resized.
 */
public abstract class HudModule extends Module {

    private final NumberSetting posX  = add(new NumberSetting("X", 0.02, 0, 1, 0.0001));
    private final NumberSetting posY  = add(new NumberSetting("Y", 0.02, 0, 1, 0.0001));
    private final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.5, 2.5, 0.05).suffix("x"));

    protected HudModule(String name, String description, double defaultX, double defaultY) {
        super(name, description, Category.HUD);
        posX.visibleWhen(() -> false);
        posY.visibleWhen(() -> false);
        posX.set(defaultX);
        posY.set(defaultY);
    }

    /** Draw at 0,0 - the manager has already translated and scaled for you. */
    public abstract void renderElement(IRenderer r, IFontRenderer font);

    public abstract double width();
    public abstract double height();

    public double scale() { return scale.get(); }

    public double screenX() { return posX.get() * mc().scaledWidth(); }
    public double screenY() { return posY.get() * mc().scaledHeight(); }

    public void setScreenPosition(double x, double y) {
        posX.set(x / Math.max(1, mc().scaledWidth()));
        posY.set(y / Math.max(1, mc().scaledHeight()));
    }

    protected Theme theme() { return Storm.get().theme(); }

    @Subscribe
    public void onRender(RenderEvent.Hud event) {
        if (!xyz.stormclient.bridge.Bridge.installed()) return;
        if (mc().gui().stormScreenOpen() && Storm.get().hud().editing()) return;   // editor draws it instead

        IRenderer r = mc().renderer();
        IFontRenderer font = mc().font(theme().font(), 16);

        r.push();
        r.translate(screenX(), screenY(), 0);
        r.scale(scale(), scale(), 1);
        renderElement(r, font);
        r.pop();
    }
}
