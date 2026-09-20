package xyz.stormclient.bridge;

/** A Storm owned GUI. The bridge wraps it in whatever the version calls a screen. */
public interface IScreen {

    void onOpen(int width, int height);
    void onClose();

    void render(int mouseX, int mouseY, float partialTicks);

    void mouseDown(int mouseX, int mouseY, int button);
    void mouseUp(int mouseX, int mouseY, int button);
    void mouseDragged(int mouseX, int mouseY, int button);
    void mouseScroll(int amount);

    void keyDown(int keyCode, char typed);

    boolean pausesGame();
    boolean closeOnEscape();
    /** Dim the world behind the GUI. */
    boolean darkenBackground();
}
