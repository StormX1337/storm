package xyz.stormclient.event.events;

import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.event.Event;

/** Fired around the model pass of a single entity, used by Chams and Nametags. */
public class RenderEntityEvent extends Event {

    private final IEntity entity;
    private final float partialTicks;

    public RenderEntityEvent(IEntity entity, float partialTicks) {
        this.entity = entity; this.partialTicks = partialTicks;
    }

    public IEntity entity()     { return entity; }
    public float partialTicks() { return partialTicks; }

    public static class Pre  extends RenderEntityEvent {
        public Pre(IEntity e, float p) { super(e, p); }
    }
    public static class Post extends RenderEntityEvent {
        public Post(IEntity e, float p) { super(e, p); }
    }
    public static class Nametag extends RenderEntityEvent {
        public Nametag(IEntity e, float p) { super(e, p); }
    }
}
