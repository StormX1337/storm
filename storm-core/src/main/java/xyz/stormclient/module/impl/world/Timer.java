package xyz.stormclient.module.impl.world;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;

/** Changes how fast the client ticks. */
public class Timer extends Module {

    private final NumberSetting  speed   = add(new NumberSetting("Speed", 1.2, 0.1, 5.0, 0.05).suffix("x"));
    private final BooleanSetting onlyMoving = add(new BooleanSetting("Only while moving", false));
    private final NumberSetting  balance = add(new NumberSetting("Balance ticks", 0, 0, 200, 5)
            .describe("Slows down again after this many sped up ticks"));

    private int used;

    public Timer() {
        super("Timer", "Speeds up or slows down the game clock", Category.WORLD);
    }

    @Override public String tag() { return String.format("%.2fx", speed.get()); }

    @Override public void onDisable() {
        if (mc() != null) mc().setTimerSpeed(1.0F);
        used = 0;
    }

    @Subscribe
    public void onTick(TickEvent.Pre event) {
        if (nullCheck()) return;
        if (onlyMoving.get() && !mc().input().moving()) {
            mc().setTimerSpeed(1.0F);
            return;
        }
        if (balance.getInt() > 0) {
            if (used >= balance.getInt()) {
                mc().setTimerSpeed(0.6F);
                if (--used <= 0) used = 0;
                return;
            }
            used++;
        }
        mc().setTimerSpeed(speed.getFloat());
    }
}
