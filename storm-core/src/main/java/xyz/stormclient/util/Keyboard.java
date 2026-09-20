package xyz.stormclient.util;

import java.util.HashMap;
import java.util.Map;

/**
 * LWJGL2 key codes. 1.8.9 uses them natively, the modern bridges translate
 * GLFW codes into these so configs stay portable between versions.
 */
public final class Keyboard {

    public static final int KEY_NONE = 0;
    public static final int KEY_ESCAPE = 1;
    public static final int KEY_TAB = 15;
    public static final int KEY_RETURN = 28;
    public static final int KEY_LCONTROL = 29;
    public static final int KEY_LSHIFT = 42;
    public static final int KEY_RSHIFT = 54;
    public static final int KEY_LMENU = 56;
    public static final int KEY_SPACE = 57;
    public static final int KEY_DELETE = 211;
    public static final int KEY_BACK = 14;
    public static final int KEY_UP = 200;
    public static final int KEY_DOWN = 208;
    public static final int KEY_LEFT = 203;
    public static final int KEY_RIGHT = 205;
    public static final int KEY_RSHIFT_ALT = 54;

    private static final Map<Integer, String> NAMES = new HashMap<Integer, String>();
    private static final Map<String, Integer> CODES = new HashMap<String, Integer>();

    static {
        String[] letters = "QWERTYUIOP".split("");
        int[] topRow = { 16, 17, 18, 19, 20, 21, 22, 23, 24, 25 };
        for (int i = 0; i < letters.length; i++) bind(topRow[i], letters[i]);

        String[] home = "ASDFGHJKL".split("");
        int[] homeRow = { 30, 31, 32, 33, 34, 35, 36, 37, 38 };
        for (int i = 0; i < home.length; i++) bind(homeRow[i], home[i]);

        String[] bottom = "ZXCVBNM".split("");
        int[] bottomRow = { 44, 45, 46, 47, 48, 49, 50 };
        for (int i = 0; i < bottom.length; i++) bind(bottomRow[i], bottom[i]);

        for (int i = 1; i <= 9; i++) bind(i + 1, String.valueOf(i));
        bind(11, "0");
        for (int i = 1; i <= 12; i++) bind(58 + (i <= 10 ? i - 1 : i + 77), "F" + i);

        bind(KEY_NONE, "NONE");
        bind(KEY_ESCAPE, "ESC");
        bind(KEY_TAB, "TAB");
        bind(KEY_RETURN, "ENTER");
        bind(KEY_LCONTROL, "LCTRL");
        bind(157, "RCTRL");
        bind(KEY_LSHIFT, "LSHIFT");
        bind(KEY_RSHIFT, "RSHIFT");
        bind(KEY_LMENU, "LALT");
        bind(184, "RALT");
        bind(KEY_SPACE, "SPACE");
        bind(KEY_BACK, "BACKSPACE");
        bind(KEY_DELETE, "DELETE");
        bind(KEY_UP, "UP");
        bind(KEY_DOWN, "DOWN");
        bind(KEY_LEFT, "LEFT");
        bind(KEY_RIGHT, "RIGHT");
        bind(12, "MINUS");
        bind(13, "EQUALS");
        bind(26, "LBRACKET");
        bind(27, "RBRACKET");
        bind(39, "SEMICOLON");
        bind(40, "APOSTROPHE");
        bind(41, "GRAVE");
        bind(43, "BACKSLASH");
        bind(51, "COMMA");
        bind(52, "PERIOD");
        bind(53, "SLASH");
        bind(199, "HOME");
        bind(207, "END");
        bind(201, "PAGEUP");
        bind(209, "PAGEDOWN");
        bind(210, "INSERT");
    }

    private Keyboard() { }

    private static void bind(int code, String name) {
        NAMES.put(code, name);
        CODES.put(name, code);
    }

    public static String name(int code) {
        String name = NAMES.get(code);
        return name != null ? name : "KEY_" + code;
    }

    public static int code(String name) {
        Integer code = CODES.get(name.toUpperCase());
        return code != null ? code : KEY_NONE;
    }
}
