package com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors;

import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundRegistry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Replaces the {@code SoundHandler.field_147694_f} (sndManager) entry from the old access transformer. The mod reads
 * the live sound-manager state (playing sounds, delayed sounds, the OpenAL command queue).
 */
@Mixin(SoundHandler.class)
public interface SoundHandlerAccessor {

    @Accessor("sndManager")
    SoundManager dynamicsurroundings$getSndManager();

    @Accessor("sndRegistry")
    SoundRegistry dynamicsurroundings$getSndRegistry();
}
