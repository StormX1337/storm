package xyz.stormclient.bridge;

import java.util.UUID;

/** Read only view on any entity in the world. */
public interface IEntity {

    int    id();
    UUID   uuid();
    String name();
    String displayName();

    double x();
    double y();
    double z();

    double lastX();
    double lastY();
    double lastZ();

    double motionX();
    double motionY();
    double motionZ();

    float yaw();
    float pitch();
    float prevYaw();
    float prevPitch();

    float health();
    float maxHealth();
    float absorption();
    float armorValue();

    int  hurtTime();
    int  ticksExisted();

    float width();
    float height();
    float eyeHeight();

    boolean isPlayer();
    boolean isLiving();
    boolean isLocalPlayer();
    boolean isMonster();
    boolean isAnimal();
    boolean isInvisible();
    boolean isSneaking();
    boolean isSprinting();
    boolean isOnGround();
    boolean isDead();
    boolean isInWater();
    boolean isInLava();
    boolean isBurning();
    boolean isNpc();

    /** Team / scoreboard colour, empty string when unteamed. */
    String teamName();
    int    teamColor();

    IItemStack heldItem();
    IItemStack armorSlot(int slot);   // 0 boots .. 3 helmet

    double distanceTo(IEntity other);
    double distanceTo(double x, double y, double z);

    /** Squared horizontal distance, cheap for sorting. */
    double distanceSqTo(IEntity other);

    double[] boundingBox();           // minX,minY,minZ,maxX,maxY,maxZ

    /** Interpolated position for smooth rendering / rotation. */
    double renderX(float partial);
    double renderY(float partial);
    double renderZ(float partial);
}
