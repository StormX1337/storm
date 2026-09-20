package xyz.stormclient.module.impl.render;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IItemStack;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.RenderEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.EntityUtil;

/** Storm's own nametags: name, health, ping, armour and the held item. */
public class Nametags extends Module {

    private final NumberSetting  scale   = add(new NumberSetting("Scale", 1.0, 0.5, 2.5, 0.05).suffix("x"));
    private final NumberSetting  range   = add(new NumberSetting("Range", 64, 8, 256, 4).suffix("m"));
    private final BooleanSetting health  = add(new BooleanSetting("Health", true));
    private final BooleanSetting armor   = add(new BooleanSetting("Armor", true));
    private final BooleanSetting distance= add(new BooleanSetting("Distance", false));
    private final BooleanSetting mobs    = add(new BooleanSetting("Mobs", false));
    private final BooleanSetting background = add(new BooleanSetting("Background", true));

    public Nametags() {
        super("Nametags", "Better nametags above players", Category.RENDER);
    }

    @Subscribe
    public void onRender(RenderEvent.Hud event) {
        if (nullCheck()) return;
        IRenderer r = mc().renderer();
        IFontRenderer font = mc().font("storm", 16);

        for (IEntity e : world().entities()) {
            if (e.isLocalPlayer() || e.isDead()) continue;
            if (!e.isPlayer() && !mobs.get()) continue;
            if (e.distanceTo(player()) > range.get()) continue;

            double[] pos = r.project(e.renderX(event.partialTicks()),
                                     e.renderY(event.partialTicks()) + e.height() + 0.45,
                                     e.renderZ(event.partialTicks()));
            if (pos == null) continue;

            StringBuilder text = new StringBuilder(EntityUtil.prettyName(e));
            if (health.get()) {
                text.append(" \u00a77").append((int) Math.ceil(EntityUtil.effectiveHealth(e)));
            }
            if (distance.get()) {
                text.append(" \u00a78").append((int) e.distanceTo(player())).append('m');
            }

            String label = text.toString();
            double s = scale.get() * Math.max(0.55, 1.4 - e.distanceTo(player()) / 60.0);
            double w = font.width(label) * s;
            double h = font.height() * s;
            double x = pos[0] - w / 2;
            double y = pos[1] - h;

            if (background.get()) {
                r.roundedRect(x - 3, y - 2, w + 6, h + 4, 3F, 0x99101318);
            }

            r.push();
            r.translate(x, y, 0);
            r.scale(s, s, 1);
            int nameColor = Storm.get().friends().isFriend(e.name()) ? 0xFF45E08A
                          : ColorUtil.health(EntityUtil.healthFraction(e));
            font.draw(label, 0, 0, nameColor);
            r.pop();

            if (armor.get() && e.isPlayer()) renderArmor(r, font, e, pos[0], y - 12);
        }
    }

    private void renderArmor(IRenderer r, IFontRenderer font, IEntity e, double cx, double y) {
        double x = cx - 20;
        for (int i = 3; i >= 0; i--) {
            IItemStack stack = e.armorSlot(i);
            if (stack == null || stack.isEmpty()) continue;
            r.image("storm/icons/armor_" + i + ".png", x, y, 10, 10, 0xFFFFFFFF);
            font.draw(String.valueOf(stack.durabilityPercent()), x + 11, y + 2, 0xFFBBBBBB);
            x += 22;
        }
    }
}
