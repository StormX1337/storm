package xyz.stormclient.module.impl.render;

import xyz.stormclient.bridge.IItemStack;
import xyz.stormclient.bridge.IRenderer;
import xyz.stormclient.bridge.ItemType;
import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.RenderEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.ColorSetting;
import xyz.stormclient.setting.NumberSetting;

/** Simulates where a thrown projectile will land and draws the path. */
public class Trajectories extends Module {

    private final ColorSetting  color = add(new ColorSetting("Color", 0xFF35C4FF));
    private final NumberSetting steps = add(new NumberSetting("Steps", 120, 20, 400, 10));

    public Trajectories() {
        super("Trajectories", "Shows where your projectiles will land", Category.RENDER);
    }

    @Subscribe
    public void onRender(RenderEvent.World event) {
        if (nullCheck()) return;
        IItemStack held = player().inventory().slot(player().inventory().heldSlot());
        ItemType type = held.type();
        if (type != ItemType.BOW && type != ItemType.PEARL && type != ItemType.SPLASH_POTION
                && type != ItemType.CROSSBOW && type != ItemType.ROD) return;

        float p = event.partialTicks();
        double x = player().renderX(p);
        double y = player().renderY(p) + player().eyeHeight() - 0.1;
        double z = player().renderZ(p);

        float yaw = (float) Math.toRadians(player().yaw());
        float pitch = (float) Math.toRadians(player().pitch());
        double power = type == ItemType.BOW ? 2.5 : 1.5;

        double mx = -Math.sin(yaw) * Math.cos(pitch) * power;
        double my = -Math.sin(pitch) * power;
        double mz = Math.cos(yaw) * Math.cos(pitch) * power;

        double gravity = type == ItemType.BOW ? 0.05 : 0.03;
        double drag = 0.99;

        IRenderer r = mc().renderer();
        r.lineWidth(1.5F);
        for (int i = 0; i < steps.getInt(); i++) {
            double nx = x + mx * 0.1;
            double ny = y + my * 0.1;
            double nz = z + mz * 0.1;

            r.line3D(x, y, z, nx, ny, nz, color.rgb(), 1.5F);

            if (world().isSolid((int) Math.floor(nx), (int) Math.floor(ny), (int) Math.floor(nz))) {
                r.box3D(Math.floor(nx), Math.floor(ny), Math.floor(nz),
                        Math.floor(nx) + 1, Math.floor(ny) + 1, Math.floor(nz) + 1, color.rgb(), false);
                break;
            }
            x = nx; y = ny; z = nz;
            mx *= drag; mz *= drag;
            my = my * drag - gravity;
        }
    }
}
