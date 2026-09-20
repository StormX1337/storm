package xyz.stormclient.module.impl.hud;

import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IInventory;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.util.ColorUtil;

/** Counts the blocks you are carrying, the number every bridger stares at. */
public class BlockCounter extends HudModule {

    private final BooleanSetting icon = add(new BooleanSetting("Item icon", true));
    private final BooleanSetting all  = add(new BooleanSetting("Whole inventory", true));

    public BlockCounter() {
        super("BlockCounter", "Counts the blocks in your inventory", 0.5, 0.72);
    }

    @Override public double width()  { return 40; }
    @Override public double height() { return 20; }

    @Override public void renderElement(IRenderer r, IFontRenderer font) {
        if (nullCheck()) return;
        IInventory inv = player().inventory();
        if (!inv.slot(inv.heldSlot()).isBlock()) return;

        String name = inv.slot(inv.heldSlot()).registryName();
        int count = 0;
        int limit = all.get() ? inv.size() : 9;
        for (int i = 0; i < limit; i++) {
            if (inv.slot(i).isBlock() && inv.slot(i).registryName().equals(name)) count += inv.slot(i).count();
        }

        IFontRenderer big = mc().font(theme().font(), 24);
        String text = String.valueOf(count);
        double x = 0;
        if (icon.get()) {
            r.image("item/" + name, 0, 2, 16, 16, 0xFFFFFFFF);
            x = 20;
        }
        big.drawShadow(text, x, 0, count <= 8 ? ColorUtil.health(count / 16F) : theme().text());
    }
}
