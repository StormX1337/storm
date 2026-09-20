package xyz.stormclient.bridge.mc189;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;

import xyz.stormclient.bridge.IGuiBridge;
import xyz.stormclient.bridge.IScreen;

public final class Mc189Gui implements IGuiBridge {

    @Override public void open(IScreen screen) {
        Minecraft.getMinecraft().displayGuiScreen(screen == null ? null : new StormGuiScreen(screen));
    }

    @Override public void close() {
        Minecraft.getMinecraft().displayGuiScreen(null);
    }

    @Override public boolean anyScreenOpen() {
        return Minecraft.getMinecraft().currentScreen != null;
    }

    @Override public boolean stormScreenOpen() {
        return Minecraft.getMinecraft().currentScreen instanceof StormGuiScreen;
    }

    @Override public String currentScreenName() {
        return Minecraft.getMinecraft().currentScreen == null
                ? "" : Minecraft.getMinecraft().currentScreen.getClass().getSimpleName();
    }

    @Override public void grabMouse(boolean grab) {
        if (grab) Minecraft.getMinecraft().mouseHelper.grabMouseCursor();
        else Minecraft.getMinecraft().mouseHelper.ungrabMouseCursor();
    }

    public boolean chatOpen() {
        return Minecraft.getMinecraft().currentScreen instanceof GuiChat;
    }
}
