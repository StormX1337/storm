package xyz.stormclient.event.events;

import xyz.stormclient.event.Event;

/** Fired once per client tick. */
public class TickEvent extends Event {
    public static class Pre  extends TickEvent { }
    public static class Post extends TickEvent { }
}
