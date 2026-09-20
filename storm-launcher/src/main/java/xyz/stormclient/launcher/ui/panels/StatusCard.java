package xyz.stormclient.launcher.ui.panels;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import xyz.stormclient.launcher.core.GameDirectories;
import xyz.stormclient.launcher.core.Injector;
import xyz.stormclient.launcher.core.LauncherConfig;
import xyz.stormclient.launcher.core.MinecraftVersion;
import xyz.stormclient.launcher.core.VersionRegistry;
import xyz.stormclient.launcher.ui.Icons;
import xyz.stormclient.launcher.ui.StormTheme;
import xyz.stormclient.launcher.ui.UiKit;

/**
 * Says in plain words what will happen when Launch is pressed, and what is
 * missing when it will not work. Three checks, each with its own remedy.
 */
public final class StatusCard extends JComponent {

    private static final class Row {
        final String label;
        final String detail;
        final Color color;
        final boolean ok;

        Row(String label, String detail, Color color, boolean ok) {
            this.label = label;
            this.detail = detail;
            this.color = color;
            this.ok = ok;
        }
    }

    private final LauncherConfig config;
    private List<Row> rows = new ArrayList<>();
    private String headline = "";

    public StatusCard(LauncherConfig config) {
        this.config = config;
        setOpaque(false);
        setPreferredSize(new Dimension(0, 124));
        refresh();
    }

    public void refresh() {
        MinecraftVersion version = VersionRegistry.byId(config.version());
        File gameDir = new File(config.gameDirectory());
        File agent = new File(config.agentJar());
        File bridge = new File(agent.getParentFile(), version.bridgeJarName());

        rows = new ArrayList<>();

        boolean installedHere = GameDirectories.canLaunchFrom(gameDir, version.id());
        String elsewhere = GameDirectories.describeAlternatives(version.id());
        rows.add(new Row("Game files",
                installedHere ? "found in " + gameDir.getName()
                              : elsewhere.isEmpty() ? "not installed anywhere Storm can see"
                                                    : "in " + elsewhere + ", not in " + gameDir.getName(),
                installedHere ? StormTheme.GREEN : StormTheme.AMBER, installedHere));

        boolean hasBridge = bridge.isFile();
        rows.add(new Row("Bridge for " + version.id(),
                hasBridge ? bridge.getName() : bridge.getName() + " missing, build it first",
                hasBridge ? StormTheme.GREEN : StormTheme.RED, hasBridge));

        boolean attach = Injector.available();
        rows.add(new Row("Attach API",
                attach ? "ready, injection works" : "missing, start the launcher with a JDK",
                attach ? StormTheme.GREEN : StormTheme.AMBER, attach));

        if (!hasBridge) {
            headline = "The game will start, but Storm will not load yet";
        } else if (!installedHere && !elsewhere.isEmpty()) {
            headline = "Start " + version.id() + " in " + elsewhere + ", then use Inject";
        } else if (!installedHere) {
            headline = "Install " + version.id() + " first";
        } else {
            headline = "Ready: Launch starts " + version.id() + " with Storm attached";
        }
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = UiKit.prepare((Graphics2D) graphics.create());
        int w = getWidth(), h = getHeight();
        if (w <= 0) { g.dispose(); return; }

        UiKit.fillRound(g, 0, 0, w, h, 13, StormTheme.alpha(StormTheme.PANEL, 190));
        UiKit.drawRound(g, 0, 0, w, h, 13, 1F, StormTheme.alpha(StormTheme.OUTLINE, 160));

        boolean allOk = rows.stream().allMatch(row -> row.ok);
        Color headlineColor = allOk ? StormTheme.GREEN : StormTheme.TEXT;

        g.setFont(StormTheme.bold(14));
        UiKit.text(g, headline, 18, 28, headlineColor);

        double y = 52;
        for (Row row : rows) {
            UiKit.statusDot(g, 20, y - 6, 7, row.color);

            g.setFont(StormTheme.font(12));
            UiKit.text(g, row.label, 38, y, StormTheme.TEXT);

            g.setFont(StormTheme.font(11));
            UiKit.text(g, row.detail, 170, y, StormTheme.TEXT_DIM);

            if (row.ok) Icons.draw(g, Icons.Kind.CHECK, w - 32, y - 11, 13, row.color);
            y += 24;
        }
        g.dispose();
    }
}
