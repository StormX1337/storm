package xyz.stormclient.module.impl.render;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.RenderEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ColorSetting;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.EntityUtil;

/** Draws boxes, outlines or a 2D frame around entities. */
public class Esp extends Module {

    private final ModeSetting   mode      = add(new ModeSetting("Mode", "2D", "2D", "Box", "Outline", "Corners"));
    private final ColorSetting  color     = add(new ColorSetting("Color", 0xFF35C4FF));
    private final ColorSetting  friendColor = add(new ColorSetting("Friend color", 0xFF45E08A));
    private final NumberSetting width     = add(new NumberSetting("Line width", 1.5, 0.5, 5.0, 0.1).suffix("px"));
    private final NumberSetting range     = add(new NumberSetting("Range", 64, 8, 256, 4).suffix("m"));
    private final BooleanSetting health   = add(new BooleanSetting("Health bar", true));
    private final BooleanSetting players  = add(new BooleanSetting("Players", true));
    private final BooleanSetting mobs     = add(new BooleanSetting("Mobs", false));
    private final BooleanSetting animals  = add(new BooleanSetting("Animals", false));
    private final BooleanSetting invisible= add(new BooleanSetting("Invisible", true));
    private final BooleanSetting teamColor= add(new BooleanSetting("Use team colors", false));

    public Esp() {
        super("ESP", "Highlights entities through walls", Category.RENDER);
    }

    @Override public String tag() { return mode.get(); }

    @Subscribe
    public void onRender3D(RenderEvent.World event) {
        if (nullCheck() || mode.is("2D")) return;
        IRenderer r = mc().renderer();
        r.lineWidth(width.getFloat());

        for (IEntity e : world().entities()) {
            if (!shouldRender(e)) continue;
            double[] box = e.boundingBox();
            int argb = colorFor(e);
            r.box3D(box[0], box[1], box[2], box[3], box[4], box[5], argb, mode.is("Box"));
        }
    }

    @Subscribe
    public void onRenderHud(RenderEvent.Hud event) {
        if (nullCheck() || !mode.is("2D")) return;
        IRenderer r = mc().renderer();

        for (IEntity e : world().entities()) {
            if (!shouldRender(e)) continue;
            double[] rect = screenRect(e, event.partialTicks());
            if (rect == null) continue;

            int argb = colorFor(e);
            r.rectOutline(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1], width.getFloat(), argb);

            if (health.get()) {
                double h = rect[3] - rect[1];
                float fraction = EntityUtil.healthFraction(e);
                r.rect(rect[0] - 4, rect[1], 2, h, 0x90000000);
                r.rect(rect[0] - 4, rect[1] + h * (1 - fraction), 2, h * fraction, ColorUtil.health(fraction));
            }
        }
    }

    private double[] screenRect(IEntity e, float partial) {
        IRenderer r = mc().renderer();
        double x = e.renderX(partial), y = e.renderY(partial), z = e.renderZ(partial);
        double w = e.width() / 1.6;
        double h = e.height() + 0.2;

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean any = false;

        for (int i = 0; i < 8; i++) {
            double px = x + ((i & 1) == 0 ? -w : w);
            double py = y + ((i & 2) == 0 ? 0 : h);
            double pz = z + ((i & 4) == 0 ? -w : w);
            double[] p = r.project(px, py, pz);
            if (p == null) continue;
            any = true;
            minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]);
        }
        return any ? new double[] { minX, minY, maxX, maxY } : null;
    }

    private int colorFor(IEntity e) {
        if (Storm.get().friends().isFriend(e.name())) return friendColor.rgb();
        if (teamColor.get() && e.teamColor() != 0) return ColorUtil.withAlpha(e.teamColor(), color.alpha());
        return color.rgb(e.id() * 40);
    }

    private boolean shouldRender(IEntity e) {
        if (e.isLocalPlayer() || e.isDead()) return false;
        if (!invisible.get() && e.isInvisible()) return false;
        if (e.distanceTo(player()) > range.get()) return false;
        if (e.isPlayer()) return players.get();
        if (e.isMonster()) return mobs.get();
        if (e.isAnimal()) return animals.get();
        return false;
    }
}
