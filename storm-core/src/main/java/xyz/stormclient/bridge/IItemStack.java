package xyz.stormclient.bridge;

import java.util.List;

public interface IItemStack {

    boolean isEmpty();

    String registryName();      // "minecraft:diamond_sword"
    String displayName();

    int count();
    int damage();
    int maxDamage();
    /** 0..100 durability left, 100 when unbreakable. */
    int durabilityPercent();

    ItemType type();

    /** Level of an enchantment by its vanilla name, 0 when absent. */
    int enchantment(String name);

    double attackDamage();
    double miningSpeed(String blockName);

    List<String> lore();
    boolean isFood();
    boolean isPotion();
    boolean isBlock();
    boolean isArmor();
    boolean isWeapon();
    boolean isTool();
}
