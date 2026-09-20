package xyz.stormclient.rotation;

import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.bridge.IPlayer;
import xyz.stormclient.event.Priority;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.MotionEvent;
import xyz.stormclient.event.events.TickEvent;

/**
 * Central owner of the rotations Storm sends to the server.
 * Modules do not touch the player's view directly, they request a rotation and
 * the manager interpolates it, keeps it alive for a few ticks and then hands
 * control back to the mouse. That way two modules can never fight each other.
 */
public final class RotationManager {

    private Rotation current;
    private Rotation requested;
    private int      priority;
    private int      ticksLeft;

    private float yawStep   = 180F;
    private float pitchStep = 180F;
    private boolean silent  = true;
    private double sensitivity = 0.5;

    public void request(Rotation rotation, int priority, int ticks, float yawStep, float pitchStep, boolean silent) {
        if (this.ticksLeft > 0 && priority < this.priority) return;
        this.requested = rotation;
        this.priority = priority;
        this.ticksLeft = ticks;
        this.yawStep = yawStep;
        this.pitchStep = pitchStep;
        this.silent = silent;
    }

    public void request(Rotation rotation, int priority) {
        request(rotation, priority, 2, 180F, 180F, true);
    }

    public void clear() {
        requested = null;
        ticksLeft = 0;
        priority = 0;
    }

    public boolean active()     { return ticksLeft > 0 && current != null; }
    public Rotation current()   { return current; }
    public Rotation requested() { return requested; }

    public void setSensitivity(double sensitivity) { this.sensitivity = sensitivity; }

    @Subscribe(priority = Priority.HIGHEST)
    public void onMotion(MotionEvent event) {
        IPlayer player = Bridge.mc().player();
        if (player == null) return;

        if (!event.isPre()) return;

        if (requested == null || ticksLeft <= 0) {
            current = null;
            player.setServerRotation(event.yaw(), event.pitch());
            return;
        }

        Rotation from = current != null ? current : new Rotation(event.yaw(), event.pitch());
        Rotation next = RotationUtil.step(from, requested, yawStep, pitchStep);
        next = RotationUtil.applyGcd(next, from, sensitivity);
        current = next;

        event.setRotation(next.yaw, next.pitch);
        player.setServerRotation(next.yaw, next.pitch);

        if (!silent) player.setRotation(next.yaw, next.pitch);
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (ticksLeft > 0 && --ticksLeft == 0) {
            requested = null;
            priority = 0;
        }
    }
}
