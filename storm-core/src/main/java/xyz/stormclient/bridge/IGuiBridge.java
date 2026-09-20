package xyz.stormclient.bridge;

public interface IGuiBridge {

    void open(IScreen screen);
    void close();

    boolean anyScreenOpen();
    boolean stormScreenOpen();
    /** Vanilla screen class name, empty when none. */
    String currentScreenName();

    /** Cursor position handling for GUIs that need free mouse movement. */
    void grabMouse(boolean grab);
}
