package xyz.stormclient.module.impl.combat;

import xyz.stormclient.bridge.IPlayer;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.ModeSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.util.RandomUtil;
import xyz.stormclient.util.TimerUtil;

/** Paces left clicks while the attack key is held, with a human looking jitter. */
public class AutoClicker extends Module {

    private final NumberSetting minCps = add(new NumberSetting("Min CPS", 10, 1, 25, 1));
    private final NumberSetting maxCps = add(new NumberSetting("Max CPS", 14, 1, 25, 1));
    private final NumberSetting jitter = add(new NumberSetting("Jitter", 12, 0, 60, 1).suffix("ms"));
    private final ModeSetting   pattern = add(new ModeSetting("Pattern", "Normal", "Normal", "Butterfly", "Drag"));
    private final BooleanSetting blocksToo = add(new BooleanSetting("Break blocks", false));
    private final BooleanSetting onlyWeapon = add(new BooleanSetting("Weapon only", false));

    private final TimerUtil timer = new TimerUtil();
    private long delay;
    private int burst;

    public AutoClicker() {
        super("AutoClicker", "Clicks for you while the attack button is held", Category.COMBAT);
    }

    @Override public String tag() { return minCps.getInt() + "-" + maxCps.getInt(); }

    @Subscribe
    public void onTick(TickEvent.Post event) {
        if (nullCheck()) return;
        IPlayer self = player();
        if (!mc().input().attack()) { burst = 0; return; }
        if (!blocksToo.get() && lookingAtBlock()) return;
        if (onlyWeapon.get() && !self.inventory().slot(self.inventory().heldSlot()).isWeapon()) return;

        if (timer.elapsed() < delay) return;

        self.swingArm();
        mc().input().setKeyState("attack", true);
        timer.reset();
        delay = nextDelay();
    }

    private boolean lookingAtBlock() {
        int[] hit = world().raytraceBlock(5.0);
        return hit != null;
    }

    private long nextDelay() {
        double min = Math.min(minCps.get(), maxCps.get());
        double max = Math.max(minCps.get(), maxCps.get());
        double cps = RandomUtil.range(min, max);

        if (pattern.is("Butterfly")) {
            // two fast clicks, then a longer pause, the way a real butterfly click looks
            burst = (burst + 1) % 3;
            cps = burst == 0 ? cps * 0.6 : cps * 1.6;
        } else if (pattern.is("Drag")) {
            cps *= RandomUtil.range(0.85, 1.35);
        }

        long base = (long) (1000.0 / Math.max(1.0, cps));
        return Math.max(20, base + (long) RandomUtil.range(-jitter.get(), jitter.get()));
    }
}
