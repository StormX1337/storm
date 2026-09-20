package xyz.stormclient.launcher;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import xyz.stormclient.launcher.core.LauncherConfig;
import xyz.stormclient.launcher.core.Log;
import xyz.stormclient.launcher.ui.LauncherFrame;

/**
 * Storm Launcher - version picker, game starter and injection menu in one window.
 *
 * <p>Two ways to get Storm into the game:
 * <ul>
 *   <li><b>Launch</b> starts Minecraft with {@code -javaagent:storm-agent.jar}</li>
 *   <li><b>Inject</b> attaches to a Minecraft that is already running</li>
 * </ul>
 */
public final class StormLauncher {

    public static final String NAME    = "Storm Launcher";
    public static final String VERSION = "1.0.0";

    private StormLauncher() { }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            Log.warn("could not set the look and feel: " + e);
        }

        LauncherConfig config = LauncherConfig.load();
        Log.info(NAME + " " + VERSION + " starting");

        SwingUtilities.invokeLater(() -> new LauncherFrame(config).setVisible(true));
    }
}
