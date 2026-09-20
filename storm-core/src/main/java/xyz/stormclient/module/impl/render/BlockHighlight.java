package xyz.stormclient.module.impl.render;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.RenderEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ColorSetting;
import xyz.stormclient.setting.NumberSetting;

public class BlockHighlight extends Module {

    private final ColorSetting   color  = add(new ColorSetting("Color", 0xFF35C4FF));
    private final NumberSetting  width  = add(new NumberSetting("Width", 1.5, 0.5, 4.0, 0.1).suffix("px"));
    private final BooleanSetting filled = add(new BooleanSetting("Filled", false));

    public BlockHighlight() {
        super("BlockHighlight", "Replaces the vanilla block outline", Category.RENDER);
    }

    @Subscribe
    public void onRender(RenderEvent.World event) {
        if (nullCheck()) return;
        int[] hit = world().raytraceBlock(6.0);
        if (hit == null) return;

        mc().renderer().lineWidth(width.getFloat());
        mc().renderer().box3D(hit[0], hit[1], hit[2], hit[0] + 1, hit[1] + 1, hit[2] + 1,
                color.rgb(), filled.get());
    }
}
