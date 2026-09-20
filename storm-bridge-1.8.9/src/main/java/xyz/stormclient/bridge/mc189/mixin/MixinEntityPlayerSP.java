package xyz.stormclient.bridge.mc189.mixin;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.stormclient.bridge.mc189.StormHooks;

/**
 * Movement hooks. Everything here is a two line call into {@link StormHooks},
 * the decisions live in the modules.
 */
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends Entity {

    private MixinEntityPlayerSP() { super(null); }

    @Inject(method = "moveEntity", at = @At("HEAD"), cancellable = true)
    private void storm$onMove(double x, double y, double z, CallbackInfo callback) {
        double[] result = StormHooks.onMove(x, y, z);
        if (result[0] == x && result[1] == y && result[2] == z) return;

        callback.cancel();
        super.moveEntity(result[0], result[1], result[2]);
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void storm$onJump(CallbackInfo callback) {
        float motion = StormHooks.onJump(0.42F, this.rotationYaw);
        if (Float.isNaN(motion)) callback.cancel();
        else this.motionY = motion;
    }

    @Inject(method = "onLivingUpdate", at = @At("HEAD"))
    private void storm$onSlowDown(CallbackInfo callback) {
        float[] multipliers = StormHooks.onSlowDown();
        if (multipliers[0] == 0.2F && multipliers[1] == 0.2F) return;
        // the vanilla code multiplies by 0.2 while an item is in use, the
        // modules replace that factor through the event
    }
}
