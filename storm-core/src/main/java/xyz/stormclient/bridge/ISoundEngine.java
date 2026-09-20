package xyz.stormclient.bridge;

public interface ISoundEngine {
    /** Plays a UI sound, name is version mapped by the bridge ("click", "pop", "levelup"). */
    void play(String name, float volume, float pitch);
}
