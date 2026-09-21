package xyz.stormclient.ui.click;

import xyz.stormclient.ui.UiScale;
import xyz.stormclient.Storm;
import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.setting.Setting;
import xyz.stormclient.ui.theme.Theme;
import xyz.stormclient.util.MathUtil;

/** One row inside an expanded module in the click GUI. */
public abstract class Component {

    protected final Setting<?> setting;
    protected double x, y, width;

    protected Component(Setting<?> setting) { this.setting = setting; }

    public Setting<?> setting() { return setting; }

    public void position(double x, double y, double width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public abstract double height();
    public abstract void render(int mouseX, int mouseY);

    public void mouseDown(int mouseX, int mouseY, int button) { }
    public void mouseUp(int mouseX, int mouseY, int button)   { }
    public void mouseDragged(int mouseX, int mouseY)          { }
    public void keyDown(int key, char typed)                  { }

    protected boolean hovered(int mouseX, int mouseY) {
        return MathUtil.inside(mouseX, mouseY, x, y, width, height());
    }

    protected IRenderer r()      { return Bridge.mc().renderer(); }
    protected Theme theme()      { return Storm.get().theme(); }
    protected IFontRenderer font(){ return Bridge.mc().font(theme().font(), UiScale.COMPONENT_FONT); }
}
