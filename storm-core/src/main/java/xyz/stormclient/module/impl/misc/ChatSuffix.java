package xyz.stormclient.module.impl.misc;

import xyz.stormclient.StormInfo;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.ChatEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.StringSetting;

public class ChatSuffix extends Module {

    private final StringSetting  suffix   = add(new StringSetting("Suffix", " \u00bb " + StormInfo.NAME));
    private final BooleanSetting commands = add(new BooleanSetting("On commands", false));

    public ChatSuffix() {
        super("ChatSuffix", "Appends a tag to your chat messages", Category.MISC);
    }

    @Subscribe
    public void onChat(ChatEvent.Send event) {
        String message = event.message();
        if (message.startsWith("/") && !commands.get()) return;
        if (message.length() + suffix.get().length() > 100) return;
        event.setMessage(message + suffix.get());
    }
}
