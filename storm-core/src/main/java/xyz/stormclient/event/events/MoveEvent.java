package xyz.stormclient.event.events;

import xyz.stormclient.event.Event;

/** Fired before the movement is applied, lets modules rewrite the motion vector. */
public class MoveEvent extends Event {

    private double x, y, z;
    private boolean safeWalk;

    public MoveEvent(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setZ(double z) { this.z = z; }
    public void set(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }

    public boolean safeWalk() { return safeWalk; }
    public void setSafeWalk(boolean safeWalk) { this.safeWalk = safeWalk; }
}
