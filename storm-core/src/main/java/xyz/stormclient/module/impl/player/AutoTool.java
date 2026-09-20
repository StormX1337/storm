package xyz.stormclient.module.impl.player;

import xyz.stormclient.bridge.IInventory;
import xyz.stormclient.bridge.IItemStack;
import xyz.stormclient.bridge.ItemType;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.AttackEvent;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;

/** Swaps to the best tool for the block you are breaking, and to your best weapon on hit. */
public class AutoTool extends Module {

    private final BooleanSetting weapons = add(new BooleanSetting("Best weapon", true));
    private final BooleanSetting blocks  = add(new BooleanSetting("Best tool", true));
    private final BooleanSetting switchBack = add(new BooleanSetting("Switch back", true));

    private int previousSlot = -1;

    public AutoTool() {
        super("AutoTool", "Always hold the right item", Category.PLAYER);
    }

    @Subscribe
    public void onAttack(AttackEvent event) {
        if (nullCheck() || !weapons.get()) return;
        IInventory inv = player().inventory();

        int best = inv.heldSlot();
        double bestDamage = -1;
        for (int i = 0; i < 9; i++) {
            IItemStack stack = inv.slot(i);
            if (stack.isEmpty() || !stack.isWeapon()) continue;
            if (stack.attackDamage() > bestDamage) {
                bestDamage = stack.attackDamage();
                best = i;
            }
        }
        if (best != inv.heldSlot()) {
            if (previousSlot < 0) previousSlot = inv.heldSlot();
            inv.setHeldSlot(best);
        }
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (nullCheck()) return;
        IInventory inv = player().inventory();

        if (blocks.get() && mc().input().attack()) {
            int[] hit = world().raytraceBlock(5.0);
            if (hit != null) {
                String block = world().blockName(hit[0], hit[1], hit[2]);
                int best = inv.heldSlot();
                double bestSpeed = inv.slot(inv.heldSlot()).miningSpeed(block);
                for (int i = 0; i < 9; i++) {
                    double speed = inv.slot(i).miningSpeed(block);
                    if (speed > bestSpeed) { bestSpeed = speed; best = i; }
                }
                if (best != inv.heldSlot()) {
                    if (previousSlot < 0) previousSlot = inv.heldSlot();
                    inv.setHeldSlot(best);
                }
                return;
            }
        }

        if (switchBack.get() && previousSlot >= 0 && !mc().input().attack()) {
            inv.setHeldSlot(previousSlot);
            previousSlot = -1;
        }
    }

    public static boolean isTool(ItemType type) {
        return type == ItemType.PICKAXE || type == ItemType.AXE || type == ItemType.SHOVEL || type == ItemType.HOE;
    }
}
