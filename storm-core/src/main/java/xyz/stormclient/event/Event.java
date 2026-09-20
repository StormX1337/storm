package xyz.stormclient.event;

/** Base of every Storm event. */
public class Event {

    private boolean cancelled;

    public boolean isCancelled()           { return cancelled; }
    public void    setCancelled(boolean c) { this.cancelled = c; }
    public void    cancel()                { this.cancelled = true; }

    /** Override and return false for events that must always run to completion. */
    public boolean cancellable() { return true; }
}
