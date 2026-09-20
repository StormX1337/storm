package xyz.stormclient.bridge.mc189.mixin;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import xyz.stormclient.event.events.ViewEvent;
import xyz.stormclient.bridge.mc189.StormHooks;

/** Camera hooks: field of view, the hurt tilt and the light level. */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Inject(method = "getFOVModifier", at = @At("RETURN"), cancellable = true)
    private void storm$onFov(float partialTicks, boolean useFov, CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue(StormHooks.onView(ViewEvent.Kind.FOV, callback.getReturnValue()));
    }

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void storm$onHurtCam(float partialTicks, CallbackInfo callback) {
        if (StormHooks.onView(ViewEvent.Kind.HURT_CAM, 1.0F) <= 0.001F) callback.cancel();
    }

    @Inject(method = "setupFog", at = @At("HEAD"))
    private void storm$onLight(int mode, float partialTicks, CallbackInfo callback) {
        StormHooks.onView(ViewEvent.Kind.LIGHT, 1.0F);
    }
}
