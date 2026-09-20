package xyz.stormclient.event.events;

import xyz.stormclient.event.Event;

/**
 * Fired right before the player's movement packets are built (pre) and right
 * after they were sent (post). Rotations written here are the ones the server
 * sees, which is what every rotation based module hooks into.
 */
public class MotionEvent extends Event {

    private final boolean pre;
    private double x, y, z;
    private float yaw, pitch;
    private boolean onGround;

    public MotionEvent(boolean pre, double x, double y, double z, float yaw, float pitch, boolean onGround) {
        this.pre = pre;
        this.x = x; this.y = y; this.z = z;
        this.yaw = yaw; this.pitch = pitch;
        this.onGround = onGround;
    }

    public boolean isPre()  { return pre; }
    public boolean isPost() { return !pre; }

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public float yaw()   { return yaw; }
    public float pitch() { return pitch; }
    public boolean onGround() { return onGround; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setZ(double z) { this.z = z; }
    public void setYaw(float yaw)     { this.yaw = yaw; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    public void setOnGround(boolean onGround) { this.onGround = onGround; }

    public void setRotation(float yaw, float pitch) { this.yaw = yaw; this.pitch = pitch; }
}
