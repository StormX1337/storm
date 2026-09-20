package xyz.stormclient.module.impl.player;

import xyz.stormclient.bridge.IInventory;
import xyz.stormclient.bridge.IItemStack;
import xyz.stormclient.bridge.ItemType;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.util.TimerUtil;

/** Equips the strongest armour it can find in your inventory. */
public class AutoArmor extends Module {

    private final NumberSetting  delay  = add(new NumberSetting("Delay", 180, 30, 1000, 10).suffix("ms"));
    private final BooleanSetting inGui  = add(new BooleanSetting("In containers", false));

    private final TimerUtil timer = new TimerUtil();

    public AutoArmor() {
        super("AutoArmor", "Wears the best armour you carry", Category.PLAYER);
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (nullCheck() || !timer.passedAndReset(delay.getLong())) return;
        IInventory inv = player().inventory();
        if (!inGui.get() && inv.containerOpen()) return;

        for (int slot = 0; slot < 4; slot++) {
            ItemType wanted = slotType(slot);
            IItemStack worn = inv.armor(slot);
            double bestScore = worn == null || worn.isEmpty() ? -1 : score(worn);
            int bestIndex = -1;

            for (int i = 0; i < inv.size(); i++) {
                IItemStack stack = inv.slot(i);
                if (stack.isEmpty() || stack.type() != wanted) continue;
                double s = score(stack);
                if (s > bestScore) { bestScore = s; bestIndex = i; }
            }
            if (bestIndex >= 0) {
                inv.click(0, bestIndex, 0, 1);
                return;
            }
        }
    }

    private ItemType slotType(int slot) {
        switch (slot) {
            case 0:  return ItemType.BOOTS;
            case 1:  return ItemType.LEGGINGS;
            case 2:  return ItemType.CHESTPLATE;
            default: return ItemType.HELMET;
        }
    }

    private double score(IItemStack stack) {
        return stack.attackDamage()
                + stack.enchantment("protection") * 2.0
                + stack.enchantment("blast_protection") * 0.5
                + stack.durabilityPercent() * 0.01;
    }
}
