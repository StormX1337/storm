package xyz.stormclient.rotation;

import xyz.stormclient.util.MathUtil;

public final class Rotation {

    public final float yaw;
    public final float pitch;

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = MathUtil.clamp(pitch, -90F, 90F);
    }

    public Rotation normalize() {
        return new Rotation(MathUtil.wrapDegrees(yaw), pitch);
    }

    public float distanceTo(Rotation other) {
        float dy = Math.abs(MathUtil.angleDiff(yaw, other.yaw));
        float dp = Math.abs(other.pitch - pitch);
        return Math.max(dy, dp);
    }

    @Override public String toString() {
        return String.format("yaw %.1f pitch %.1f", yaw, pitch);
    }
}
