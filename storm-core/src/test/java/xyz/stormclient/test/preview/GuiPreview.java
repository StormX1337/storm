package xyz.stormclient.test.preview;

import java.awt.Color;
import java.io.File;

import javax.imageio.ImageIO;

import xyz.stormclient.Storm;
import xyz.stormclient.StormBoot;
import xyz.stormclient.module.Module;
import xyz.stormclient.test.HeadlessGame;
import xyz.stormclient.ui.click.ClickGuiScreen;

/** Renders the real click GUI into a PNG so its layout can be inspected. */
public class GuiPreview {

    public static void main(String[] args) throws Exception {
        int width = Integer.parseInt(args[0]);
        int height = Integer.parseInt(args[1]);
        File out = new File(args[2]);
        String mode = args.length > 3 ? args[3] : "menu";
        boolean hud = "hud".equals(mode);
        if (mode.startsWith("menu")) { /* page chosen below */ }

        File dir = new File(System.getProperty("java.io.tmpdir"), "storm-preview");
        dir.mkdirs();
        HeadlessGame game = new HeadlessGame(dir);
        StormBoot.boot(game);

        ImageRenderer renderer = new ImageRenderer(width, height);
        game.setRenderer(renderer);
        game.setFont(new ImageFont(renderer.graphics(), 9));
        game.setFonts(new HeadlessGame.FontFactory() {
            private final java.util.Map<Integer, ImageFont> cache =
                    new java.util.HashMap<Integer, ImageFont>();
            public xyz.stormclient.bridge.IFontRenderer font(String name, int size) {
                ImageFont existing = cache.get(size);
                if (existing == null) {
                    existing = new ImageFont(renderer.graphics(), size);
                    cache.put(size, existing);
                }
                return existing;
            }
        });

        // a handful of modules on, and one module expanded, to exercise every row state
        int on = 0;
        for (Module module : Storm.get().modules().all()) {
            if (on < 6 && module.settings().size() > 2) { module.setEnabled(true); on++; }
        }
        for (int i = 0; i < 80; i++) Storm.get().modules().all().get(0).animation.get();

        if (hud) {
            for (Module module : Storm.get().modules().byCategory(
                    xyz.stormclient.module.Category.HUD)) {
                module.setEnabled(true);
            }
            for (int i = 0; i < 90; i++) {
                Storm.get().bus().post(new xyz.stormclient.event.events.TickEvent.Pre());
            }
            backdrop(renderer, width, height);
            Storm.get().bus().post(new xyz.stormclient.event.events.RenderEvent.Hud(1F));
            ImageIO.write(renderer.image, "png", out);
            System.out.println("wrote " + out + "  " + width + "x" + height + " (hud)");
            System.exit(0);
        }

        xyz.stormclient.ui.Screen screen;
        if ("panels".equals(mode)) {
            ClickGuiScreen panels = new ClickGuiScreen();
            panels.panels().get(0).buttons().get(1).setExpanded(true);
            screen = panels;
        } else {
            xyz.stormclient.ui.menu.MenuScreen menu = new xyz.stormclient.ui.menu.MenuScreen();
            // mode picks which page the picture shows: menu, menu:Theme, menu:Configs, ...
            int colon = mode.indexOf(':');
            String page = colon < 0 ? "Combat" : mode.substring(colon + 1);
            xyz.stormclient.ui.menu.Page opened = menu.select(page);
            if (opened instanceof xyz.stormclient.ui.menu.ModulesPage) {
                ((xyz.stormclient.ui.menu.ModulesPage) opened).expand(
                        Storm.get().modules().byCategory(
                                xyz.stormclient.module.Category.valueOf(page.toUpperCase()))
                                .get(0).name());
            }
            screen = menu;
        }
        screen.onOpen(width, height);

        // The menu is translucent, so stacking frames would paint it opaque.
        // Settle the animations first, then draw one frame over the backdrop.
        for (int i = 0; i < 90; i++) screen.render(-1, -1, 1F);
        backdrop(renderer, width, height);
        screen.render(-1, -1, 1F);

        ImageIO.write(renderer.image, "png", out);
        System.out.println("wrote " + out + "  " + width + "x" + height);
        System.exit(0);
    }

    /** A rough sky and grass, so the menu's translucency shows the way it will in game. */
    private static void backdrop(ImageRenderer renderer, int width, int height) {
        renderer.graphics().setColor(new Color(0x4C7A3F));
        renderer.graphics().fillRect(0, 0, width, height);
        renderer.graphics().setColor(new Color(0x7FB2E5));
        renderer.graphics().fillRect(0, 0, width, height / 3);
    }

}
