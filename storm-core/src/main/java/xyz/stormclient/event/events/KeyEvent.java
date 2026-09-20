package xyz.stormclient.event.events;

import xyz.stormclient.event.Event;

public class KeyEvent extends Event {

    private final int key;
    private final char typed;

    public KeyEvent(int key, char typed) { this.key = key; this.typed = typed; }

    public int  key()   { return key; }
    public char typed() { return typed; }
}
