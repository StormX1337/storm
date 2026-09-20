package xyz.stormclient.module.impl.render;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.RenderEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ColorSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.EntityUtil;

public class Tracers extends Module {

    private final ColorSetting   color   = add(new ColorSetting("Color", 0xFF35C4FF));
    private final NumberSetting  width   = add(new NumberSetting("Width", 1.2, 0.5, 4.0, 0.1).suffix("px"));
    private final NumberSetting  range   = add(new NumberSetting("Range", 64, 8, 256, 4).suffix("m"));
    private final BooleanSetting players = add(new BooleanSetting("Players", true));
    private final BooleanSetting mobs    = add(new BooleanSetting("Mobs", false));
    private final BooleanSetting healthColor = add(new BooleanSetting("Health color", true));

    public Tracers() {
        super("Tracers", "Draws a line to every entity", Category.RENDER);
    }

    @Subscribe
    public void onRender(RenderEvent.World event) {
        if (nullCheck()) return;
        float p = event.partialTicks();
        double ex = player().renderX(p);
        double ey = player().renderY(p) + player().eyeHeight();
        double ez = player().renderZ(p);

        mc().renderer().lineWidth(width.getFloat());
        for (IEntity e : world().entities()) {
            if (e.isLocalPlayer() || e.isDead()) continue;
            if (e.isPlayer() ? !players.get() : !mobs.get()) continue;
            if (e.distanceTo(player()) > range.get()) continue;

            int argb = Storm.get().friends().isFriend(e.name()) ? 0xFF45E08A
                     : healthColor.get() ? ColorUtil.withAlpha(ColorUtil.health(EntityUtil.healthFraction(e)), color.alpha())
                     : color.rgb();

            mc().renderer().line3D(ex, ey, ez,
                    e.renderX(p), e.renderY(p) + e.height() / 2, e.renderZ(p), argb, width.getFloat());
        }
    }
}
