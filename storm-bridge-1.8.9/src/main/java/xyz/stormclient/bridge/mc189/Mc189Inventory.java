package xyz.stormclient.bridge.mc189;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C09PacketHeldItemChange;

import xyz.stormclient.bridge.IInventory;
import xyz.stormclient.bridge.IItemStack;
import xyz.stormclient.bridge.ItemType;

public final class Mc189Inventory implements IInventory {

    private final EntityPlayerSP player;

    public Mc189Inventory(EntityPlayerSP player) { this.player = player; }

    private Minecraft mc() { return Minecraft.getMinecraft(); }

    @Override public IItemStack slot(int index) {
        if (index < 0 || index >= player.inventory.mainInventory.length) return Mc189ItemStack.EMPTY;
        return Mc189ItemStack.of(player.inventory.mainInventory[index]);
    }

    @Override public int size() { return player.inventory.mainInventory.length; }

    @Override public IItemStack armor(int slot) {
        if (slot < 0 || slot >= player.inventory.armorInventory.length) return Mc189ItemStack.EMPTY;
        return Mc189ItemStack.of(player.inventory.armorInventory[slot]);
    }

    /** 1.8.9 has no off hand. */
    @Override public IItemStack offhand() { return Mc189ItemStack.EMPTY; }

    @Override public int heldSlot() { return player.inventory.currentItem; }

    @Override public void setHeldSlot(int slot) {
        if (slot < 0 || slot > 8 || slot == player.inventory.currentItem) return;
        player.inventory.currentItem = slot;
        player.sendQueue.addToSendQueue(new C09PacketHeldItemChange(slot));
    }

    @Override public boolean containerOpen() {
        return player.openContainer != null && player.openContainer != player.inventoryContainer;
    }

    @Override public int windowId() {
        return player.openContainer == null ? -1 : player.openContainer.windowId;
    }

    @Override public int containerSize() {
        Container container = player.openContainer;
        if (container == null) return 0;
        if (container instanceof ContainerChest) {
            return ((ContainerChest) container).getLowerChestInventory().getSizeInventory();
        }
        return container.inventorySlots.size();
    }

    @Override public IItemStack containerSlot(int index) {
        Container container = player.openContainer;
        if (container == null || index < 0 || index >= container.inventorySlots.size()) {
            return Mc189ItemStack.EMPTY;
        }
        ItemStack stack = container.getSlot(index).getStack();
        return Mc189ItemStack.of(stack);
    }

    @Override public void click(int windowId, int slot, int button, int mode) {
        mc().playerController.windowClick(windowId, slot, button, mode, player);
    }

    @Override public void swapToHotbar(int slot, int hotbarIndex) {
        // mode 2 is the number key swap, the slot index is the source
        click(player.openContainer == null ? 0 : player.openContainer.windowId, slot, hotbarIndex, 2);
    }

    @Override public void drop(int slot, boolean fullStack) {
        click(player.openContainer == null ? 0 : player.openContainer.windowId, slot,
              fullStack ? 1 : 0, 4);
    }

    @Override public void closeContainer() {
        if (!containerOpen()) return;
        player.sendQueue.addToSendQueue(new C0DPacketCloseWindow(player.openContainer.windowId));
        player.closeScreen();
    }

    @Override public int findSlot(ItemType type, int from, int to) {
        for (int i = Math.max(0, from); i < Math.min(size(), to); i++) {
            if (slot(i).type() == type) return i;
        }
        return -1;
    }

    @Override public int findBlockSlot() {
        int best = -1;
        int bestCount = 0;
        for (int i = 0; i < 9; i++) {
            IItemStack stack = slot(i);
            if (!stack.isBlock()) continue;
            if (stack.count() > bestCount) { bestCount = stack.count(); best = i; }
        }
        return best;
    }
}
