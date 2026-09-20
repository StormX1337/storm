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

            Method loadAgent = vmClass.getMethod("loadAgent", String.class, String.class);
            loadAgent.invoke(vm, agentJar.getAbsolutePath(), options);

            Log.info("injected into pid " + process.pid() + " (" + process.shortName() + ")");
            return new Result(true, "injected into " + process.shortName());
        } catch (Throwable t) {
            Throwable cause = t.getCause() == null ? t : t.getCause();
            Log.error("injection failed", cause);
            return new Result(false, describe(cause));
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

    private static String describe(Throwable t) {
        String name = t.getClass().getSimpleName();
        String message = t.getMessage() == null ? "" : t.getMessage();

        if (name.contains("AttachNotSupported")) {
            return "the target JVM does not allow attaching (start it with -XX:+EnableDynamicAgentLoading)";
        }
        if (message.contains("Non-numeric")) return "invalid process id";
        if (message.toLowerCase().contains("permission") || message.contains("Operation not permitted")) {
            return "no permission to attach, run the launcher as the same user as the game";
        }
        return name + (message.isEmpty() ? "" : ": " + message);
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
