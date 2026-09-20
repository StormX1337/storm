package xyz.stormclient.module.impl.misc;

import xyz.stormclient.Storm;
import xyz.stormclient.bridge.PacketType;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.ChatEvent;
import xyz.stormclient.event.events.PacketEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.ui.notify.Notification;
import xyz.stormclient.util.EntityUtil;

/** Turns useful game events into Storm notifications. */
public class Notifier extends Module {

    private final BooleanSetting kills   = add(new BooleanSetting("Kills", true));
    private final BooleanSetting lowHealth = add(new BooleanSetting("Low health", true));
    private final BooleanSetting whispers = add(new BooleanSetting("Whispers", true));

    private boolean warned;

    public Notifier() {
        super("Notifier", "Pops up notifications for important events", Category.MISC);
    }

    @Subscribe
    public void onPacket(PacketEvent.Receive event) {
        if (nullCheck()) return;

        if (lowHealth.get() && event.type() == PacketType.S_UPDATE_HEALTH) {
            float health = event.packet().getFloat("health");
            if (health <= 6F && health > 0F && !warned) {
                warned = true;
                Storm.get().notifications().push(new Notification("Low health",
                        "You are at " + (int) health + " hearts", Notification.Type.WARNING, 3000));
            } else if (health > 8F) {
                warned = false;
            }
        }
    }

    @Subscribe
    public void onChat(ChatEvent.Receive event) {
        if (!whispers.get()) return;
        String plain = EntityUtil.stripColor(event.message());
        if (plain.contains("whispers to you") || plain.startsWith("From ")) {
            Storm.get().notifications().push(new Notification("Whisper", plain, Notification.Type.INFO, 4000));
        }
    }
}
