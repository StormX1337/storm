package xyz.stormclient.launcher.ui.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import xyz.stormclient.launcher.core.GameLauncher;
import xyz.stormclient.launcher.core.Injector;
import xyz.stormclient.launcher.core.LauncherConfig;
import xyz.stormclient.launcher.core.Log;
import xyz.stormclient.launcher.core.MinecraftVersion;
import xyz.stormclient.launcher.core.ProfileInstaller;
import xyz.stormclient.launcher.core.VersionRegistry;
import xyz.stormclient.launcher.ui.StormButton;
import xyz.stormclient.launcher.ui.StormTheme;
import xyz.stormclient.launcher.ui.UiKit;

import java.io.File;

/** Version picker plus the launch button. */
public final class HomePanel extends BasePanel {

    private final LauncherConfig config;
    private final VersionGrid grid;
    private final StormButton launch;
    private final StormButton profile;

    public HomePanel(LauncherConfig config) {
        super("Play", "Pick a version, Storm attaches itself while the game starts");
        this.config = config;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(headerHeight() + 16, 30, 24, 30));

        grid = new VersionGrid(config.version(), this::onVersionSelected);
        JPanel gridHolder = new JPanel(new BorderLayout());
        gridHolder.setOpaque(false);
        gridHolder.add(grid, BorderLayout.CENTER);
        add(gridHolder, BorderLayout.CENTER);

        launch = new StormButton("LAUNCH", StormButton.Style.PRIMARY, this::onLaunch);
        launch.setPreferredSize(new Dimension(190, 46));

        profile = new StormButton("Add to official launcher", StormButton.Style.GHOST, this::onInstallProfile);
        profile.setPreferredSize(new Dimension(210, 46));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.setPreferredSize(new Dimension(0, 60));
        actions.add(profile);
        actions.add(launch);
        add(actions, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        MinecraftVersion version = VersionRegistry.byId(config.version());
        launch.setSubLabel(version.id() + "  ·  " + config.username());
        launch.setEnabledState(version.playable());
        repaint();
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
        options.bridgeJar = new File(options.agentJar.getParentFile(), version.bridgeJarName());
        options.ram = config.ram();
        options.debug = config.debug();
        options.agentOptions = Injector.buildOptions(version.id(), config.configProfile(),
                options.bridgeJar, config.debug());

        launch.setEnabledState(false);
        launch.setSubLabel("starting...");

        new Thread(() -> {
            try {
                GameLauncher.launch(options, Log::info);
                SwingUtilities.invokeLater(() -> {
                    launch.setSubLabel("running");
                    if (config.closeOnLaunch()) System.exit(0);
                });
            } catch (Exception e) {
                Log.error("launch failed", e);
                SwingUtilities.invokeLater(() -> {
                    launch.setEnabledState(true);
                    launch.setSubLabel(e.getMessage() == null ? "failed" : shorten(e.getMessage()));
                });
            }
        }, "Storm-Launch").start();
    }

    private void onInstallProfile() {
        MinecraftVersion version = VersionRegistry.byId(config.version());
        File agent = new File(config.agentJar());
        File bridge = new File(agent.getParentFile(), version.bridgeJarName());
        String options = Injector.buildOptions(version.id(), config.configProfile(), bridge, config.debug());

        boolean ok = ProfileInstaller.install(new File(config.gameDirectory()), version.id(), agent, options);
        profile.setSubLabel(ok ? "profile written" : "could not write profile");
    }

    private String shorten(String message) {
        return message.length() > 46 ? message.substring(0, 43) + "..." : message;
    }

    @Override protected void paintBody(Graphics2D g) {
        g.setFont(StormTheme.font(12));
        String hint = Injector.available()
                ? "attach API ready"
                : "no attach API - run the launcher with a JDK to use injection";
        UiKit.textRight(g, hint, getWidth() - 30, 44,
                Injector.available() ? StormTheme.GREEN : StormTheme.AMBER);
        UiKit.textRight(g, config.gameDirectory(), getWidth() - 30, 63, StormTheme.TEXT_FAINT);
    }
}
