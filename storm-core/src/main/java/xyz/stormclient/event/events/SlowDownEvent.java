package xyz.stormclient.event.events;

import xyz.stormclient.event.Event;

/** Vanilla item use slowdown, NoSlow rewrites the multipliers. */
public class SlowDownEvent extends Event {

    private float forward = 0.2F;
    private float strafe  = 0.2F;

    public float forward() { return forward; }
    public float strafe()  { return strafe; }
    public void setForward(float forward) { this.forward = forward; }
    public void setStrafe(float strafe)   { this.strafe = strafe; }
    public void set(float value) { this.forward = value; this.strafe = value; }
}
