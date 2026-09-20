package xyz.stormclient.bridge;

/** The local player. Everything Storm is allowed to write to lives here. */
public interface IPlayer extends IEntity {

    void setYaw(float yaw);
    void setPitch(float pitch);
    void setRotation(float yaw, float pitch);

    /** Rotations that are only sent to the server, the camera keeps the real ones. */
    void setServerRotation(float yaw, float pitch);
    float serverYaw();
    float serverPitch();

    void setMotion(double x, double y, double z);
    void setMotionX(double x);
    void setMotionY(double y);
    void setMotionZ(double z);

    void setPosition(double x, double y, double z);

    void setSprinting(boolean sprinting);
    void setSneaking(boolean sneaking);
    void setOnGround(boolean onGround);

    void jump();
    void swingArm();
    void attack(IEntity target);
    void useItem();
    void stopUsingItem();

    boolean isUsingItem();
    boolean isBlocking();
    boolean isCollidedHorizontally();
    boolean isCollidedVertically();
    boolean isOnLadder();
    boolean isInWeb();
    boolean isCreative();
    boolean isSpectator();
    boolean canBeHurt();

    float  hunger();
    float  saturation();
    int    experienceLevel();
    int    jumpTicks();
    void   setJumpTicks(int ticks);

    float  fallDistance();
    void   setFallDistance(float distance);

    /** 1.9+ attack cooldown, always 1.0 on legacy versions. */
    float  attackStrength();

    float  moveForward();
    float  moveStrafe();
    void   setMoveForward(float forward);
    void   setMoveStrafe(float strafe);

    /** 0 = survival, 1 = creative, 2 = adventure, 3 = spectator */
    int gameMode();

    IInventory inventory();

    /** Server side reach in blocks for the current version/gamemode. */
    double reach();
}
