package xyz.stormclient.bridge;

public interface IInput {

    boolean keyDown(int key);
    boolean mouseDown(int button);

    int mouseX();
    int mouseY();

    /** Vanilla keybind states, so modules do not have to know the user's layout. */
    boolean forward();
    boolean back();
    boolean left();
    boolean right();
    boolean jump();
    boolean sneak();
    boolean sprint();
    boolean attack();
    boolean use();

    void setKeyState(String bind, boolean pressed);   // "forward", "jump", "sneak", ...

    /** True while any movement key is held. */
    boolean moving();

    String keyName(int key);
}
