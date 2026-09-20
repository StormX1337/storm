package xyz.stormclient.ui.notify;

import xyz.stormclient.util.Animation;

public class Notification {

    public enum Type {
        INFO   (0xFF35C4FF),
        SUCCESS(0xFF45E08A),
        WARNING(0xFFFFB648),
        ERROR  (0xFFFF4E6E);

        public final int color;
        Type(int color) { this.color = color; }
    }

    private final String title;
    private final String message;
    private final Type type;
    private final long duration;
    private final long created = System.currentTimeMillis();

    public final Animation animation = new Animation(7F);

    public Notification(String title, String message, Type type, long duration) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.duration = duration;
        animation.set(true);
    }

    public String title()   { return title; }
    public String message() { return message; }
    public Type type()      { return type; }

    public long age()        { return System.currentTimeMillis() - created; }
    public boolean expired() { return age() > duration; }
    /** 0..1 of the lifetime, used by the progress bar. */
    public float progress()  { return Math.min(1F, age() / (float) duration); }
}
