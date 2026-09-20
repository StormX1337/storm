package xyz.stormclient.launcher.core;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Launcher log, mirrored into the console panel. */
public final class Log {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final List<String> HISTORY = new ArrayList<>();
    private static final List<Consumer<String>> LISTENERS = new ArrayList<>();

    private Log() { }

    public static void info(String message)  { write("INFO", message); }
    public static void warn(String message)  { write("WARN", message); }
    public static void error(String message) { write("ERROR", message); }

    public static void error(String message, Throwable t) {
        write("ERROR", message + " - " + t);
    }

    private static synchronized void write(String level, String message) {
        String line = "[" + LocalTime.now().format(TIME) + "] [" + level + "] " + message;
        System.out.println(line);
        HISTORY.add(line);
        if (HISTORY.size() > 500) HISTORY.remove(0);
        for (Consumer<String> listener : LISTENERS) listener.accept(line);
    }

    public static synchronized void listen(Consumer<String> listener) {
        LISTENERS.add(listener);
        for (String line : HISTORY) listener.accept(line);
    }

    public static synchronized List<String> history() { return new ArrayList<>(HISTORY); }
}
