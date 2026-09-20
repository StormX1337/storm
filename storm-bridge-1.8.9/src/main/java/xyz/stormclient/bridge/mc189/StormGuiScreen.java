package xyz.stormclient.bridge.mc189;

import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Mouse;

import xyz.stormclient.bridge.IScreen;

/** Hosts a Storm screen inside a vanilla GuiScreen. */
public final class StormGuiScreen extends GuiScreen {

    private final IScreen screen;
    private int lastButton = -1;

    public StormGuiScreen(IScreen screen) { this.screen = screen; }

    public IScreen screen() { return screen; }

    @Override public void initGui() {
        screen.onOpen(width, height);
    }

    @Override public void onGuiClosed() {
        screen.onClose();
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (screen.darkenBackground()) drawDefaultBackground();
        screen.render(mouseX, mouseY, partialTicks);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        lastButton = button;
        screen.mouseDown(mouseX, mouseY, button);
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override protected void mouseReleased(int mouseX, int mouseY, int state) {
        lastButton = -1;
        screen.mouseUp(mouseX, mouseY, state);
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override protected void mouseClickMove(int mouseX, int mouseY, int button, long heldTime) {
        screen.mouseDragged(mouseX, mouseY, button);
    }

    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) screen.mouseScroll(wheel > 0 ? 1 : -1);
        if (lastButton >= 0 && Mouse.isButtonDown(lastButton)) {
            screen.mouseDragged(mouseXScaled(), mouseYScaled(), lastButton);
        }
    }

    private int mouseXScaled() {
        return Mouse.getX() * width / mc.displayWidth;
    }

    private int mouseYScaled() {
        return height - Mouse.getY() * height / mc.displayHeight - 1;
    }

    @Override protected void keyTyped(char typed, int keyCode) throws IOException {
        if (keyCode == 1 && screen.closeOnEscape()) {          // escape
            mc.displayGuiScreen(null);
            return;
        }
        screen.keyDown(keyCode, typed);
    }

    @Override public boolean doesGuiPauseGame() { return screen.pausesGame(); }
}
