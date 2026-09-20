package xyz.stormclient.util;

import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.bridge.IPlayer;

public final class EntityUtil {

    private EntityUtil() { }

    /** Health plus absorption, which is what actually has to be chewed through. */
    public static float effectiveHealth(IEntity entity) {
        return entity.health() + entity.absorption();
    }

    public static float healthFraction(IEntity entity) {
        float max = entity.maxHealth();
        return max <= 0 ? 0 : MathUtil.clamp(entity.health() / max, 0F, 1F);
    }

    /** Distance from the player's eyes to the closest point of the target hitbox. */
    public static double hitboxDistance(IEntity target) {
        IPlayer player = Bridge.mc().player();
        if (player == null) return Double.MAX_VALUE;

        double[] box = target.boundingBox();
        double ex = player.x();
        double ey = player.y() + player.eyeHeight();
        double ez = player.z();

        double dx = Math.max(box[0] - ex, Math.max(0, ex - box[3]));
        double dy = Math.max(box[1] - ey, Math.max(0, ey - box[4]));
        double dz = Math.max(box[2] - ez, Math.max(0, ez - box[5]));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static boolean isTeammate(IEntity entity) {
        IPlayer player = Bridge.mc().player();
        if (player == null) return false;
        String own = player.teamName();
        return own != null && !own.isEmpty() && own.equals(entity.teamName());
    }

    /** Moving average of an entity's motion, used by prediction and backtrack. */
    public static double speed(IEntity entity) {
        double dx = entity.x() - entity.lastX();
        double dz = entity.z() - entity.lastZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static String prettyName(IEntity entity) {
        String name = entity.displayName();
        return name == null || name.isEmpty() ? entity.name() : stripColor(name);
    }

    public static String stripColor(String input) {
        return input == null ? "" : input.replaceAll("(?i)\u00a7[0-9A-FK-OR]", "");
    }
}
