package xyz.stormclient.command;

import xyz.stormclient.Storm;
import xyz.stormclient.StormInfo;
import xyz.stormclient.bridge.Bridge;

public abstract class Command {

    private final String name;
    private final String[] aliases;
    private final String usage;
    private final String description;

    protected Command(String name, String usage, String description, String... aliases) {
        this.name = name;
        this.usage = usage;
        this.description = description;
        this.aliases = aliases;
    }

    public String name()        { return name; }
    public String[] aliases()   { return aliases; }
    public String usage()       { return usage; }
    public String description() { return description; }

    public boolean matches(String input) {
        if (name.equalsIgnoreCase(input)) return true;
        for (String alias : aliases) if (alias.equalsIgnoreCase(input)) return true;
        return false;
    }

    /** @param args everything after the command name. */
    public abstract void execute(String[] args);

    protected void print(String message) { Bridge.mc().chat().print(message); }

    protected void error(String message) {
        Bridge.mc().chat().print("§c" + message);
    }

    protected void printUsage() {
        print("§7usage: §f" + Storm.get().commands().prefix() + usage);
    }

    protected String join(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    protected String prefix() { return StormInfo.CHAT_PREFIX; }
}
