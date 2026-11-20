package com.mrfuzzihead.dynamicsurroundings.mixins.early;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrfuzzihead.dynamicsurroundings.client.RenderWeather;

/**
 * Port of the {@code PatchEntityRenderer} coremod patch. Rain splashes and the rain/snow overlay are delegated to
 * the mod's own renderers, which know about dimensions and biomes vanilla does not model.
 *
 * <p>
 * Vanilla's {@code addRainParticles} is fully replaced (the old transformer overwrote its instruction list) while
 * {@code renderRainSnow} is kept and the mod's renderers run first, matching the original insertion point.
 * </p>
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "addRainParticles", at = @At("HEAD"), cancellable = true, require = 1)
    private void dynamicsurroundings$addRainParticles(CallbackInfo ci) {
        RenderWeather.addRainParticles((EntityRenderer) (Object) this);
        ci.cancel();
    }

    @Inject(method = "renderRainSnow", at = @At("HEAD"), require = 1)
    private void dynamicsurroundings$renderRainSnow(float partialTicks, CallbackInfo ci) {
        RenderWeather.renderRainSnow((EntityRenderer) (Object) this, partialTicks);
    }
}
