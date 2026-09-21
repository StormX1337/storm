package xyz.stormclient.launcher.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.io.File;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import xyz.stormclient.launcher.core.GameDirectories;
import xyz.stormclient.launcher.core.GameLauncher;
import xyz.stormclient.launcher.core.Injector;
import xyz.stormclient.launcher.core.LauncherConfig;
import xyz.stormclient.launcher.core.LicenceClient;
import xyz.stormclient.launcher.core.Log;
import xyz.stormclient.launcher.core.MinecraftVersion;
import xyz.stormclient.launcher.core.ProfileInstaller;
import xyz.stormclient.launcher.core.VersionRegistry;
import xyz.stormclient.launcher.ui.Icons;
import xyz.stormclient.launcher.ui.StormButton;
import xyz.stormclient.launcher.ui.StormTheme;
import xyz.stormclient.launcher.ui.UiKit;

/** Version picker plus the launch button. */
public final class HomePanel extends BasePanel {

    private final LauncherConfig config;
    private final VersionGrid grid;
    private final StatusCard statusCard;
    private final StormButton launch;
    private final StormButton profile;

    private String status = "";
    private Color statusColor = StormTheme.TEXT_FAINT;

    public HomePanel(LauncherConfig config) {
        super("Play", "Pick a version, Storm attaches itself while the game starts");
        this.config = config;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(headerHeight() + 18, 30, 24, 30));

        grid = new VersionGrid(config.version(), this::onVersionSelected);
        JPanel gridHolder = new JPanel(new BorderLayout());
        gridHolder.setOpaque(false);
        gridHolder.add(grid, BorderLayout.NORTH);
        add(gridHolder, BorderLayout.NORTH);

        statusCard = new StatusCard(config);

        launch = new StormButton("LAUNCH", StormButton.Style.PRIMARY, this::onLaunch);
        launch.setIcon(Icons.Kind.PLAY);
        launch.setPreferredSize(new Dimension(200, 48));

        profile = new StormButton("Add to official launcher", StormButton.Style.GHOST, this::onInstallProfile);
        profile.setPreferredSize(new Dimension(216, 48));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.setPreferredSize(new Dimension(0, 66));
        actions.add(profile);
        actions.add(launch);

        // status card and buttons share the bottom, so the card can never be
        // squeezed out by however many rows of version cards there are
        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        bottom.add(statusCard, BorderLayout.CENTER);
        bottom.add(actions, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        MinecraftVersion version = VersionRegistry.byId(config.version());
        File gameDir = new File(config.gameDirectory());

        Set<String> installed = GameDirectories.installedIn(gameDir);
        grid.setInstalled(installed);
        if (statusCard != null) statusCard.refresh();

        // only the official launcher has a profiles file to add to
        File profiles = new File(gameDir, "launcher_profiles.json");
        profile.setEnabledState(profiles.isFile());
        profile.setSubLabel(profiles.isFile() ? "" : "official launcher only");

        boolean launchable = GameDirectories.canLaunchFrom(gameDir, version.id());
        launch.setEnabledState(version.playable() && launchable);
        launch.setSubLabel(version.id() + "  \u00b7  " + config.username());

        repaint();
    }

    private void setStatus(String text, Color color) {
        this.status = text;
        this.statusColor = color;
    }

    /**
     * The licence to start the game with, refreshed against the server first.
     *
     * <p>Refreshing on every launch is what makes a revoked key stop working:
     * the signed licence only lasts days, so the server gets a say without the
     * client ever needing it to be up. A server that cannot be reached is not a
     * reason to keep a paying customer out, so the cached licence is used; a
     * server that answers and says no clears it.
     */
    private String currentLicence() {
        if (config.licenceServer().isEmpty() || config.licenceKey().isEmpty()) {
            return config.licence();
        }
        LicenceClient.Result result =
                LicenceClient.activate(config.licenceServer(), config.licenceKey());
        if (result.ok()) {
            config.setLicence(result.licence);
            config.save();
            Log.info("licence refreshed for " + result.holder);
            return result.licence;
        }
        if (result.refused) {
            config.setLicence("");
            config.save();
            Log.warn("licence refused: " + result.error);
            return "";
        }
        Log.warn("could not refresh the licence, using the stored one: " + result.error);
        return config.licence();
    }

    private void onVersionSelected(MinecraftVersion version) {
        config.setVersion(version.id());
        config.save();
        refresh();
    }

    private void onLaunch() {
        MinecraftVersion version = VersionRegistry.byId(config.version());

        GameLauncher.Options options = new GameLauncher.Options();
        options.version = version.id();
        options.username = config.username();
        options.gameDir = new File(config.gameDirectory());
        options.javaPath = new File(config.javaPath());
        options.agentJar = new File(config.agentJar());
        options.bridgeJar = xyz.stormclient.launcher.core.LauncherPaths.bridgeJar(options.agentJar, version.bridgeJarName());
        options.ram = config.ram();
        options.debug = config.debug();
        options.agentOptions = Injector.buildOptions(version.id(), config.configProfile(),
                options.bridgeJar, config.debug());
        launch.setLoading(true);
        setStatus("starting " + version.id() + "...", StormTheme.TEXT_DIM);
        repaint();

        new Thread(() -> {
            try {
                options.licence = currentLicence();
                GameLauncher.launch(options, Log::info);
                SwingUtilities.invokeLater(() -> {
                    launch.setLoading(false);
                    launch.setSubLabel("running");
                    setStatus("game started", StormTheme.GREEN);
                    repaint();
                    if (config.closeOnLaunch()) System.exit(0);
                });
            } catch (Exception e) {
                Log.error("launch failed", e);
                SwingUtilities.invokeLater(() -> {
                    launch.setLoading(false);
                    refresh();
                    setStatus(e.getMessage() == null ? "launch failed" : e.getMessage(), StormTheme.RED);
                    repaint();
                });
            }
        }, "Storm-Launch").start();
    }

    private void onInstallProfile() {
        MinecraftVersion version = VersionRegistry.byId(config.version());
        File agent = new File(config.agentJar());
        File bridge = xyz.stormclient.launcher.core.LauncherPaths.bridgeJar(agent, version.bridgeJarName());
        String options = Injector.buildOptions(version.id(), config.configProfile(), bridge, config.debug());

        boolean ok = ProfileInstaller.install(new File(config.gameDirectory()), version.id(), agent, options);
        setStatus(ok ? "profile written into the official launcher" : "could not write the profile",
                ok ? StormTheme.GREEN : StormTheme.RED);
        repaint();
    }

    @Override protected void paintBody(Graphics2D g) {
        int right = getWidth() - 30;

        boolean attach = Injector.available();
        String attachText = attach ? "attach API ready" : "no attach API, use a JDK";
        chip(g, right, 30, attachText, attach ? StormTheme.GREEN : StormTheme.AMBER);

        g.setFont(StormTheme.font(11));
        File gameDir = new File(config.gameDirectory());
        int count = GameDirectories.installedIn(gameDir).size();
        UiKit.textRight(g, gameDir.getName() + "  \u00b7  " + count + " versions installed",
                right, 64, StormTheme.TEXT_FAINT);

        if (!status.isEmpty()) {
            g.setFont(StormTheme.font(12));
            UiKit.text(g, status, 30, getHeight() - 24, statusColor);
        }
    }

    /** Small rounded pill with a status dot. */
    private void chip(Graphics2D g, double right, double y, String text, Color color) {
        g.setFont(StormTheme.font(11));
        double textWidth = g.getFontMetrics().stringWidth(text);
        double width = textWidth + 30;
        double x = right - width;

        UiKit.fillRound(g, x, y, width, 22, 11, StormTheme.alpha(color, 26));
        UiKit.statusDot(g, x + 10, y + 8, 6, color);
        UiKit.text(g, text, x + 24, y + 15, color);
    }
}
