package xyz.stormclient.util;

/**
 * A one shot reveal that runs down a list.
 *
 * <p>Rows appearing all at once reads as a jump cut; the same rows arriving a
 * few tens of milliseconds apart reads as the list being dealt out. Each item
 * asks for its own progress by index and the delay falls out of that, so a page
 * needs no timers of its own.
 */
public final class Stagger {

    private final float durationSeconds;
    private final float stepSeconds;
    private final int maxStep;

    private long startedAt;

    /**
     * @param durationSeconds how long one item takes
     * @param stepSeconds     how far apart two neighbours start
     * @param maxStep         after this many items the delay stops growing, so a
     *                        long list does not take a visible age to appear
     */
    public Stagger(float durationSeconds, float stepSeconds, int maxStep) {
        this.durationSeconds = durationSeconds;
        this.stepSeconds = stepSeconds;
        this.maxStep = maxStep;
        this.startedAt = System.nanoTime();
    }

    /** Plays the reveal again from the top, e.g. when the page is opened. */
    public void restart() { startedAt = System.nanoTime(); }

    /** 0 to 1 for the item at this index, eased. */
    public float progress(int index) {
        float elapsed = (System.nanoTime() - startedAt) / 1_000_000_000F;
        float delay = Math.min(index, maxStep) * stepSeconds;
        if (elapsed <= delay) return 0F;
        return MathUtil.easeOut(Math.min(1F, (elapsed - delay) / durationSeconds));
    }

    /** True once every item up to {@code count} has finished. */
    public boolean finished(int count) {
        float elapsed = (System.nanoTime() - startedAt) / 1_000_000_000F;
        return elapsed > Math.min(count, maxStep) * stepSeconds + durationSeconds;
    }
}
