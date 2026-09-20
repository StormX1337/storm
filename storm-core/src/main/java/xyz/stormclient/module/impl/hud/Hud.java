package xyz.stormclient.module.impl.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import xyz.stormclient.Storm;
import xyz.stormclient.StormInfo;
import xyz.stormclient.bridge.IFontRenderer;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.RenderEvent;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ColorSetting;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.MovementUtil;

/** Watermark, module list and the small info strip. */
public class Hud extends HudModule {

    private final ModeSetting    listMode  = add(new ModeSetting("List", "Right", "Right", "Left", "Off"));
    private final ModeSetting    sortMode  = add(new ModeSetting("Sort", "Length", "Length", "Alphabetical", "Category"));
    private final ColorSetting   color     = add(new ColorSetting("Color", 0xFF35C4FF));
    private final BooleanSetting watermark = add(new BooleanSetting("Watermark", true));
    private final BooleanSetting tags      = add(new BooleanSetting("Show tags", true));
    private final BooleanSetting background= add(new BooleanSetting("Background", false));
    private final BooleanSetting fps       = add(new BooleanSetting("FPS", true));
    private final BooleanSetting ping      = add(new BooleanSetting("Ping", true));
    private final BooleanSetting coords    = add(new BooleanSetting("Coordinates", true));
    private final BooleanSetting speed     = add(new BooleanSetting("Speed", false));

    public Hud() {
        super("HUD", "Watermark, module list and info", 0.01, 0.01);
        setEnabled(true);
    }

    @Override public double width()  { return 110; }
    @Override public double height() { return 20; }

    @Override public void renderElement(IRenderer r, IFontRenderer font) {
        if (!watermark.get()) return;
        IFontRenderer big = mc().font(theme().font(), 22);

        xyz.stormclient.ui.click.ClickGuiScreen.drawBolt(r, 0, 2, 0.9, color.rgb());
        big.drawShadow(StormInfo.NAME, 12, 0, theme().text());
        font.drawShadow("v" + StormInfo.VERSION, 14 + big.width(StormInfo.NAME), 6, color.rgb());
    }

    /** The array list and the info strip are screen anchored, not part of the draggable element. */
    @Subscribe
    public void onRenderExtra(RenderEvent.Hud event) {
        if (nullCheck()) return;
        IRenderer r = mc().renderer();
        IFontRenderer font = mc().font(theme().font(), 16);

        if (!listMode.is("Off")) renderModuleList(r, font);
        renderInfo(font);
    }

    private void renderModuleList(IRenderer r, IFontRenderer font) {
        List<Module> list = new ArrayList<Module>();
        for (Module m : Storm.get().modules().all()) {
            if (m.drawn() && (m.isEnabled() || m.animation.visible())) list.add(m);
        }

        final boolean showTags = tags.get();
        list.sort(new Comparator<Module>() {
            public int compare(Module a, Module b) {
                if (sortMode.is("Alphabetical")) return a.name().compareToIgnoreCase(b.name());
                if (sortMode.is("Category")) {
                    int c = a.category().compareTo(b.category());
                    if (c != 0) return c;
                }
                return Integer.compare(width(b, showTags), width(a, showTags));
            }
            private int width(Module m, boolean withTags) {
                return mc().font(theme().font(), 16).width(label(m, withTags));
            }
        });

        boolean right = listMode.is("Right");
        double y = 2;
        int index = 0;

        for (Module module : list) {
            String label = label(module, showTags);
            float anim = module.animation.easedOut();
            if (anim <= 0.002F) continue;

            double textWidth = font.width(label);
            double x = right
                    ? mc().scaledWidth() - 3 - textWidth * anim
                    : 3 - textWidth * (1 - anim);

            int argb = color.rainbow() ? color.rgb(index * 120) : color.rgb();
            if (background.get()) {
                r.rect(x - 2, y - 1, textWidth + 4, font.height() + 2, ColorUtil.withAlpha(0xFF000000, (int) (110 * anim)));
            }
            r.rect(right ? mc().scaledWidth() - 2 : 0, y - 1, 2, font.height() + 2, ColorUtil.fade(argb, anim));
            font.drawShadow(label, x, y, ColorUtil.fade(theme().text(), anim));

            y += (font.height() + 2) * anim;
            index++;
        }
    }

    private String label(Module module, boolean withTags) {
        String tag = module.tag();
        return withTags && tag != null && !tag.isEmpty() ? module.name() + " \u00a77" + tag : module.name();
    }

    private void renderInfo(IFontRenderer font) {
        List<String> lines = new ArrayList<String>();
        if (fps.get())   lines.add(mc().fps() + " fps");
        if (ping.get())  lines.add(mc().network().ping() + " ms");
        if (speed.get()) lines.add(String.format("%.1f b/s", MovementUtil.blocksPerSecond()));
        if (coords.get()) {
            lines.add(String.format("%.0f, %.0f, %.0f", player().x(), player().y(), player().z()));
        }
        double y = mc().scaledHeight() - 4 - lines.size() * (font.height() + 1);
        for (String line : lines) {
            font.drawShadow(line, 3, y, theme().text());
            y += font.height() + 1;
        }
    }
}
