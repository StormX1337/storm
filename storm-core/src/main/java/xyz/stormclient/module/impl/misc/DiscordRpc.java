package xyz.stormclient.module.impl.misc;

import xyz.stormclient.StormInfo;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.StringSetting;
import xyz.stormclient.util.StormLogger;

/**
 * Rich presence state. The bridge module owns the native IPC connection,
 * this module only decides what should be displayed.
 */
public class DiscordRpc extends Module {

    private final BooleanSetting showServer = add(new BooleanSetting("Show server", false));
    private final BooleanSetting showMode   = add(new BooleanSetting("Show game mode", true));
    private final StringSetting  custom     = add(new StringSetting("Custom line", ""));

    public DiscordRpc() {
        super("DiscordRPC", "Shows Storm in your Discord status", Category.MISC);
    }

    @Override public void onEnable()  { StormLogger.info("rich presence enabled"); }
    @Override public void onDisable() { StormLogger.info("rich presence disabled"); }

    public String details() {
        if (!custom.get().isEmpty()) return custom.get();
        if (showServer.get() && !nullCheck()) return "Playing on " + mc().serverAddress();
        return StormInfo.FULL_NAME + " " + StormInfo.VERSION;
    }

    public String state() {
        if (nullCheck()) return "In the menu";
        return showMode.get() ? "Minecraft " + mc().version().id() : "In game";
    }
}
