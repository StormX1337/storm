package xyz.stormclient.bridge.mc189;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;

import xyz.stormclient.bridge.IItemStack;
import xyz.stormclient.bridge.ItemType;

public final class Mc189ItemStack implements IItemStack {

    public static final Mc189ItemStack EMPTY = new Mc189ItemStack(null);

    private final ItemStack stack;

    private Mc189ItemStack(ItemStack stack) { this.stack = stack; }

    public static IItemStack of(ItemStack stack) {
        return stack == null ? EMPTY : new Mc189ItemStack(stack);
    }

    public ItemStack handle() { return stack; }

    @Override public boolean isEmpty() { return stack == null || stack.stackSize <= 0; }

    @Override public String registryName() {
        if (isEmpty()) return "";
        return String.valueOf(Item.itemRegistry.getNameForObject(stack.getItem()));
    }

    @Override public String displayName() { return isEmpty() ? "" : stack.getDisplayName(); }

    @Override public int count()     { return isEmpty() ? 0 : stack.stackSize; }
    @Override public int damage()    { return isEmpty() ? 0 : stack.getItemDamage(); }
    @Override public int maxDamage() { return isEmpty() ? 0 : stack.getMaxDamage(); }

    @Override public int durabilityPercent() {
        if (isEmpty() || stack.getMaxDamage() <= 0) return 100;
        return (int) ((1.0 - stack.getItemDamage() / (double) stack.getMaxDamage()) * 100);
    }

    @Override public ItemType type() {
        if (isEmpty()) return ItemType.EMPTY;
        Item item = stack.getItem();

        if (item instanceof ItemSword)   return ItemType.SWORD;
        if (item instanceof ItemAxe)     return ItemType.AXE;
        if (item instanceof ItemPickaxe) return ItemType.PICKAXE;
        if (item instanceof ItemSpade)   return ItemType.SHOVEL;
        if (item instanceof ItemHoe)     return ItemType.HOE;
        if (item instanceof ItemBow)     return ItemType.BOW;
        if (item instanceof ItemBucket)  return ItemType.BUCKET;
        if (item instanceof ItemFood)    return goldenApple() ? ItemType.GOLDEN_APPLE : ItemType.FOOD;
        if (item instanceof ItemPotion) {
            return ItemPotion.isSplash(stack.getItemDamage()) ? ItemType.SPLASH_POTION : ItemType.POTION;
        }
        if (item == Item.getItemFromBlock(Block.getBlockById(0))) return ItemType.MISC;
        if (registryName().contains("ender_pearl")) return ItemType.PEARL;
        if (registryName().contains("fishing_rod")) return ItemType.ROD;

        if (item instanceof ItemArmor) {
            switch (((ItemArmor) item).armorType) {
                case 0: return ItemType.HELMET;
                case 1: return ItemType.CHESTPLATE;
                case 2: return ItemType.LEGGINGS;
                default: return ItemType.BOOTS;
            }
        }
        if (item instanceof ItemBlock) return ItemType.BLOCK;
        return ItemType.MISC;
    }

    private boolean goldenApple() {
        return registryName().contains("golden_apple");
    }

    @Override public int enchantment(String name) {
        if (isEmpty()) return 0;
        for (Enchantment enchantment : Enchantment.enchantmentsList) {
            if (enchantment == null) continue;
            if (!enchantment.getName().toLowerCase().endsWith(name.toLowerCase())) continue;
            return EnchantmentHelper.getEnchantmentLevel(enchantment.effectId, stack);
        }
        return 0;
    }

    @Override public double attackDamage() {
        if (isEmpty()) return 0;
        double base = 1;
        Item item = stack.getItem();
        if (item instanceof ItemSword) base = ((ItemSword) item).getDamageVsEntity() + 4;
        else if (item instanceof ItemTool) base = 3;
        else if (item instanceof ItemArmor) base = ((ItemArmor) item).damageReduceAmount;

        return base + enchantment("sharpness") * 1.25;
    }

    @Override public double miningSpeed(String blockName) {
        if (isEmpty()) return 1;
        Block block = Block.getBlockFromName(blockName);
        if (block == null) return 1;

        double speed = stack.getItem().getStrVsBlock(stack, block);
        int efficiency = enchantment("efficiency");
        if (efficiency > 0 && speed > 1) speed += efficiency * efficiency + 1;
        return speed;
    }

    @Override public List<String> lore() {
        List<String> out = new ArrayList<String>();
        if (isEmpty() || !stack.hasTagCompound()) return out;
        try {
            net.minecraft.nbt.NBTTagCompound display = stack.getTagCompound().getCompoundTag("display");
            net.minecraft.nbt.NBTTagList lore = display.getTagList("Lore", 8);
            for (int i = 0; i < lore.tagCount(); i++) out.add(lore.getStringTagAt(i));
        } catch (Exception ignored) { }
        return out;
    }

    @Override public boolean isFood()   { return type() == ItemType.FOOD || type() == ItemType.GOLDEN_APPLE; }
    @Override public boolean isPotion() { return type() == ItemType.POTION || type() == ItemType.SPLASH_POTION; }
    @Override public boolean isBlock()  { return type() == ItemType.BLOCK; }

    @Override public boolean isArmor() {
        ItemType type = type();
        return type == ItemType.HELMET || type == ItemType.CHESTPLATE
                || type == ItemType.LEGGINGS || type == ItemType.BOOTS;
    }

    @Override public boolean isWeapon() {
        ItemType type = type();
        return type == ItemType.SWORD || type == ItemType.AXE || type == ItemType.BOW;
    }

    @Override public boolean isTool() {
        ItemType type = type();
        return type == ItemType.PICKAXE || type == ItemType.AXE
                || type == ItemType.SHOVEL || type == ItemType.HOE;
    }
}
