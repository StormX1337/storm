package xyz.stormclient.event.events;

import xyz.stormclient.event.Event;

public class JumpEvent extends Event {

    private float motion;
    private float yaw;

    public JumpEvent(float motion, float yaw) { this.motion = motion; this.yaw = yaw; }

    public float motion() { return motion; }
    public float yaw()    { return yaw; }
    public void setMotion(float motion) { this.motion = motion; }
    public void setYaw(float yaw)       { this.yaw = yaw; }
}
