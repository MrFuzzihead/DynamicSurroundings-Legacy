package com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Replaces the {@code EntityRenderer *} entry from the old access transformer. Only the two fields the weather
 * renderers actually read are exposed; {@code mc} is public API ({@code Minecraft.getMinecraft()}) and the other
 * fields the wildcard exposed were unused.
 */
@Mixin(EntityRenderer.class)
public interface EntityRendererAccessor {

    @Accessor("rendererUpdateCount")
    int dynamicsurroundings$getRendererUpdateCount();

    @Accessor("rainSoundCounter")
    int dynamicsurroundings$getRainSoundCounter();

    @Accessor("rainSoundCounter")
    void dynamicsurroundings$setRainSoundCounter(int rainSoundCounter);
}
