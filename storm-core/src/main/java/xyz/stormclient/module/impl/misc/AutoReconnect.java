package xyz.stormclient.module.impl.misc;

import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.NumberSetting;

/** Reconnects to the last server after a disconnect. The bridge drives the screen. */
public class AutoReconnect extends Module {

    private final NumberSetting delay   = add(new NumberSetting("Delay", 3, 1, 60, 1).suffix("s"));
    private final NumberSetting retries = add(new NumberSetting("Max retries", 10, 1, 100, 1));

    public AutoReconnect() {
        super("AutoReconnect", "Rejoins after a disconnect", Category.MISC);
    }

    public int delaySeconds() { return delay.getInt(); }
    public int maxRetries()   { return retries.getInt(); }
}
