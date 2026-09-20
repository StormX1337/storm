package xyz.stormclient.event.events;

import xyz.stormclient.event.Event;

public class RenderEvent extends Event {

    private final float partialTicks;

    public RenderEvent(float partialTicks) { this.partialTicks = partialTicks; }

    public float partialTicks() { return partialTicks; }

    /** Overlay pass in scaled GUI space. */
    public static class Hud extends RenderEvent {
        public Hud(float partialTicks) { super(partialTicks); }
    }

    /** World pass, camera already translated to the render origin. */
    public static class World extends RenderEvent {
        public World(float partialTicks) { super(partialTicks); }
    }
}
