package xyz.stormclient.module.impl.player;

import xyz.stormclient.event.Subscribe;
import xyz.stormclient.event.events.PacketEvent;
import xyz.stormclient.event.events.TickEvent;
import xyz.stormclient.module.Category;
import xyz.stormclient.module.Module;
import xyz.stormclient.setting.BooleanSetting;
import xyz.stormclient.setting.NumberSetting;
import xyz.stormclient.util.MovementUtil;

/** Detaches the camera from the body. The body stays where you left it. */
public class Freecam extends Module {

    private final NumberSetting  speed    = add(new NumberSetting("Speed", 1.0, 0.2, 5.0, 0.1));
    private final BooleanSetting noPackets= add(new BooleanSetting("Freeze position", true));
    private final BooleanSetting showBody = add(new BooleanSetting("Show body", true));

    private double originX, originY, originZ;
    private float originYaw, originPitch;

    public Freecam() {
        super("Freecam", "Fly the camera around without moving", Category.PLAYER);
    }

    @Override public void onEnable() {
        if (nullCheck()) return;
        originX = player().x();
        originY = player().y();
        originZ = player().z();
        originYaw = player().yaw();
        originPitch = player().pitch();
    }

    @Override public void onDisable() {
        if (nullCheck()) return;
        player().setPosition(originX, originY, originZ);
        player().setRotation(originYaw, originPitch);
        player().setMotion(0, 0, 0);
    }

    @Subscribe
    public void onPacket(PacketEvent.Send event) {
        if (!noPackets.get()) return;
        switch (event.type()) {
            case C_PLAYER:
            case C_PLAYER_POSITION:
            case C_PLAYER_LOOK:
            case C_PLAYER_POSITION_LOOK:
            case C_BLOCK_DIG:
            case C_USE_ENTITY:
                event.cancel();
                break;
            default:
        }
    }

    @Subscribe
    public void onTick(TickEvent.Pre event) {
        if (nullCheck()) return;
        player().setOnGround(false);
        player().setFallDistance(0);

        double y = 0;
        if (mc().input().jump())  y += speed.get() * 0.5;
        if (mc().input().sneak()) y -= speed.get() * 0.5;

        double x = 0, z = 0;
        if (MovementUtil.isMoving()) {
            double yaw = Math.toRadians(MovementUtil.movementYaw());
            x = -Math.sin(yaw) * speed.get() * 0.5;
            z = Math.cos(yaw) * speed.get() * 0.5;
        }
        player().setMotion(x, y, z);
    }

    public boolean showBody() { return showBody.get(); }
    public double[] origin()  { return new double[] { originX, originY, originZ }; }
}
