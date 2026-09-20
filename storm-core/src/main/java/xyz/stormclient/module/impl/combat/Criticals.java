package xyz.stormclient.module.impl.combat;

import xyz.stormclient.bridge.IPlayer;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.AttackEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.setting.NumberSetting;

/** Makes attacks count as critical hits. */
public class Criticals extends Module {

    private final ModeSetting mode = add(new ModeSetting("Mode", "Packet", "Packet", "Jump", "Mini jump"));
    private final NumberSetting chance = add(new NumberSetting("Chance", 100, 1, 100, 1).suffix("%"));

    public Criticals() {
        super("Criticals", "Turns your hits into critical hits", Category.COMBAT);
    }

    @Override public String tag() { return mode.get(); }

    @Subscribe
    public void onAttack(AttackEvent event) {
        if (nullCheck()) return;
        IPlayer self = player();
        if (!self.isOnGround() || self.isInWater() || self.isInLava() || self.isOnLadder()) return;
        if (Math.random() * 100 > chance.get()) return;

        if (mode.is("Packet")) {
            double x = self.x(), y = self.y(), z = self.z();
            mc().network().sendSilent(mc().network().createPositionPacket(x, y + 0.0625, z, false));
            mc().network().sendSilent(mc().network().createPositionPacket(x, y, z, false));
            mc().network().sendSilent(mc().network().createPositionPacket(x, y + 1.1E-5, z, false));
            mc().network().sendSilent(mc().network().createPositionPacket(x, y, z, false));
        } else if (mode.is("Jump")) {
            self.jump();
        } else {
            self.setMotionY(0.1);
        }
    }
}
