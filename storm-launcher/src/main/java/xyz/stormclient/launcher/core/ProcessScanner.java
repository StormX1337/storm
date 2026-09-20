package xyz.stormclient.launcher.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds running Minecraft instances.
 * The Attach API gives the nicest names, {@link ProcessHandle} is the fallback
 * for JVMs that do not show up there.
 */
public final class ProcessScanner {

    private static final Pattern VERSION = Pattern.compile("(?:--version|versions[/\\\\])\\s*([0-9]+\\.[0-9]+(?:\\.[0-9]+)?)");

    private ProcessScanner() { }

    public static List<GameProcess> scan() {
        List<GameProcess> found = fromAttachApi();
        if (found.isEmpty()) found = fromProcessHandle();
        return found;
    }

    @SuppressWarnings("unchecked")
    private static List<GameProcess> fromAttachApi() {
        List<GameProcess> out = new ArrayList<>();
        try {
            Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            Method list = vmClass.getMethod("list");
            List<Object> descriptors = (List<Object>) list.invoke(null);

            String self = String.valueOf(ProcessHandle.current().pid());
            for (Object descriptor : descriptors) {
                String id = (String) descriptor.getClass().getMethod("id").invoke(descriptor);
                String name = (String) descriptor.getClass().getMethod("displayName").invoke(descriptor);
                if (id.equals(self)) continue;

                String command = commandLine(id);
                boolean isMinecraft = looksLikeMinecraft(name) || looksLikeMinecraft(command);
                out.add(new GameProcess(id, name, guessVersion(name + " " + command), isMinecraft));
            }
        } catch (Throwable t) {
            Log.warn("attach API unavailable (" + t.getClass().getSimpleName() + "), falling back to process scan");
        }
        return out;
    }

    private static List<GameProcess> fromProcessHandle() {
        List<GameProcess> out = new ArrayList<>();
        long self = ProcessHandle.current().pid();

        ProcessHandle.allProcesses().forEach(handle -> {
            if (handle.pid() == self) return;
            Optional<String> command = handle.info().command();
            String line = handle.info().commandLine().orElse(command.orElse(""));
            if (line.isEmpty()) return;
            if (!line.contains("java")) return;
            if (!looksLikeMinecraft(line)) return;

            out.add(new GameProcess(String.valueOf(handle.pid()), line, guessVersion(line), true));
        });
        return out;
    }

    private static String commandLine(String pid) {
        try {
            Optional<ProcessHandle> handle = ProcessHandle.of(Long.parseLong(pid));
            if (handle.isPresent()) return handle.get().info().commandLine().orElse("");
        } catch (Throwable ignored) { }
        return "";
    }

    private static boolean looksLikeMinecraft(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        return lower.contains("net.minecraft")
                || lower.contains("minecraft.client")
                || lower.contains("launchwrapper")
                || lower.contains("knot")                 // Fabric
                || lower.contains("cpw.mods")             // Forge
                || lower.contains(".minecraft");
    }

    private static String guessVersion(String text) {
        if (text == null) return "";
        Matcher matcher = VERSION.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }
}
