package xyz.stormclient.event.events;

import xyz.stormclient.event.Event;

/** Air movement hook, used by the custom movement modes. */
public class StrafeEvent extends Event {

    private float friction;
    private float yaw;
    private float forward;
    private float strafe;

    public StrafeEvent(float friction, float yaw, float forward, float strafe) {
        this.friction = friction; this.yaw = yaw; this.forward = forward; this.strafe = strafe;
    }

    public float friction() { return friction; }
    public float yaw()      { return yaw; }
    public float forward()  { return forward; }
    public float strafe()   { return strafe; }

    public void setFriction(float friction) { this.friction = friction; }
    public void setYaw(float yaw)           { this.yaw = yaw; }
    public void setForward(float forward)   { this.forward = forward; }
    public void setStrafe(float strafe)     { this.strafe = strafe; }
}
