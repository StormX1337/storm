package xyz.stormclient.module.impl.player;

import xyz.stormclient.bridge.IInventory;
import xyz.stormclient.bridge.IItemStack;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.util.RandomUtil;
import xyz.stormclient.util.TimerUtil;

/** Empties open containers one item at a time with a human looking delay. */
public class ChestStealer extends Module {

    private final NumberSetting  delay      = add(new NumberSetting("Delay", 110, 0, 500, 10).suffix("ms"));
    private final NumberSetting  jitter     = add(new NumberSetting("Jitter", 40, 0, 200, 5).suffix("ms"));
    private final BooleanSetting close      = add(new BooleanSetting("Close when done", true));
    private final BooleanSetting onlyUseful = add(new BooleanSetting("Only useful items", false));

    private final TimerUtil timer = new TimerUtil();
    private long next;

    public ChestStealer() {
        super("ChestStealer", "Takes everything out of open chests", Category.PLAYER);
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (nullCheck()) return;
        IInventory inv = player().inventory();
        if (!inv.containerOpen() || inv.windowId() < 0) return;
        if (timer.elapsed() < next) return;

        int size = inv.containerSize();
        for (int i = 0; i < size; i++) {
            IItemStack stack = inv.containerSlot(i);
            if (stack == null || stack.isEmpty()) continue;
            if (onlyUseful.get() && !useful(stack)) continue;

            inv.click(inv.windowId(), i, 0, 1);      // shift click
            timer.reset();
            next = (long) (delay.get() + RandomUtil.range(-jitter.get(), jitter.get()));
            return;
        }

        if (close.get()) inv.closeContainer();
    }

    private boolean useful(IItemStack stack) {
        return stack.isArmor() || stack.isWeapon() || stack.isTool()
                || stack.isFood() || stack.isPotion() || stack.isBlock();
    }
}
