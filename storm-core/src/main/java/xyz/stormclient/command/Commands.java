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
                print("§f" + command.name() + " §7- " + command.description());
                printUsageOf(command);
                return;
            }
            print("§f" + StormInfo.FULL_NAME + " §7commands");
            for (Command command : Storm.get().commands().commands()) {
                Bridge.mc().chat().printRaw("  §b" + Storm.get().commands().prefix() + command.name()
                        + " §8- §7" + command.description());
            }
        }

        private void printUsageOf(Command command) {
            Bridge.mc().chat().printRaw("  §7" + Storm.get().commands().prefix() + command.usage());
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
                print(module.name() + " §7is now " + (module.isEnabled() ? "§aon" : "§coff"));
                return;
            }

            Setting<?> setting = module.setting(args[1]);
            if (setting == null) { error(module.name() + " has no setting " + args[1]); return; }
            if (args.length == 2) {
                print(module.name() + " §7" + setting.name() + " §f= " + setting.display());
                return;
            }
            setting.deserialize(join(args, 2));
            print(module.name() + " §7" + setting.name() + " §f-> " + setting.display());
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
                print(module.name() + " §7unbound");
                return;
            }
            int key = Keyboard.code(args[1]);
            if (key == Keyboard.KEY_NONE) { error("unknown key " + args[1]); return; }
            module.setKeybind(key);
            print(module.name() + " §7bound to §f" + Keyboard.name(key));
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
                print(config.save(name) ? "saved §f" + name : "§ccould not save " + name);
            } else if (args[0].equalsIgnoreCase("load")) {
                print(config.load(name) ? "loaded §f" + name : "§ccould not load " + name);
            } else if (args[0].equalsIgnoreCase("new")) {
                print(config.create(name) ? "created §f" + name : "§c" + name + " already exists");
            } else if (args[0].equalsIgnoreCase("delete")) {
                print(config.delete(name) ? "deleted §f" + name : "§ccould not delete " + name);
            } else if (args[0].equalsIgnoreCase("list")) {
                print("configs §8(" + config.currentName() + " active)");
                for (String profile : config.profiles()) {
                    Bridge.mc().chat().printRaw("  §b" + profile);
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
                print("friends §8(" + friends.size() + ")");
                for (String name : friends.names()) Bridge.mc().chat().printRaw("  §b" + name);
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
                print("§f" + args[1] + " §7is now your friend");
            } else if (args[0].equalsIgnoreCase("remove")) {
                print(friends.remove(args[1]) ? "removed §f" + args[1] : "§c" + args[1] + " was not a friend");
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
                print("current theme §f" + theme.preset().name().toLowerCase());
                printUsage();
                return;
            }
            if (args[0].equalsIgnoreCase("preset")) {
                try {
                    theme.setPreset(Theme.Preset.valueOf(args[1].toUpperCase()));
                    print("theme set to §f" + args[1].toLowerCase());
                } catch (IllegalArgumentException e) {
                    error("unknown preset, try storm / midnight / ember / mint / light");
                }
            } else if (args[0].equalsIgnoreCase("accent")) {
                theme.setAccent(ColorUtil.parseHex(args[1]));
                print("accent set to §f" + args[1]);
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
            print("disabled §f" + count + " §7modules");
        }
    }

    // ------------------------------------------------------------------
    public static final class Prefix extends Command {

        public Prefix() { super("prefix", "prefix <character>", "Changes the command prefix"); }

        @Override public void execute(String[] args) {
            if (args.length == 0) { printUsage(); return; }
            Storm.get().commands().setPrefix(args[0]);
            print("prefix is now §f" + args[0]);
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
            print(module.name() + " §7reset");
        }
    }

    // ------------------------------------------------------------------
    public static final class Info extends Command {

        public Info() { super("info", "info", "Shows build information", "version", "about"); }

        @Override public void execute(String[] args) {
            print("§f" + StormInfo.FULL_NAME + " §b" + StormInfo.VERSION);
            Bridge.mc().chat().printRaw("  §7build   §f" + StormInfo.BUILD);
            Bridge.mc().chat().printRaw("  §7game    §f" + Bridge.version().id());
            Bridge.mc().chat().printRaw("  §7modules §f" + Storm.get().modules().all().size()
                    + " §8(" + Storm.get().modules().enabled().size() + " on)");
            Bridge.mc().chat().printRaw("  §7config  §f" + Storm.get().config().currentName());
        }
    }
}
