package xyz.stormclient.module.impl.world;

import xyz.stormclient.Storm;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.rotation.Rotation;
import xyz.stormclient.rotation.RotationUtil;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.setting.StringSetting;

/** Breaks every block in range, optionally only a chosen one. */
public class Nuker extends Module {

    private final ModeSetting    mode   = add(new ModeSetting("Mode", "Normal", "Normal", "Flatten", "Select"));
    private final NumberSetting  range  = add(new NumberSetting("Range", 4.5, 1.0, 6.0, 0.1).suffix("m"));
    private final StringSetting  filter = add(new StringSetting("Block", "stone"));
    private final BooleanSetting rotate = add(new BooleanSetting("Rotations", true));

    public Nuker() {
        super("Nuker", "Breaks blocks around you", Category.WORLD);
        filter.visibleWhen(() -> mode.is("Select"));
    }

    @Override public String tag() { return mode.get(); }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (nullCheck()) return;

        int r = (int) Math.ceil(range.get());
        int px = (int) Math.floor(player().x());
        int py = (int) Math.floor(player().y());
        int pz = (int) Math.floor(player().z());

        int[] best = null;
        double bestDist = Double.MAX_VALUE;

        for (int x = px - r; x <= px + r; x++) {
            for (int y = py - r; y <= py + r; y++) {
                for (int z = pz - r; z <= pz + r; z++) {
                    if (world().isAir(x, y, z) || world().isLiquid(x, y, z)) continue;
                    if (mode.is("Flatten") && y <= py) continue;
                    if (mode.is("Select") && !world().blockName(x, y, z).contains(filter.get())) continue;

                    double dist = player().distanceTo(x + 0.5, y + 0.5, z + 0.5);
                    if (dist > range.get() || dist >= bestDist) continue;
                    bestDist = dist;
                    best = new int[] { x, y, z };
                }
            }
        }
        if (best == null) return;

        if (rotate.get()) {
            Rotation rot = RotationUtil.toPosition(best[0] + 0.5, best[1] + 0.5, best[2] + 0.5);
            Storm.get().rotations().request(rot, 40);
        }
        player().swingArm();
    }
}
