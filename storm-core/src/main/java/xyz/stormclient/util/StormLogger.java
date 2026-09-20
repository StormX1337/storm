package xyz.stormclient.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class StormLogger {

    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss");
    private static boolean debug = false;

    private StormLogger() { }

    public static void setDebug(boolean value) { debug = value; }
    public static boolean debugEnabled() { return debug; }

    public static void info(String message)  { print("INFO", message); }
    public static void warn(String message)  { print("WARN", message); }
    public static void error(String message) { print("ERROR", message); }

    public static void error(String message, Throwable t) {
        print("ERROR", message + " - " + t);
        if (debug) t.printStackTrace();
    }

    public static void debug(String message) { if (debug) print("DEBUG", message); }

    private static void print(String level, String message) {
        System.out.println("[" + TIME.format(new Date()) + "] [Storm/" + level + "] " + message);
    }
}
