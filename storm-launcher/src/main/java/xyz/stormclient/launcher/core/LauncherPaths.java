package xyz.stormclient.launcher.core;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Where the launcher's own files are.
 *
 * <p>The build writes the jars into {@code dist/}, but people start the
 * launcher from the repository root just as often as from inside dist, so the
 * agent is looked for next to the running jar first and then in the obvious
 * places around the working directory.
 */
public final class LauncherPaths {

    private LauncherPaths() { }

    /** The folder the running launcher jar sits in, or the working directory. */
    public static File ownDirectory() {
        try {
            URL location = LauncherPaths.class.getProtectionDomain().getCodeSource().getLocation();
            File self = new File(location.toURI());
            File directory = self.isDirectory() ? self : self.getParentFile();
            if (directory != null && directory.isDirectory()) return directory;
        } catch (URISyntaxException | RuntimeException ignored) {
            // started in a way that hides the code source, fall through
        }
        return new File(System.getProperty("user.dir", "."));
    }

    /** First existing candidate, or the most likely path when none exists yet. */
    public static File defaultAgentJar() {
        File own = ownDirectory();
        File working = new File(System.getProperty("user.dir", "."));

        File[] candidates = {
                new File(own, "storm-agent.jar"),
                new File(own, "dist/storm-agent.jar"),
                new File(own.getParentFile() == null ? own : own.getParentFile(), "storm-agent.jar"),
                new File(working, "dist/storm-agent.jar"),
                new File(working, "storm-agent.jar")
        };
        for (File candidate : candidates) {
            if (candidate.isFile()) return candidate.getAbsoluteFile();
        }
        return new File(own, "storm-agent.jar").getAbsoluteFile();
    }

    /** The bridge jar belonging to a version, looked for beside the agent. */
    public static File bridgeJar(File agentJar, String jarName) {
        File beside = agentJar == null || agentJar.getParentFile() == null
                ? new File(jarName)
                : new File(agentJar.getParentFile(), jarName);
        if (beside.isFile()) return beside;

        File inDist = new File(ownDirectory(), "dist/" + jarName);
        return inDist.isFile() ? inDist : beside;
    }
}
