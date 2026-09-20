package xyz.stormclient.bridge;

import java.io.File;

/** The root handle on the running game. One implementation per Minecraft version. */
public interface IMinecraft {

    GameVersion version();

    IPlayer player();          // null when not ingame
    IWorld world();            // null when not ingame

    IRenderer renderer();
    IFontRenderer font();
    IFontRenderer font(String name, int size);   // custom Storm font, falls back to vanilla
    IInput input();
    INetwork network();
    IGuiBridge gui();
    IChat chat();
    ISoundEngine sound();

    boolean ingame();
    boolean windowFocused();

    int displayWidth();
    int displayHeight();
    int scaledWidth();
    int scaledHeight();
    double scaleFactor();

    int fps();
    float partialTicks();
    float renderPartialTicks();

    /** Vanilla render tick timer, used by Timer / speed modules. */
    float timerSpeed();
    void  setTimerSpeed(float speed);

    String serverAddress();     // "singleplayer" when local
    String username();
    File   gameDirectory();

    /** Third person / first person switch used by Freecam and camera mods. */
    int  perspective();
    void setPerspective(int mode);

    void shutdownSafely();
}
