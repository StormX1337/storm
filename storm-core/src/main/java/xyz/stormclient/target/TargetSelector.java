package xyz.stormclient.target;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.bridge.IPlayer;
import xyz.stormclient.rotation.Rotation;
import xyz.stormclient.rotation.RotationUtil;
import xyz.stormclient.util.EntityUtil;
import xyz.stormclient.util.MathUtil;

/** Shared target picking, used by KillAura, Aim assist, TargetHUD and the ESP. */
public final class TargetSelector {

    private TargetSelector() { }

    public static List<IEntity> candidates(TargetFilter filter) {
        List<IEntity> out = new ArrayList<IEntity>();
        IPlayer self = Bridge.mc().player();
        if (self == null || Bridge.mc().world() == null) return out;

        for (IEntity e : Bridge.mc().world().entities()) {
            if (!valid(e, self, filter)) continue;
            out.add(e);
        }
        return out;
    }

    public static IEntity best(TargetFilter filter, TargetSort sort) {
        List<IEntity> list = candidates(filter);
        if (list.isEmpty()) return null;
        list.sort(comparator(sort));
        return list.get(0);
    }

    public static boolean valid(IEntity e, IPlayer self, TargetFilter filter) {
        if (e == null || self == null) return false;
        if (e.isLocalPlayer()) return false;
        if (!e.isLiving()) return false;
        if (!filter.dead && (e.isDead() || e.health() <= 0)) return false;
        if (!filter.invisible && e.isInvisible()) return false;
        if (!filter.npcs && e.isNpc()) return false;

        if (e.isPlayer()) {
            if (!filter.players) return false;
            if (!filter.friends && Storm.get().friends().isFriend(e.name())) return false;
            if (!filter.teammates && EntityUtil.isTeammate(e)) return false;
            xyz.stormclient.module.impl.combat.AntiBot antiBot = Storm.get().antiBot();
            if (antiBot != null && antiBot.isBot(e)) return false;
        } else if (e.isMonster()) {
            if (!filter.mobs) return false;
        } else if (e.isAnimal()) {
            if (!filter.animals) return false;
        } else {
            return false;
        }

        double distance = EntityUtil.hitboxDistance(e);
        return distance <= filter.range;
    }

    public static Comparator<IEntity> comparator(final TargetSort sort) {
        return new Comparator<IEntity>() {
            public int compare(IEntity a, IEntity b) {
                switch (sort) {
                    case HEALTH:
                        return Float.compare(EntityUtil.effectiveHealth(a), EntityUtil.effectiveHealth(b));
                    case ARMOR:
                        return Float.compare(a.armorValue(), b.armorValue());
                    case HURT_TIME:
                        return Integer.compare(a.hurtTime(), b.hurtTime());
                    case ANGLE:
                        return Float.compare(angleTo(a), angleTo(b));
                    case DISTANCE:
                    default:
                        return Double.compare(EntityUtil.hitboxDistance(a), EntityUtil.hitboxDistance(b));
                }
            }
        };
    }

    /** Angular distance between the current view and the rotation that would hit the target. */
    public static float angleTo(IEntity target) {
        IPlayer self = Bridge.mc().player();
        if (self == null) return 180F;
        Rotation needed = RotationUtil.toEntity(target, 0.5, 0);
        float yaw = Math.abs(MathUtil.angleDiff(self.yaw(), needed.yaw));
        float pitch = Math.abs(needed.pitch - self.pitch());
        return Math.max(yaw, pitch);
    }
}
