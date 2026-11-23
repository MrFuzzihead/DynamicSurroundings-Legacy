package com.mrfuzzihead.dynamicsurroundings.mixins.early;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrfuzzihead.dynamicsurroundings.client.RenderWeather;
import com.mrfuzzihead.dynamicsurroundings.client.weather.WeatherBackend;

/**
 * Port of the {@code PatchEntityRenderer} coremod patch. Rain splashes and the rain/snow overlay are delegated to
 * the mod's own renderers, which know about dimensions and biomes vanilla does not model.
 *
 * <p>
 * The injectors are cancellable at {@code HEAD} and only cancel when {@link WeatherBackend} says DS owns the frame.
 * That is what fixes the old double render: the transformer inserted a {@code RETURN}, the mixin conversion dropped
 * it, and every non-DS owner (vanilla, an {@code IRenderHandler}, or Angelica's instanced weather) was left running
 * on top of DS. The explicit {@code order} makes DS decide before Angelica's default-priority injector; if DS
 * declines, Angelica's injector still runs normally.
 * </p>
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Unique
    private static final int DS_WEATHER_INJECTOR_ORDER = 900;

    @Inject(
        method = "addRainParticles",
        at = @At("HEAD"),
        cancellable = true,
        order = DS_WEATHER_INJECTOR_ORDER,
        require = 1)
    private void dynamicsurroundings$addRainParticles(CallbackInfo ci) {
        if (!WeatherBackend.isDsOwner(Minecraft.getMinecraft().theWorld)) {
            return;
        }

        RenderWeather.addRainParticles((EntityRenderer) (Object) this);
        ci.cancel();
    }

    @Inject(
        method = "renderRainSnow",
        at = @At("HEAD"),
        cancellable = true,
        order = DS_WEATHER_INJECTOR_ORDER,
        require = 1)
    private void dynamicsurroundings$renderRainSnow(float partialTicks, CallbackInfo ci) {
        if (!WeatherBackend.isDsOwner(Minecraft.getMinecraft().theWorld)) {
            return;
        }

        RenderWeather.renderRainSnow((EntityRenderer) (Object) this, partialTicks);
        ci.cancel();
    }
}
