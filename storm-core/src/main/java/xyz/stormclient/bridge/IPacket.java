package xyz.stormclient.bridge;

/**
 * Version neutral packet handle. Modules never touch a net.minecraft class,
 * they ask for the type and read or write named fields.
 */
public interface IPacket {

    PacketType type();
    /** Raw vanilla class name, useful for logging and for niche checks. */
    String rawName();

    double getDouble(String field);
    float  getFloat(String field);
    int    getInt(String field);
    boolean getBoolean(String field);
    String getString(String field);
    Object getRaw();

    void setDouble(String field, double value);
    void setFloat(String field, float value);
    void setInt(String field, int value);
    void setBoolean(String field, boolean value);
}
