package xyz.stormclient.bridge;

public interface IInventory {

    /** 0..8 hotbar, 9..35 main, both indexed the vanilla way. */
    IItemStack slot(int index);
    int size();

    IItemStack armor(int slot);     // 0 boots .. 3 helmet
    IItemStack offhand();

    int  heldSlot();
    void setHeldSlot(int slot);

    boolean containerOpen();
    /** Window id of the open container, -1 when only the player inventory is open. */
    int  windowId();
    int  containerSize();
    IItemStack containerSlot(int index);

    void click(int windowId, int slot, int button, int mode);
    void swapToHotbar(int slot, int hotbarIndex);
    void drop(int slot, boolean fullStack);
    void closeContainer();

    /** First slot holding the given item type, or -1. */
    int findSlot(ItemType type, int from, int to);
    int findBlockSlot();
}
