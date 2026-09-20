package xyz.stormclient.command;

import java.util.ArrayList;
import java.util.List;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.ChatEvent;
import xyz.stormclient.util.StormLogger;

public final class CommandManager {

    private final List<Command> commands = new ArrayList<Command>();
    private String prefix = ".";

    public void init() {
        commands.add(new Commands.Help());
        commands.add(new Commands.Toggle());
        commands.add(new Commands.Bind());
        commands.add(new Commands.Config());
        commands.add(new Commands.Friend());
        commands.add(new Commands.Hud());
        commands.add(new Commands.ThemeCommand());
        commands.add(new Commands.Panic());
        commands.add(new Commands.Prefix());
        commands.add(new Commands.Say());
        commands.add(new Commands.Reset());
        commands.add(new Commands.Info());
        StormLogger.info("registered " + commands.size() + " commands");
    }

    public List<Command> commands() { return commands; }

    public String prefix() { return prefix; }
    public void setPrefix(String prefix) {
        if (prefix != null && !prefix.isEmpty()) this.prefix = prefix;
    }

    public Command byName(String name) {
        for (Command c : commands) if (c.matches(name)) return c;
        return null;
    }

    @Subscribe
    public void onChat(ChatEvent.Send event) {
        String message = event.message();
        if (!message.startsWith(prefix)) return;
        event.cancel();
        dispatch(message.substring(prefix.length()));
    }

    public void dispatch(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return;

        Command command = byName(parts[0]);
        if (command == null) {
            xyz.stormclient.bridge.Bridge.mc().chat().print(
                    "§cunknown command §f" + parts[0] + "§c, try " + prefix + "help");
            return;
        }
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        try {
            command.execute(args);
        } catch (Throwable t) {
            StormLogger.error("command " + command.name() + " failed", t);
            xyz.stormclient.bridge.Bridge.mc().chat().print("§ccommand failed: " + t);
        }
    }
}
