package xyz.stormclient.module.impl.combat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.bridge.PacketType;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.PacketEvent;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.event.events.WorldEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;

/**
 * Flags fake players so combat and the ESP ignore them.
 * Several cheap heuristics vote together instead of one hard rule, which keeps
 * false positives on normal players low.
 */
public class AntiBot extends Module {

    private final BooleanSetting tabCheck    = add(new BooleanSetting("Tab list", true));
    private final BooleanSetting groundCheck = add(new BooleanSetting("Ground", true));
    private final BooleanSetting pingCheck   = add(new BooleanSetting("No ping", true));
    private final BooleanSetting armorCheck  = add(new BooleanSetting("No armor", false));
    private final BooleanSetting swingCheck  = add(new BooleanSetting("Never swings", true));
    private final NumberSetting  votes       = add(new NumberSetting("Votes needed", 2, 1, 4, 1));

    private final Set<Integer> bots = new HashSet<Integer>();
    private final Map<Integer, Integer> airTicks = new HashMap<Integer, Integer>();
    private final Set<Integer> swung = new HashSet<Integer>();

    public AntiBot() {
        super("AntiBot", "Ignores fake players", Category.COMBAT);
    }

    @Override public String tag() { return bots.isEmpty() ? "" : bots.size() + " found"; }

    @Override public void onDisable() { reset(); }

    @Subscribe public void onWorld(WorldEvent.Load event) { reset(); }

    private void reset() {
        bots.clear();
        airTicks.clear();
        swung.clear();
    }

    @Subscribe
    public void onPacket(PacketEvent.Receive event) {
        if (event.type() == PacketType.C_ANIMATION || event.type() == PacketType.S_ENTITY_STATUS) {
            swung.add(event.packet().getInt("entityId"));
        }
        if (event.type() == PacketType.S_DESTROY_ENTITIES) {
            int id = event.packet().getInt("entityId");
            bots.remove(id);
            airTicks.remove(id);
        }
    }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (nullCheck()) return;

        for (IEntity e : world().players()) {
            if (e.isLocalPlayer()) continue;

            if (!e.isOnGround()) {
                Integer ticks = airTicks.get(e.id());
                airTicks.put(e.id(), ticks == null ? 1 : ticks + 1);
            } else {
                airTicks.put(e.id(), 0);
            }

            int score = 0;
            if (tabCheck.get() && (e.displayName() == null || e.displayName().isEmpty())) score++;
            if (groundCheck.get() && air(e.id()) > 60) score++;
            if (pingCheck.get() && e.ticksExisted() > 100 && e.name().length() > 16) score++;
            if (armorCheck.get() && noArmor(e) && e.ticksExisted() > 100) score++;
            if (swingCheck.get() && e.ticksExisted() > 200 && !swung.contains(e.id())) score++;

            if (score >= votes.getInt()) bots.add(e.id());
            else bots.remove(e.id());
        }
    }

    private int air(int id) {
        Integer ticks = airTicks.get(id);
        return ticks == null ? 0 : ticks;
    }

    private boolean noArmor(IEntity e) {
        for (int i = 0; i < 4; i++) {
            if (e.armorSlot(i) != null && !e.armorSlot(i).isEmpty()) return false;
        }
        return true;
    }

    public boolean isBot(IEntity entity) {
        return isEnabled() && entity != null && bots.contains(entity.id());
    }
}
