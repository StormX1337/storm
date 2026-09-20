package xyz.stormclient.event.events;

import xyz.stormclient.event.Event;

public class WorldEvent extends Event {

    /** A new world was loaded, or the player left one (world may be null). */
    public static class Load extends WorldEvent { }

    /** Joined a server, address is available through the bridge. */
    public static class Join extends WorldEvent { }

    public static class Leave extends WorldEvent { }

    public static class Respawn extends WorldEvent { }
}
