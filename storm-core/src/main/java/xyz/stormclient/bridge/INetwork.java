package xyz.stormclient.bridge;

public interface INetwork {

    /** Sends a packet through the normal pipeline (Storm's own outgoing event fires). */
    void send(IPacket packet);
    /** Sends without firing Storm's packet event, used to avoid feedback loops. */
    void sendSilent(IPacket packet);

    /** Handles an incoming packet locally without touching the network. */
    void receive(IPacket packet);

    int  ping();
    boolean connected();

    void sendChat(String message);

    IPacket createRotationPacket(float yaw, float pitch, boolean onGround);
    IPacket createPositionPacket(double x, double y, double z, boolean onGround);
    IPacket createFullPacket(double x, double y, double z, float yaw, float pitch, boolean onGround);
    IPacket createOnGroundPacket(boolean onGround);
    IPacket createUseEntityPacket(IEntity target, boolean attack);
    IPacket createSwingPacket();
    IPacket createEntityActionPacket(String action);   // START_SPRINTING, STOP_SNEAKING, ...
}
