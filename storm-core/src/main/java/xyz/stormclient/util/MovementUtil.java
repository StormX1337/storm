package xyz.stormclient.util;

import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.bridge.IPlayer;

public final class MovementUtil {

    public static final double WALK_SPEED   = 0.2873;
    public static final double SPRINT_SPEED = 0.2873 * 1.3;

    private MovementUtil() { }

    public static double horizontalSpeed() {
        IPlayer p = Bridge.mc().player();
        if (p == null) return 0;
        return Math.sqrt(p.motionX() * p.motionX() + p.motionZ() * p.motionZ());
    }

    /** Blocks per second, the number the speed HUD element shows. */
    public static double blocksPerSecond() {
        return horizontalSpeed() * 20.0;
    }

    public static boolean isMoving() {
        IPlayer p = Bridge.mc().player();
        return p != null && (Math.abs(p.moveForward()) > 0.005F || Math.abs(p.moveStrafe()) > 0.005F);
    }

    /** Yaw the player is actually travelling at, including strafe. */
    public static float movementYaw() {
        IPlayer p = Bridge.mc().player();
        if (p == null) return 0;
        float yaw = p.yaw();
        float forward = p.moveForward();
        float strafe = p.moveStrafe();

        if (forward < 0) yaw += 180;
        float strafeOffset = forward < 0 ? -45 : (forward > 0 ? 45 : 90);
        if (strafe > 0)      yaw -= strafeOffset;
        else if (strafe < 0) yaw += strafeOffset;
        return yaw;
    }

    /** Applies a speed to the motion vector along the current movement direction. */
    public static void setSpeed(double speed) {
        IPlayer p = Bridge.mc().player();
        if (p == null) return;
        if (!isMoving()) { p.setMotionX(0); p.setMotionZ(0); return; }
        double yaw = Math.toRadians(movementYaw());
        p.setMotionX(-Math.sin(yaw) * speed);
        p.setMotionZ(Math.cos(yaw) * speed);
    }

    /** Vanilla base speed including the speed potion effect. */
    public static double baseSpeed(boolean sprinting, int speedAmplifier) {
        double base = sprinting ? SPRINT_SPEED : WALK_SPEED;
        if (speedAmplifier > 0) base *= 1.0 + 0.2 * speedAmplifier;
        return base;
    }
}
