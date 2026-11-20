package com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors;

import java.util.Map;

import net.minecraft.client.audio.SoundManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Replaces the {@code SoundManager} map entries from the old access transformer. Only the state the mod actually
 * inspects is exposed; the remaining fields the transformer opened (sndSystem, invPlayingSounds, categorySounds,
 * tickableSounds, playingSoundPoolEntries, playingSoundsStopTime, loaded, listeners, UNABLE_TO_PLAY) had no callers
 * or, for {@code sndSystem}, are reached through the sound-manager mixin instead.
 */
@Mixin(SoundManager.class)
public interface SoundManagerAccessor {

    @Accessor("playingSounds")
    Map<?, ?> dynamicsurroundings$getPlayingSounds();

    @Accessor("delayedSounds")
    Map<?, ?> dynamicsurroundings$getDelayedSounds();
}
