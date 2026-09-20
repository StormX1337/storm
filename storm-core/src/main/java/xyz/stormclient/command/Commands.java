package xyz.stormclient.command;

import java.util.List;

import xyz.stormclient.Storm;
import xyz.stormclient.StormInfo;
import xyz.stormclient.bridge.Bridge;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.Setting;
import xyz.stormclient.ui.theme.Theme;
import xyz.stormclient.util.ColorUtil;
import xyz.stormclient.util.Keyboard;

public final class Commands {

    private Commands() { }

    // ------------------------------------------------------------------
    public static final class Help extends Command {

        public Help() { super("help", "help [command]", "Lists every command", "?", "commands"); }

        @Override public void execute(String[] args) {
            if (args.length > 0) {
                Command command = Storm.get().commands().byName(args[0]);
                if (command == null) { error("no command called " + args[0]); return; }
                print("\u00a7f" + command.name() + " \u00a77- " + command.description());
                printUsageOf(command);
                return;
            }
            print("\u00a7f" + StormInfo.FULL_NAME + " \u00a77commands");
            for (Command command : Storm.get().commands().commands()) {
                Bridge.mc().chat().printRaw("  \u00a7b" + Storm.get().commands().prefix() + command.name()
                        + " \u00a78- \u00a77" + command.description());
            }
        }

        private void printUsageOf(Command command) {
            Bridge.mc().chat().printRaw("  \u00a77" + Storm.get().commands().prefix() + command.usage());
        }
    }

    // ------------------------------------------------------------------
    public static final class Toggle extends Command {

        public Toggle() { super("toggle", "toggle <module> [setting] [value]", "Toggles a module or changes a setting", "t"); }

        @Override public void execute(String[] args) {
            if (args.length == 0) { printUsage(); return; }

            Module module = Storm.get().modules().byName(args[0]);
            if (module == null) {
                List<Module> found = Storm.get().modules().search(args[0]);
                if (found.isEmpty()) { error("no module called " + args[0]); return; }
                module = found.get(0);
            }
            if (args.length == 1) {
                module.toggle();
                print(module.name() + " \u00a77is now " + (module.isEnabled() ? "\u00a7aon" : "\u00a7coff"));
                return;
            }

            Setting<?> setting = module.setting(args[1]);
            if (setting == null) { error(module.name() + " has no setting " + args[1]); return; }
            if (args.length == 2) {
                print(module.name() + " \u00a77" + setting.name() + " \u00a7f= " + setting.display());
                return;
            }
            setting.deserialize(join(args, 2));
            print(module.name() + " \u00a77" + setting.name() + " \u00a7f-> " + setting.display());
        }
    }

    // ------------------------------------------------------------------
    public static final class Bind extends Command {

        public Bind() { super("bind", "bind <module> <key|none>", "Binds a module to a key", "b"); }

        @Override public void execute(String[] args) {
            if (args.length < 2) { printUsage(); return; }
            Module module = Storm.get().modules().byName(args[0]);
            if (module == null) { error("no module called " + args[0]); return; }

            if (args[1].equalsIgnoreCase("none")) {
                module.setKeybind(Keyboard.KEY_NONE);
                print(module.name() + " \u00a77unbound");
                return;
            }
            int key = Keyboard.code(args[1]);
            if (key == Keyboard.KEY_NONE) { error("unknown key " + args[1]); return; }
            module.setKeybind(key);
            print(module.name() + " \u00a77bound to \u00a7f" + Keyboard.name(key));
        }
    }

    // ------------------------------------------------------------------
    public static final class Config extends Command {

        public Config() { super("config", "config <save|load|list|new|delete> [name]", "Manages your configs", "cfg"); }

        @Override public void execute(String[] args) {
            if (args.length == 0) { printUsage(); return; }
            xyz.stormclient.config.ConfigManager config = Storm.get().config();
            String name = args.length > 1 ? args[1] : config.currentName();

            if (args[0].equalsIgnoreCase("save")) {
                print(config.save(name) ? "saved \u00a7f" + name : "\u00a7ccould not save " + name);
            } else if (args[0].equalsIgnoreCase("load")) {
                print(config.load(name) ? "loaded \u00a7f" + name : "\u00a7ccould not load " + name);
            } else if (args[0].equalsIgnoreCase("new")) {
                print(config.create(name) ? "created \u00a7f" + name : "\u00a7c" + name + " already exists");
            } else if (args[0].equalsIgnoreCase("delete")) {
                print(config.delete(name) ? "deleted \u00a7f" + name : "\u00a7ccould not delete " + name);
            } else if (args[0].equalsIgnoreCase("list")) {
                print("configs \u00a78(" + config.currentName() + " active)");
                for (String profile : config.profiles()) {
                    Bridge.mc().chat().printRaw("  \u00a7b" + profile);
                }
            } else {
                printUsage();
            }
        }
    }

    // ------------------------------------------------------------------
    public static final class Friend extends Command {

        public Friend() { super("friend", "friend <add|remove|list|clear> [name] [alias]", "Manages your friends", "f"); }

        @Override public void execute(String[] args) {
            if (args.length == 0) { printUsage(); return; }
            xyz.stormclient.social.FriendManager friends = Storm.get().friends();

            if (args[0].equalsIgnoreCase("list")) {
                print("friends \u00a78(" + friends.size() + ")");
                for (String name : friends.names()) Bridge.mc().chat().printRaw("  \u00a7b" + name);
                return;
            }
            if (args[0].equalsIgnoreCase("clear")) {
                friends.clear();
                print("friend list cleared");
                return;
            }
            if (args.length < 2) { printUsage(); return; }

            if (args[0].equalsIgnoreCase("add")) {
                friends.add(args[1], args.length > 2 ? args[2] : "");
                print("\u00a7f" + args[1] + " \u00a77is now your friend");
            } else if (args[0].equalsIgnoreCase("remove")) {
                print(friends.remove(args[1]) ? "removed \u00a7f" + args[1] : "\u00a7c" + args[1] + " was not a friend");
            } else {
                printUsage();
            }
        }
    }

    // ------------------------------------------------------------------
    public static final class Hud extends Command {

        public Hud() { super("hud", "hud [reset]", "Opens the HUD editor", "hudeditor"); }

        @Override public void execute(String[] args) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
                Storm.get().hud().resetPositions();
                print("HUD positions reset");
                return;
            }
            Storm.get().hud().openEditor();
        }
    }

    // ------------------------------------------------------------------
    public static final class ThemeCommand extends Command {

        public ThemeCommand() { super("theme", "theme <preset|accent> <value>", "Changes the client colours"); }

        @Override public void execute(String[] args) {
            Theme theme = Storm.get().theme();
            if (args.length < 2) {
                print("current theme \u00a7f" + theme.preset().name().toLowerCase());
                printUsage();
                return;
            }
            if (args[0].equalsIgnoreCase("preset")) {
                try {
                    theme.setPreset(Theme.Preset.valueOf(args[1].toUpperCase()));
                    print("theme set to \u00a7f" + args[1].toLowerCase());
                } catch (IllegalArgumentException e) {
                    error("unknown preset, try storm / midnight / ember / mint / light");
                }
            } else if (args[0].equalsIgnoreCase("accent")) {
                theme.setAccent(ColorUtil.parseHex(args[1]));
                print("accent set to \u00a7f" + args[1]);
            } else {
                printUsage();
            }
        }
    }

    // ------------------------------------------------------------------
    public static final class Panic extends Command {

        public Panic() { super("panic", "panic", "Turns every module off at once", "off"); }

        @Override public void execute(String[] args) {
            int count = 0;
            for (Module module : Storm.get().modules().enabled()) {
                if (module.category() == xyz.stormclient.module.Category.HUD) continue;
                module.setEnabled(false);
                count++;
            }
            print("disabled \u00a7f" + count + " \u00a77modules");
        }
    }

    // ------------------------------------------------------------------
    public static final class Prefix extends Command {

        public Prefix() { super("prefix", "prefix <character>", "Changes the command prefix"); }

        @Override public void execute(String[] args) {
            if (args.length == 0) { printUsage(); return; }
            Storm.get().commands().setPrefix(args[0]);
            print("prefix is now \u00a7f" + args[0]);
        }
    }

    // ------------------------------------------------------------------
    public static final class Say extends Command {

        public Say() { super("say", "say <message>", "Sends a chat message, prefix and all"); }

        @Override public void execute(String[] args) {
            if (args.length == 0) { printUsage(); return; }
            Bridge.mc().network().sendChat(join(args, 0));
        }
    }

    // ------------------------------------------------------------------
    public static final class Reset extends Command {

        public Reset() { super("reset", "reset <module|all>", "Resets settings to their defaults"); }

        @Override public void execute(String[] args) {
            if (args.length == 0) { printUsage(); return; }

            if (args[0].equalsIgnoreCase("all")) {
                for (Module module : Storm.get().modules().all()) {
                    for (Setting<?> setting : module.settings()) setting.reset();
                }
                print("every setting reset");
                return;
            }
            Module module = Storm.get().modules().byName(args[0]);
            if (module == null) { error("no module called " + args[0]); return; }
            for (Setting<?> setting : module.settings()) setting.reset();
            print(module.name() + " \u00a77reset");
        }
    }

    // ------------------------------------------------------------------
    public static final class Info extends Command {

        public Info() { super("info", "info", "Shows build information", "version", "about"); }

        @Override public void execute(String[] args) {
            print("\u00a7f" + StormInfo.FULL_NAME + " \u00a7b" + StormInfo.VERSION);
            Bridge.mc().chat().printRaw("  \u00a77build   \u00a7f" + StormInfo.BUILD);
            Bridge.mc().chat().printRaw("  \u00a77game    \u00a7f" + Bridge.version().id());
            Bridge.mc().chat().printRaw("  \u00a77modules \u00a7f" + Storm.get().modules().all().size()
                    + " \u00a78(" + Storm.get().modules().enabled().size() + " on)");
            Bridge.mc().chat().printRaw("  \u00a77config  \u00a7f" + Storm.get().config().currentName());
        }
    }
}
