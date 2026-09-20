package xyz.stormclient.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

import xyz.stormclient.bridge.GameVersion;
import xyz.stormclient.util.StormLogger;

/**
 * JVM agent entry point.
 *
 * <p>Works both ways round:
 * <ul>
 *   <li>{@code premain}  - the launcher started Minecraft with {@code -javaagent:storm-agent.jar}</li>
 *   <li>{@code agentmain} - the injection menu attached to an already running game</li>
 * </ul>
 *
 * <p>The agent itself contains no game code. It works out which Minecraft is
 * running, puts the matching bridge on the class path and asks that bridge to
 * install itself. Everything after that happens inside storm-core.
 */
public final class StormAgent {

    private static volatile boolean loaded;

    private StormAgent() { }

    public static void premain(String args, Instrumentation instrumentation) {
        bootstrap(args, instrumentation, "premain");
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        bootstrap(args, instrumentation, "agentmain");
    }

    private static synchronized void bootstrap(String rawArgs, Instrumentation instrumentation, String how) {
        if (loaded) {
            StormLogger.warn("agent already loaded, ignoring second " + how);
            return;
        }
        loaded = true;

        AgentOptions options = AgentOptions.parse(rawArgs);
        StormLogger.setDebug(options.debug());
        StormLogger.info("Storm agent attached via " + how + " " + options);

        try {
            appendBridge(instrumentation, options);

            GameVersion version = VersionDetector.detect(options.forcedVersion());
            StormLogger.info("detected Minecraft " + version.id());

            if (version == GameVersion.UNKNOWN) {
                StormLogger.error("could not identify this Minecraft build, "
                        + "start the launcher with an explicit version or pass version=<id>");
                return;
            }
            if (!BridgeLoader.load(version, options)) {
                StormLogger.error("Storm did not load. The game runs normally, without the client.");
            }
        } catch (Throwable t) {
            StormLogger.error("agent bootstrap failed", t);
        }
    }

    /** Adds the bridge jar to the system class path when the launcher passed one. */
    private static void appendBridge(Instrumentation instrumentation, AgentOptions options) throws Exception {
        if (instrumentation == null || options.bridgeJar() == null) return;

        File jar = new File(options.bridgeJar());
        if (!jar.isFile()) {
            StormLogger.warn("bridge jar " + jar + " does not exist, relying on the class path");
            return;
        }
        instrumentation.appendToSystemClassLoaderSearch(new JarFile(jar));
        StormLogger.debug("appended " + jar.getName() + " to the system class path");
    }

    public static boolean loaded() { return loaded; }
}
