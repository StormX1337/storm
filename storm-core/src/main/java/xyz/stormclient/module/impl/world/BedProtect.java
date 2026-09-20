package xyz.stormclient.module.impl.world;

import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.RenderEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.ColorSetting;
import xyz.stormclient.setting.NumberSetting;

/** Bedwars helper: highlights the blocks that still protect a bed. */
public class BedProtect extends Module {

    private final NumberSetting range = add(new NumberSetting("Range", 24, 4, 64, 2).suffix("m"));
    private final ColorSetting  color = add(new ColorSetting("Color", 0x6635C4FF));

    public BedProtect() {
        super("BedProtect", "Shows which blocks still cover a bed", Category.WORLD);
    }

    @Subscribe
    public void onRender(RenderEvent.World event) {
        if (nullCheck()) return;
        int r = range.getInt();
        int px = (int) player().x(), py = (int) player().y(), pz = (int) player().z();
        IRenderer renderer = mc().renderer();

        for (int x = px - r; x <= px + r; x++) {
            for (int y = Math.max(world().minY(), py - 10); y <= py + 10; y++) {
                for (int z = pz - r; z <= pz + r; z++) {
                    if (!world().blockName(x, y, z).contains("bed")) continue;
                    for (int[] offset : new int[][] { {1,0,0}, {-1,0,0}, {0,1,0}, {0,0,1}, {0,0,-1} }) {
                        int bx = x + offset[0], by = y + offset[1], bz = z + offset[2];
                        if (world().isAir(bx, by, bz)) continue;
                        renderer.box3D(bx, by, bz, bx + 1, by + 1, bz + 1, color.rgb(), true);
                    }
                }
            }
        }
    }
}
