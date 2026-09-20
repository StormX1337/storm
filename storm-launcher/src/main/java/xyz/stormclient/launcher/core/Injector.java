package xyz.stormclient.launcher.core;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Attaches the Storm agent to a running JVM.
 * Everything goes through reflection so the launcher still starts on a JRE
 * that has no attach provider, and can say so instead of crashing.
 */
public final class Injector {

    public static final class Result {
        public final boolean success;
        public final String message;

        Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private Injector() { }

    public static boolean available() {
        try {
            Class.forName("com.sun.tools.attach.VirtualMachine");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** The java.version of the target JVM, empty when it cannot be read. */
    public static String targetJavaVersion(GameProcess process) {
        if (process == null || !available()) return "";
        Object vm = null;
        try {
            Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            vm = vmClass.getMethod("attach", String.class).invoke(null, process.pid());
            java.util.Properties properties =
                    (java.util.Properties) vmClass.getMethod("getSystemProperties").invoke(vm);
            return properties.getProperty("java.version", "");
        } catch (Throwable t) {
            return "";
        } finally {
            detach(vm);
        }
    }

    private static int majorOf(String javaVersion) {
        if (javaVersion == null || javaVersion.isEmpty()) return 0;
        String[] parts = javaVersion.split("[._-]");
        try {
            int first = Integer.parseInt(parts[0]);
            if (first == 1 && parts.length > 1) return Integer.parseInt(parts[1]);
            return first;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static Result inject(GameProcess process, File agentJar, String options) {
        if (process == null) return new Result(false, "no process selected");
        if (agentJar == null || !agentJar.isFile()) {
            return new Result(false, "agent jar not found: " + agentJar);
        }
        if (!available()) {
            return new Result(false, "this JVM has no attach provider, run the launcher with a full JDK");
        }

        Object vm = null;
        try {
            Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            Method attach = vmClass.getMethod("attach", String.class);
            vm = attach.invoke(null, process.pid());

            String targetVersion = "";
            try {
                java.util.Properties properties =
                        (java.util.Properties) vmClass.getMethod("getSystemProperties").invoke(vm);
                targetVersion = properties.getProperty("java.version", "");
                Log.info("target runs Java " + targetVersion
                        + ", launcher runs Java " + System.getProperty("java.version"));
            } catch (Throwable ignored) {
                // some JVMs refuse the property read but still accept an agent
            }

            Method loadAgent = vmClass.getMethod("loadAgent", String.class, String.class);
            loadAgent.invoke(vm, agentJar.getAbsolutePath(), options);

            Log.info("injected into pid " + process.pid() + " (" + process.shortName() + ")");
            return new Result(true, "injected into " + process.shortName());
        } catch (Throwable t) {
            Throwable cause = t.getCause() == null ? t : t.getCause();
            Log.error("injection failed", cause);
            return new Result(false, describe(cause, process));
        } finally {
            detach(vm);
        }
    }

    private static void detach(Object vm) {
        if (vm == null) return;
        try {
            vm.getClass().getMethod("detach").invoke(vm);
        } catch (Throwable ignored) { }
    }

    private static String describe(Throwable t, GameProcess process) {
        String name = t.getClass().getSimpleName();
        String message = t.getMessage() == null ? "" : t.getMessage();

        if (name.contains("AgentLoad") || message.contains("Failed to load agent library")) {
            int launcher = majorOf(System.getProperty("java.version"));
            int target = majorOf(targetJavaVersion(process));

            Log.warn("the target JVM refused the agent."
                    + " launcher Java " + launcher + ", game Java " + (target == 0 ? "unknown" : target));
            Log.warn("the game's own console holds the real reason, look there for a stack trace");

            if (target > 0 && launcher > 0 && target != launcher) {
                return "the game runs Java " + target + ", this launcher runs Java " + launcher
                        + ". Attaching across versions often fails - use the JVM argument instead.";
            }
            return "the game refused the agent. Use the JVM argument instead, see below.";
        }
        if (name.contains("AttachNotSupported")) {
            return "the target JVM does not allow attaching (start it with -XX:+EnableDynamicAgentLoading)";
        }
        if (message.contains("Non-numeric")) return "invalid process id";
        if (message.toLowerCase().contains("permission") || message.contains("Operation not permitted")) {
            return "no permission to attach, run the launcher as the same user as the game";
        }
        return name + (message.isEmpty() ? "" : ": " + message);
    }

    /** The argument to paste into any launcher's JVM arguments box. */
    public static String jvmArgument(File agentJar, String options) {
        String path = agentJar == null ? "storm-agent.jar" : agentJar.getAbsolutePath();
        return "-javaagent:" + path + (options == null || options.isEmpty() ? "" : "=" + options);
    }

    /** The option string handed to the agent. */
    public static String buildOptions(String version, String configProfile, File bridgeJar, boolean debug) {
        StringBuilder sb = new StringBuilder();
        sb.append("version=").append(version);
        sb.append(";config=").append(configProfile);
        if (bridgeJar != null && bridgeJar.isFile()) sb.append(";bridge=").append(bridgeJar.getAbsolutePath());
        if (debug) sb.append(";debug=true");
        return sb.toString();
    }
}
