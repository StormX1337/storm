package xyz.stormclient.ui;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IMinecraft;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.bridge.IScreen;
import xyz.stormclient.ui.theme.Theme;

/** Base for every Storm owned GUI, with the boring parts filled in. */
public abstract class Screen implements IScreen {

    protected int width;
    protected int height;

    @Override public void onOpen(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override public void onClose() { }

    @Override public void mouseDown(int mouseX, int mouseY, int button) { }
    @Override public void mouseUp(int mouseX, int mouseY, int button) { }
    @Override public void mouseDragged(int mouseX, int mouseY, int button) { }
    @Override public void mouseScroll(int amount) { }
    @Override public void keyDown(int keyCode, char typed) { }

    @Override public boolean pausesGame()       { return false; }
    @Override public boolean closeOnEscape()    { return true; }
    @Override public boolean darkenBackground() { return true; }

    protected IMinecraft mc()  { return Bridge.mc(); }
    protected IRenderer r()    { return Bridge.mc().renderer(); }
    protected Theme theme()    { return Storm.get().theme(); }
    protected IFontRenderer font()          { return Bridge.mc().font(theme().font(), theme().fontSize()); }
    protected IFontRenderer font(int size)  { return Bridge.mc().font(theme().font(), size); }

    protected void close() { Bridge.mc().gui().close(); }
}
