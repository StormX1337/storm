package xyz.stormclient.event.events;

import xyz.stormclient.event.Event;

public class ModuleEvent extends Event {

    private final Object module;
    private final boolean enabled;

    public ModuleEvent(Object module, boolean enabled) { this.module = module; this.enabled = enabled; }

    public Object module()   { return module; }
    public boolean enabled() { return enabled; }
}
