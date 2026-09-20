package xyz.stormclient.event.events;

import xyz.stormclient.event.Event;

/** Camera tweaks: fov, hurt cam, view bobbing, hand rendering. */
public class ViewEvent extends Event {

    private float value;
    private final Kind kind;

    public enum Kind { FOV, HURT_CAM, BOB, HAND_SCALE, LIGHT }

    public ViewEvent(Kind kind, float value) { this.kind = kind; this.value = value; }

    public Kind kind()   { return kind; }
    public float value() { return value; }
    public void setValue(float value) { this.value = value; }
}
