package xyz.stormclient.util;

/**
 * Frame rate independent 0..1 animation. Everything in the UI that moves,
 * fades or slides runs through one of these.
 */
public final class Animation {

    private float value;
    private float target;
    private float speed;      // units per second
    private long  lastUpdate = System.nanoTime();

    public Animation(float initial, float speed) {
        this.value = initial;
        this.target = initial;
        this.speed = speed;
    }

    public Animation(float speed) { this(0F, speed); }

    public void setTarget(float target) { this.target = MathUtil.clamp(target, 0F, 1F); }
    public void set(boolean on)         { setTarget(on ? 1F : 0F); }
    public void snap(float value)       { this.value = this.target = MathUtil.clamp(value, 0F, 1F); }

    public float target() { return target; }
    public void setSpeed(float speed) { this.speed = speed; }

    /** Advances and returns the eased value. */
    public float get() {
        long now = System.nanoTime();
        float delta = (now - lastUpdate) / 1_000_000_000F;
        lastUpdate = now;
        if (delta > 0.25F) delta = 0.25F;               // tab-out guard

        if (value < target)      value = Math.min(target, value + speed * delta);
        else if (value > target) value = Math.max(target, value - speed * delta);

        return value;
    }

    public float raw()    { return value; }
    public float eased()  { return MathUtil.easeInOut(get()); }
    public float easedOut() { return MathUtil.easeOut(get()); }

    public boolean finished() { return Math.abs(value - target) < 0.001F; }
    public boolean visible()  { return value > 0.001F || target > 0.001F; }
}
