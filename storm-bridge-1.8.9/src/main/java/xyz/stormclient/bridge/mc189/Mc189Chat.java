package xyz.stormclient.bridge.mc189;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.util.ChatComponentText;

import xyz.stormclient.StormInfo;
import xyz.stormclient.bridge.IChat;

public final class Mc189Chat implements IChat {

    @Override public void print(String message) {
        printRaw(StormInfo.CHAT_PREFIX + message);
    }

    @Override public void printRaw(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            System.out.println("[Storm] " + message);
            return;
        }
        mc.thePlayer.addChatMessage(new ChatComponentText(message));
    }

    @Override public void printWithId(String message, int id) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.ingameGUI == null) { printRaw(message); return; }
        mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(
                new ChatComponentText(StormInfo.CHAT_PREFIX + message), id);
    }

    @Override public boolean isOpen() {
        return Minecraft.getMinecraft().currentScreen instanceof GuiChat;
    }

    @Override public void open(String prefill) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiChat(prefill == null ? "" : prefill));
    }
}
