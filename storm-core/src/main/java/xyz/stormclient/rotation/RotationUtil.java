package xyz.stormclient.rotation;

import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.bridge.IPlayer;
import xyz.stormclient.util.MathUtil;
import xyz.stormclient.util.RandomUtil;

public final class RotationUtil {

    private RotationUtil() { }

    public static Rotation toPosition(double x, double y, double z) {
        IPlayer player = Bridge.mc().player();
        if (player == null) return new Rotation(0, 0);

        double dx = x - player.x();
        double dy = y - (player.y() + player.eyeHeight());
        double dz = z - player.z();
        double dist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return new Rotation(yaw, pitch).normalize();
    }

    /** Aims at a point inside the hitbox, biased towards the part that is closest to the crosshair. */
    public static Rotation toEntity(IEntity target, double yOffsetFactor, double randomness) {
        double[] box = target.boundingBox();
        double centerX = (box[0] + box[3]) / 2.0;
        double centerZ = (box[2] + box[5]) / 2.0;
        double height = box[4] - box[1];
        double aimY = box[1] + height * yOffsetFactor;

        if (randomness > 0) {
            double halfWidth = (box[3] - box[0]) / 2.0;
            centerX += RandomUtil.range(-halfWidth * randomness, halfWidth * randomness);
            centerZ += RandomUtil.range(-halfWidth * randomness, halfWidth * randomness);
            aimY    += RandomUtil.range(-height * 0.1 * randomness, height * 0.1 * randomness);
        }
        return toPosition(centerX, aimY, centerZ);
    }

    /** Moves {@code from} towards {@code to} by at most the given per tick step. */
    public static Rotation step(Rotation from, Rotation to, float yawStep, float pitchStep) {
        float dYaw = MathUtil.angleDiff(from.yaw, to.yaw);
        float dPitch = to.pitch - from.pitch;

        float yaw = from.yaw + MathUtil.clamp(dYaw, -yawStep, yawStep);
        float pitch = from.pitch + MathUtil.clamp(dPitch, -pitchStep, pitchStep);
        return new Rotation(yaw, pitch).normalize();
    }

    /**
     * Snaps a rotation onto the grid the vanilla mouse handler would produce for
     * the player's sensitivity, so server side rotations keep the same
     * granularity as real mouse input.
     */
    public static Rotation applyGcd(Rotation rotation, Rotation previous, double sensitivity) {
        double f = sensitivity * 0.6 + 0.2;
        double gcd = f * f * f * 1.2;
        if (gcd <= 0) return rotation;

        float yaw = (float) (previous.yaw + Math.round(MathUtil.angleDiff(previous.yaw, rotation.yaw) / gcd) * gcd);
        float pitch = (float) (previous.pitch + Math.round((rotation.pitch - previous.pitch) / gcd) * gcd);
        return new Rotation(yaw, pitch);
    }

    /** True while the current view is within the given angle of the target rotation. */
    public static boolean facing(Rotation current, Rotation target, float maxAngle) {
        return current.distanceTo(target) <= maxAngle;
    }
}
