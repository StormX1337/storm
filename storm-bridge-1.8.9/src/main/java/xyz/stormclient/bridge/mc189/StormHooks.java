package xyz.stormclient.bridge.mc189;

import xyz.stormclient.Storm;
import xyz.stormclient.event.events.JumpEvent;
import xyz.stormclient.event.events.MoveEvent;
import xyz.stormclient.event.events.RenderEntityEvent;
import xyz.stormclient.event.events.SlowDownEvent;
import xyz.stormclient.event.events.StepEvent;
import xyz.stormclient.event.events.StrafeEvent;
import xyz.stormclient.event.events.ViewEvent;
import xyz.stormclient.bridge.IEntity;

/**
 * Static entry points for the events Forge does not expose.
 * The mixins in {@code xyz.stormclient.bridge.mc189.mixin} call straight into
 * here, which keeps every mixin down to a couple of lines.
 */
public final class StormHooks {

    private StormHooks() { }

    /** @return the possibly rewritten motion as {x, y, z, safeWalk}. */
    public static double[] onMove(double x, double y, double z) {
        MoveEvent event = new MoveEvent(x, y, z);
        Storm.get().bus().post(event);
        return new double[] { event.x(), event.y(), event.z(), event.safeWalk() ? 1 : 0 };
    }

    /** @return the jump motion, or Float.NaN when a module cancelled the jump. */
    public static float onJump(float motion, float yaw) {
        JumpEvent event = new JumpEvent(motion, yaw);
        Storm.get().bus().post(event);
        return event.isCancelled() ? Float.NaN : event.motion();
    }

    public static float onStep(float height) {
        StepEvent event = new StepEvent(height);
        Storm.get().bus().post(event);
        return event.height();
    }

    /** @return {forward, strafe} multipliers while an item is being used. */
    public static float[] onSlowDown() {
        SlowDownEvent event = new SlowDownEvent();
        Storm.get().bus().post(event);
        return new float[] { event.forward(), event.strafe() };
    }

    public static float[] onStrafe(float friction, float yaw, float forward, float strafe) {
        StrafeEvent event = new StrafeEvent(friction, yaw, forward, strafe);
        Storm.get().bus().post(event);
        return new float[] { event.friction(), event.yaw(), event.forward(), event.strafe() };
    }

    public static float onView(ViewEvent.Kind kind, float value) {
        ViewEvent event = new ViewEvent(kind, value);
        Storm.get().bus().post(event);
        return event.value();
    }

    /** @return true when the entity model should be skipped entirely. */
    public static boolean onRenderEntity(IEntity entity, float partialTicks) {
        RenderEntityEvent.Pre event = new RenderEntityEvent.Pre(entity, partialTicks);
        Storm.get().bus().post(event);
        return event.isCancelled();
    }

    public static boolean onRenderNametag(IEntity entity, float partialTicks) {
        RenderEntityEvent.Nametag event = new RenderEntityEvent.Nametag(entity, partialTicks);
        Storm.get().bus().post(event);
        return event.isCancelled();
    }
}
