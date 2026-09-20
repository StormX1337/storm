package xyz.stormclient.bridge.mc189;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.C0BPacketEntityAction;

import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.bridge.IInventory;
import xyz.stormclient.bridge.IPlayer;

public final class Mc189Player extends Mc189Entity implements IPlayer {

    private final EntityPlayerSP player;
    private final Mc189Inventory inventory;

    private float serverYaw;
    private float serverPitch;

    public Mc189Player(EntityPlayerSP player) {
        super(player);
        this.player = player;
        this.inventory = new Mc189Inventory(player);
        this.serverYaw = player.rotationYaw;
        this.serverPitch = player.rotationPitch;
    }

    @Override public EntityPlayerSP handle() { return player; }

    @Override public void setYaw(float yaw)     { player.rotationYaw = yaw; }
    @Override public void setPitch(float pitch) { player.rotationPitch = pitch; }

    @Override public void setRotation(float yaw, float pitch) {
        player.rotationYaw = yaw;
        player.rotationPitch = pitch;
    }

    @Override public void setServerRotation(float yaw, float pitch) {
        this.serverYaw = yaw;
        this.serverPitch = pitch;
    }

    @Override public float serverYaw()   { return serverYaw; }
    @Override public float serverPitch() { return serverPitch; }

    @Override public void setMotion(double x, double y, double z) {
        player.motionX = x;
        player.motionY = y;
        player.motionZ = z;
    }

    @Override public void setMotionX(double x) { player.motionX = x; }
    @Override public void setMotionY(double y) { player.motionY = y; }
    @Override public void setMotionZ(double z) { player.motionZ = z; }

    @Override public void setPosition(double x, double y, double z) {
        player.setPosition(x, y, z);
    }

    @Override public void setSprinting(boolean sprinting) { player.setSprinting(sprinting); }
    @Override public void setSneaking(boolean sneaking)   { player.setSneaking(sneaking); }
    @Override public void setOnGround(boolean onGround)   { player.onGround = onGround; }

    @Override public void jump()     { player.jump(); }
    @Override public void swingArm() { player.swingItem(); }

    @Override public void attack(IEntity target) {
        if (!(target instanceof Mc189Entity)) return;
        Minecraft.getMinecraft().playerController.attackEntity(player, ((Mc189Entity) target).handle());
    }

    @Override public void useItem() {
        Minecraft mc = Minecraft.getMinecraft();
        if (player.getHeldItem() == null) return;
        mc.playerController.sendUseItem(player, mc.theWorld, player.getHeldItem());
    }

    @Override public void stopUsingItem() { player.stopUsingItem(); }

    @Override public boolean isUsingItem()  { return player.isUsingItem(); }
    @Override public boolean isBlocking()   { return player.isBlocking(); }
    @Override public boolean isCollidedHorizontally() { return player.isCollidedHorizontally; }
    @Override public boolean isCollidedVertically()   { return player.isCollidedVertically; }
    @Override public boolean isOnLadder()   { return player.isOnLadder(); }
    @Override public boolean isInWeb()      { return player.isInWeb; }
    @Override public boolean isCreative()   { return player.capabilities.isCreativeMode; }
    @Override public boolean isSpectator()  { return Minecraft.getMinecraft().playerController.isSpectatorMode(); }
    @Override public boolean canBeHurt()    { return !player.capabilities.disableDamage; }

    @Override public float hunger()         { return player.getFoodStats().getFoodLevel(); }
    @Override public float saturation()     { return player.getFoodStats().getSaturationLevel(); }
    @Override public int experienceLevel()  { return player.experienceLevel; }

    @Override public int jumpTicks()             { return 0; }   // private in 1.8.9, not needed by any module yet
    @Override public void setJumpTicks(int ticks) { }

    @Override public float fallDistance()               { return player.fallDistance; }
    @Override public void setFallDistance(float value)  { player.fallDistance = value; }

    /** 1.8 has no attack cooldown, every hit is at full strength. */
    @Override public float attackStrength() { return 1.0F; }

    @Override public float moveForward() { return player.movementInput.moveForward; }
    @Override public float moveStrafe()  { return player.movementInput.moveStrafe; }

    @Override public void setMoveForward(float forward) { player.movementInput.moveForward = forward; }
    @Override public void setMoveStrafe(float strafe)   { player.movementInput.moveStrafe = strafe; }

    @Override public int gameMode() {
        if (player.capabilities.isCreativeMode) return 1;
        if (isSpectator()) return 3;
        return 0;
    }

    @Override public IInventory inventory() { return inventory; }

    @Override public double reach() {
        return Minecraft.getMinecraft().playerController.getBlockReachDistance();
    }

    /** Used by Sneak and WTap without going through the full packet wrapper. */
    public void sendAction(C0BPacketEntityAction.Action action) {
        player.sendQueue.addToSendQueue(new C0BPacketEntityAction(player, action));
    }
}
