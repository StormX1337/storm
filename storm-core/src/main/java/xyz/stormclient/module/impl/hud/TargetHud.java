package xyz.stormclient.module.impl.hud;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.module.impl.combat.KillAura;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.util.Animation;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.EntityUtil;

/** Shows who you are fighting, with a smoothed health bar. */
public class TargetHud extends HudModule {

    private final ModeSetting    style   = add(new ModeSetting("Style", "Modern", "Modern", "Compact"));
    private final BooleanSetting head    = add(new BooleanSetting("Player head", true));
    private final BooleanSetting distance= add(new BooleanSetting("Distance", true));
    private final BooleanSetting fadeOut = add(new BooleanSetting("Fade out", true));

    private final Animation show   = new Animation(8F);
    private final Animation health = new Animation(3F);

    private IEntity last;

    public TargetHud() {
        super("TargetHUD", "Information about your current target", 0.42, 0.62);
    }

    @Override public double width()  { return style.is("Compact") ? 96 : 128; }
    @Override public double height() { return style.is("Compact") ? 26 : 38; }

    private IEntity currentTarget() {
        KillAura aura = Storm.get().modules().get(KillAura.class);
        if (aura != null && aura.isEnabled() && aura.target() != null) return aura.target();
        return Storm.get().lastAttacked();
    }

    @Override public void renderElement(IRenderer r, IFontRenderer font) {
        IEntity target = currentTarget();
        if (target != null) last = target;

        boolean visible = target != null || (fadeOut.get() && last != null && Storm.get().sinceLastAttack() < 2500);
        show.set(visible);
        float t = show.easedOut();
        if (t <= 0.01F || last == null) return;

        double w = width(), h = height();
        float fraction = EntityUtil.healthFraction(last);
        health.setTarget(fraction);
        float shown = health.get();

        if (theme().shadows()) r.shadow(0, 0, w, h, theme().radius(), ColorUtil.withAlpha(0xFF000000, (int) (60 * t)));
        r.roundedRect(0, 0, w, h, theme().radius(), ColorUtil.fade(theme().panel(), t * 0.94F));
        r.roundedRectOutline(0, 0, w, h, theme().radius(), 1F, ColorUtil.fade(theme().outline(), t));

        double textX = 6;
        if (head.get() && last.isPlayer()) {
            r.image("storm/heads/" + last.name() + ".png", 5, 5, h - 10, h - 10, ColorUtil.fade(0xFFFFFFFF, t));
            textX = h;
        }

        font.draw(font.trim(EntityUtil.prettyName(last), (int) (w - textX - 6)), textX, 5, ColorUtil.fade(theme().text(), t));

        double barY = h - 12;
        r.roundedRect(textX, barY, w - textX - 6, 4, 2F, ColorUtil.fade(theme().panelDark(), t));
        r.roundedRect(textX, barY, (w - textX - 6) * shown, 4, 2F,
                ColorUtil.fade(ColorUtil.health(shown), t));

        String info = String.format("%.1f hp", EntityUtil.effectiveHealth(last));
        if (distance.get()) info += String.format("  %.1fm", EntityUtil.hitboxDistance(last));
        font.draw(info, textX, barY - 10, ColorUtil.fade(theme().textDim(), t));
    }
}
