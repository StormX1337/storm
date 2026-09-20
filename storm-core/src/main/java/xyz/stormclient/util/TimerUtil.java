package xyz.stormclient.util;

/** Simple millisecond stopwatch, the backbone of every delay in the client. */
public final class TimerUtil {

    private long last = System.currentTimeMillis();

    public void reset() { last = System.currentTimeMillis(); }

    public long elapsed() { return System.currentTimeMillis() - last; }

    public boolean passed(long millis) { return elapsed() >= millis; }

    /** Resets and returns true when the delay has passed. */
    public boolean passedAndReset(long millis) {
        if (passed(millis)) { reset(); return true; }
        return false;
    }

    public void setElapsed(long millis) { last = System.currentTimeMillis() - millis; }
}
