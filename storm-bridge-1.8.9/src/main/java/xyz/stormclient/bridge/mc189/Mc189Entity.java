package xyz.stormclient.bridge.mc189;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;

import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.bridge.IItemStack;

public class Mc189Entity implements IEntity {

    protected final Entity entity;

    public Mc189Entity(Entity entity) { this.entity = entity; }

    public Entity handle() { return entity; }

    private EntityLivingBase living() {
        return entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
    }

    @Override public int id()        { return entity.getEntityId(); }
    @Override public UUID uuid()     { return entity.getUniqueID(); }
    @Override public String name()   { return entity.getName(); }

    @Override public String displayName() {
        return entity.getDisplayName() == null ? name() : entity.getDisplayName().getFormattedText();
    }

    @Override public double x() { return entity.posX; }
    @Override public double y() { return entity.posY; }
    @Override public double z() { return entity.posZ; }

    @Override public double lastX() { return entity.lastTickPosX; }
    @Override public double lastY() { return entity.lastTickPosY; }
    @Override public double lastZ() { return entity.lastTickPosZ; }

    @Override public double motionX() { return entity.motionX; }
    @Override public double motionY() { return entity.motionY; }
    @Override public double motionZ() { return entity.motionZ; }

    @Override public float yaw()       { return entity.rotationYaw; }
    @Override public float pitch()     { return entity.rotationPitch; }
    @Override public float prevYaw()   { return entity.prevRotationYaw; }
    @Override public float prevPitch() { return entity.prevRotationPitch; }

    @Override public float health()    { return living() == null ? 0 : living().getHealth(); }
    @Override public float maxHealth() { return living() == null ? 0 : living().getMaxHealth(); }
    @Override public float absorption(){ return living() == null ? 0 : living().getAbsorptionAmount(); }

    @Override public float armorValue() {
        return living() == null ? 0 : living().getTotalArmorValue();
    }

    @Override public int hurtTime()    { return living() == null ? 0 : living().hurtTime; }
    @Override public int ticksExisted(){ return entity.ticksExisted; }

    @Override public float width()     { return entity.width; }
    @Override public float height()    { return entity.height; }
    @Override public float eyeHeight() { return entity.getEyeHeight(); }

    @Override public boolean isPlayer()      { return entity instanceof EntityPlayer; }
    @Override public boolean isLiving()      { return entity instanceof EntityLivingBase; }
    @Override public boolean isLocalPlayer() { return entity == Minecraft.getMinecraft().thePlayer; }
    @Override public boolean isMonster()     { return entity instanceof IMob; }
    @Override public boolean isAnimal()      { return entity instanceof EntityAnimal; }
    @Override public boolean isInvisible()   { return entity.isInvisible(); }
    @Override public boolean isSneaking()    { return entity.isSneaking(); }
    @Override public boolean isSprinting()   { return entity.isSprinting(); }
    @Override public boolean isOnGround()    { return entity.onGround; }
    @Override public boolean isDead()        { return entity.isDead || (living() != null && living().getHealth() <= 0); }
    @Override public boolean isInWater()     { return entity.isInWater(); }
    @Override public boolean isInLava()      { return entity.isInLava(); }
    @Override public boolean isBurning()     { return entity.isBurning(); }

    /** Entities without an AI and without a valid tab entry are usually NPCs. */
    @Override public boolean isNpc() {
        if (!isPlayer()) return false;
        return Minecraft.getMinecraft().getNetHandler() != null
                && Minecraft.getMinecraft().getNetHandler().getPlayerInfo(entity.getUniqueID()) == null;
    }

    /** 1.8.9 has no Entity#getTeam, the scoreboard is the way in. */
    private ScorePlayerTeam team() {
        if (entity.worldObj == null || entity.worldObj.getScoreboard() == null) return null;
        return entity.worldObj.getScoreboard().getPlayersTeam(entity.getName());
    }

    @Override public String teamName() {
        ScorePlayerTeam team = team();
        return team == null ? "" : team.getRegisteredName();
    }

    @Override public int teamColor() {
        ScorePlayerTeam team = team();
        if (team == null) return 0;
        String prefix = team.getColorPrefix();
        for (EnumChatFormatting format : EnumChatFormatting.values()) {
            if (!format.isColor()) continue;
            if (prefix.contains(format.toString())) return Mc189Colors.of(format);
        }
        return 0;
    }

    @Override public IItemStack heldItem() {
        return living() == null ? Mc189ItemStack.EMPTY : Mc189ItemStack.of(living().getHeldItem());
    }

    @Override public IItemStack armorSlot(int slot) {
        if (entity instanceof EntityPlayer) {
            return Mc189ItemStack.of(((EntityPlayer) entity).inventory.armorInventory[slot]);
        }
        return living() == null ? Mc189ItemStack.EMPTY : Mc189ItemStack.of(living().getEquipmentInSlot(slot + 1));
    }

    @Override public double distanceTo(IEntity other) {
        return distanceTo(other.x(), other.y(), other.z());
    }

    @Override public double distanceTo(double x, double y, double z) {
        double dx = entity.posX - x, dy = entity.posY - y, dz = entity.posZ - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override public double distanceSqTo(IEntity other) {
        double dx = entity.posX - other.x(), dz = entity.posZ - other.z();
        return dx * dx + dz * dz;
    }

    @Override public double[] boundingBox() {
        AxisAlignedBB box = entity.getEntityBoundingBox();
        return new double[] { box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ };
    }

    @Override public double renderX(float partial) {
        return entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partial;
    }

    @Override public double renderY(float partial) {
        return entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partial;
    }

    @Override public double renderZ(float partial) {
        return entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partial;
    }
}
