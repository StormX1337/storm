package xyz.stormclient.event.events;

import xyz.stormclient.event.Event;

public class ChatEvent extends Event {

    private String message;

    public ChatEvent(String message) { this.message = message; }

    public String message() { return message; }
    public void setMessage(String message) { this.message = message; }

    /** The local player is about to send a chat message. */
    public static class Send extends ChatEvent {
        public Send(String message) { super(message); }
    }

    /** A message arrived from the server. */
    public static class Receive extends ChatEvent {
        public Receive(String message) { super(message); }
    }
}
