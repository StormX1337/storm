package xyz.stormclient.module.impl.player;

import xyz.stormclient.bridge.IInventory;
import xyz.stormclient.bridge.IItemStack;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.util.TimerUtil;

/** Sorts the hotbar, keeps blocks and gaps stocked and throws away junk. */
public class InventoryManager extends Module {

    private final NumberSetting  delay    = add(new NumberSetting("Delay", 150, 20, 800, 10).suffix("ms"));
    private final BooleanSetting sortBar  = add(new BooleanSetting("Sort hotbar", true));
    private final BooleanSetting dropJunk = add(new BooleanSetting("Drop junk", true));
    private final BooleanSetting inGuiOnly= add(new BooleanSetting("Only in inventory", false));

    private final TimerUtil timer = new TimerUtil();

    public InventoryManager() {
        super("InvManager", "Keeps your inventory tidy", Category.PLAYER);
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (nullCheck() || !timer.passedAndReset(delay.getLong())) return;
        IInventory inv = player().inventory();
        if (inGuiOnly.get() && !inv.containerOpen()) return;

        if (sortBar.get() && sortHotbar(inv)) return;
        if (dropJunk.get()) dropJunk(inv);
    }

    /** Puts the sword on slot 0, blocks on 1, food on 2, pick on 3. */
    private boolean sortHotbar(IInventory inv) {
        int[] wanted = { 0, 1, 2, 3 };
        for (int hotbar : wanted) {
            int source = findFor(inv, hotbar);
            if (source >= 9 && source < inv.size()) {
                inv.swapToHotbar(source, hotbar);
                return true;
            }
        }
        return false;
    }

    private int findFor(IInventory inv, int hotbarSlot) {
        for (int i = 9; i < inv.size(); i++) {
            IItemStack stack = inv.slot(i);
            if (stack.isEmpty()) continue;
            boolean match;
            switch (hotbarSlot) {
                case 0: match = stack.isWeapon(); break;
                case 1: match = stack.isBlock(); break;
                case 2: match = stack.isFood(); break;
                default: match = stack.isTool();
            }
            if (match && !inv.slot(hotbarSlot).isWeapon()) return i;
        }
        return -1;
    }

    private void dropJunk(IInventory inv) {
        for (int i = 9; i < inv.size(); i++) {
            IItemStack stack = inv.slot(i);
            if (stack.isEmpty()) continue;
            if (stack.isArmor() || stack.isWeapon() || stack.isTool()
                    || stack.isFood() || stack.isPotion() || stack.isBlock()) continue;
            inv.drop(i, true);
            return;
        }
    }
}
