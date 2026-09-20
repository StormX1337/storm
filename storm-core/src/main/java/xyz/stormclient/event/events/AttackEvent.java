package xyz.stormclient.event.events;

import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.event.Event;

public class AttackEvent extends Event {

    private final IEntity target;

    public AttackEvent(IEntity target) { this.target = target; }

    public IEntity target() { return target; }
}
