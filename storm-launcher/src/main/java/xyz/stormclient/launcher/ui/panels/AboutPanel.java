package xyz.stormclient.launcher.ui.panels;

import java.awt.Graphics2D;

import xyz.stormclient.launcher.StormLauncher;
import xyz.stormclient.launcher.ui.StormTheme;
import xyz.stormclient.launcher.ui.UiKit;

public final class AboutPanel extends BasePanel {

    private static final String[] LINES = {
            "Storm is written from scratch: own event bus, own module system,",
            "own rendering layer and own interface. No code, assets or logos from",
            "any other client are used, and nothing here touches licensing or DRM.",
            "",
            "The client core knows nothing about Minecraft. Everything goes through",
            "the bridge interfaces in xyz.stormclient.bridge, which is why one build",
            "can drive several game versions.",
            "",
            "Play  -  starts a version with the agent already on the command line.",
            "Inject  -  attaches the agent to a game that is already running.",
    };

    public AboutPanel() {
        super("About", "Storm Client · xyz.stormclient");
    }

    @Override protected void paintBody(Graphics2D g) {
        UiKit.bolt(g, 30, 110, 58, StormTheme.ACCENT);

        g.setFont(StormTheme.bold(30));
        UiKit.text(g, "STORM", 104, 140, StormTheme.TEXT);
        g.setFont(StormTheme.font(13));
        UiKit.text(g, "client 1.0.0  ·  launcher " + StormLauncher.VERSION, 104, 160, StormTheme.ACCENT);

        g.setFont(StormTheme.font(13));
        int y = 214;
        for (String line : LINES) {
            UiKit.text(g, line, 30, y, line.isEmpty() ? StormTheme.TEXT_DIM : StormTheme.TEXT_DIM);
            y += 20;
        }

        g.setFont(StormTheme.font(11));
        UiKit.text(g, "Use it where you are allowed to. Many servers forbid clients like this.",
                30, getHeight() - 34, StormTheme.AMBER);
    }
}
