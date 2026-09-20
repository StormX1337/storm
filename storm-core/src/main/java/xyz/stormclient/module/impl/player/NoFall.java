package xyz.stormclient.module.impl.player;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.setting.NumberSetting;

public class NoFall extends Module {

    private final ModeSetting   mode   = add(new ModeSetting("Mode", "Packet", "Packet", "Cancel", "Edit"));
    private final NumberSetting minFall = add(new NumberSetting("Min fall", 3, 1, 20, 1).suffix("m"));

    public NoFall() {
        super("NoFall", "Take no fall damage", Category.PLAYER);
    }

    @Override public String tag() { return mode.get(); }

    @Subscribe
    public void onTick(TickEvent.Pre event) {
        if (nullCheck()) return;
        if (player().fallDistance() <= minFall.getFloat()) return;

        if (mode.is("Packet")) {
            mc().network().sendSilent(mc().network().createOnGroundPacket(true));
            player().setFallDistance(0);
        } else if (mode.is("Cancel")) {
            player().setFallDistance(0);
        } else if (mode.is("Edit") && player().motionY() < -0.5) {
            player().setMotionY(-0.05);
        }
    }
}
