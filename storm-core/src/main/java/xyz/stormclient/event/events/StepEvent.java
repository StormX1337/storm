package xyz.stormclient.event.events;

import xyz.stormclient.event.Event;

public class StepEvent extends Event {

    private float height;

    public StepEvent(float height) { this.height = height; }

    public float height() { return height; }
    public void setHeight(float height) { this.height = height; }
}
