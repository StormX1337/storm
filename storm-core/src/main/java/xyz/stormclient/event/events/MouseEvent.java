package xyz.stormclient.event.events;

import xyz.stormclient.event.Event;

public class MouseEvent extends Event {

    private final int button;
    private final boolean pressed;
    private final int scroll;

    public MouseEvent(int button, boolean pressed, int scroll) {
        this.button = button; this.pressed = pressed; this.scroll = scroll;
    }

    public int  button()  { return button; }
    public boolean down() { return pressed; }
    public int  scroll()  { return scroll; }
}
