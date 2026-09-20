package xyz.stormclient.module.impl.misc;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.ChatEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.setting.StringSetting;
import xyz.stormclient.util.EntityUtil;
import xyz.stormclient.util.TimerUtil;

/** Says something after a game ends. */
public class AutoGg extends Module {

    private final StringSetting message = add(new StringSetting("Message", "gg"));
    private final StringSetting triggers = add(new StringSetting("Triggers", "1st Killer,Winner,has won,Victory!")
            .maxLength(256));
    private final NumberSetting delay   = add(new NumberSetting("Delay", 800, 0, 5000, 100).suffix("ms"));

    private final TimerUtil cooldown = new TimerUtil();

    public AutoGg() {
        super("AutoGG", "Sends a message when a game ends", Category.MISC);
    }

    @Subscribe
    public void onChat(ChatEvent.Receive event) {
        if (nullCheck() || !cooldown.passed(8000)) return;
        String plain = EntityUtil.stripColor(event.message());

        for (String trigger : triggers.get().split(",")) {
            String t = trigger.trim();
            if (t.isEmpty() || !plain.contains(t)) continue;

            cooldown.reset();
            final long wait = delay.getLong();
            new Thread(new Runnable() {
                public void run() {
                    try { Thread.sleep(wait); } catch (InterruptedException ignored) { }
                    if (!nullCheck()) mc().network().sendChat(message.get());
                }
            }, "Storm-AutoGG").start();
            return;
        }
    }
}
