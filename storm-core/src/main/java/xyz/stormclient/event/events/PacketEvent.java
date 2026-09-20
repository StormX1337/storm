package xyz.stormclient.event.events;

import xyz.stormclient.bridge.IPacket;
import xyz.stormclient.bridge.PacketType;
import xyz.stormclient.event.Event;

public class PacketEvent extends Event {

    private final IPacket packet;

    public PacketEvent(IPacket packet) { this.packet = packet; }

    public IPacket packet()  { return packet; }
    public PacketType type() { return packet.type(); }
    public boolean is(PacketType type) { return packet.type() == type; }

    /** Outgoing, client to server. */
    public static class Send extends PacketEvent {
        public Send(IPacket packet) { super(packet); }
    }

    /** Incoming, server to client. */
    public static class Receive extends PacketEvent {
        public Receive(IPacket packet) { super(packet); }
    }
}
