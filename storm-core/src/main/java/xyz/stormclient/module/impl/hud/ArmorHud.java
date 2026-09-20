package xyz.stormclient.module.impl.hud;

import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IItemStack;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.util.ColorUtil;

public class ArmorHud extends HudModule {

    private final ModeSetting    direction  = add(new ModeSetting("Direction", "Horizontal", "Horizontal", "Vertical"));
    private final BooleanSetting durability = add(new BooleanSetting("Durability", true));
    private final BooleanSetting held       = add(new BooleanSetting("Held item", true));
    private final BooleanSetting bars       = add(new BooleanSetting("Durability bars", true));

    public ArmorHud() {
        super("ArmorHUD", "Shows your armour and its durability", 0.45, 0.78);
    }

    private int slots() { return 4 + (held.get() ? 1 : 0); }

    @Override public double width()  { return direction.is("Horizontal") ? slots() * 20 : 20; }
    @Override public double height() { return direction.is("Horizontal") ? 26 : slots() * 20; }

    @Override public void renderElement(IRenderer r, IFontRenderer font) {
        if (nullCheck()) return;
        double x = 0, y = 0;

        for (int i = 3; i >= 0; i--) {
            IItemStack stack = player().inventory().armor(i);
            draw(r, font, stack, x, y);
            if (direction.is("Horizontal")) x += 20; else y += 20;
        }
        if (held.get()) {
            draw(r, font, player().inventory().slot(player().inventory().heldSlot()), x, y);
        }
    }

    private void draw(IRenderer r, IFontRenderer font, IItemStack stack, double x, double y) {
        if (stack == null || stack.isEmpty()) return;
        r.image("item/" + stack.registryName(), x, y, 16, 16, 0xFFFFFFFF);

        if (stack.count() > 1) {
            font.drawShadow(String.valueOf(stack.count()), x + 10, y + 9, theme().text());
        }
        if (bars.get() && stack.maxDamage() > 0) {
            float fraction = stack.durabilityPercent() / 100F;
            r.rect(x, y + 17, 16, 2, 0x99000000);
            r.rect(x, y + 17, 16 * fraction, 2, ColorUtil.health(fraction));
        }
        if (durability.get() && stack.maxDamage() > 0) {
            String text = stack.durabilityPercent() + "%";
            font.drawShadow(text, x + 8 - font.width(text) / 2.0, y + 19, theme().textDim());
        }
    }
}
