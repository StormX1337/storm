package xyz.stormclient.module.impl.movement;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.NumberSetting;

/** Catches you before you fall out of the world. */
public class AntiVoid extends Module {

    private final NumberSetting fallDistance = add(new NumberSetting("Fall distance", 6, 2, 30, 1).suffix("m"));
    private final NumberSetting minY = add(new NumberSetting("Min Y", 2, -64, 64, 1));

    private double lastSafeX, lastSafeY, lastSafeZ;
    private boolean hasSafe;

    public AntiVoid() {
        super("AntiVoid", "Stops you from falling into the void", Category.MOVEMENT);
    }

    @Subscribe
    public void onTick(TickEvent.Pre event) {
        if (nullCheck()) return;

        if (player().isOnGround()) {
            lastSafeX = player().x();
            lastSafeY = player().y();
            lastSafeZ = player().z();
            hasSafe = true;
            return;
        }

        boolean voidBelow = true;
        int px = (int) Math.floor(player().x());
        int pz = (int) Math.floor(player().z());
        for (int y = (int) player().y(); y >= world().minY(); y--) {
            if (world().isSolid(px, y, pz)) { voidBelow = false; break; }
        }

        boolean falling = player().fallDistance() > fallDistance.getFloat();
        if ((voidBelow && falling) || player().y() < minY.get()) {
            player().setMotion(0, 0, 0);
            if (hasSafe) player().setPosition(lastSafeX, lastSafeY, lastSafeZ);
        }
    }
}
