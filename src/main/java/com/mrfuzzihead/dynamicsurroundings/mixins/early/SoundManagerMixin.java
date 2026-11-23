package com.mrfuzzihead.dynamicsurroundings.mixins.early;

import java.net.URL;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundPoolEntry;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrfuzzihead.dynamicsurroundings.client.sound.cache.SoundCache;

import paulscode.sound.SoundSystem;

/**
 * Port of the four {@code SoundManager} coremod patches (plus the play-time constant tweak):
 *
 * <ul>
 * <li>{@code PatchSoundManager} - serve decoded sounds from {@link SoundCache} instead of opening a stream URL for
 * every play.</li>
 * <li>{@code PatchSoundManagerClampPitch}/{@code PatchSoundManagerClampVolume} - clamp pitch and volume with the
 * mod's configurable registry.</li>
 * <li>{@code SoundPlayFlush} - flush the buffered spot sounds right after vanilla queues a sound, because the OpenAL
 * worker thread must not race the mod's own queue.</li>
 * <li>{@code PatchSoundManagerPlayTime} - vanilla keeps a played sound reserved for 20 ticks; the mod uses 0 so a
 * finished sound is released immediately. The old transformer replaced the {@code BIPUSH 20} in {@code playSound};
 * the {@code @ModifyConstant} below changes the same constant.</li>
 * </ul>
 *
 * <p>
 * The four methods the old transformer replaced have their bodies discarded in favour of the mod's implementation,
 * which is exactly what {@code @Inject(HEAD, cancellable)} + {@code setReturnValue} does. The other sound manager
 * patch (forcing every public method to be {@code synchronized}) is not expressible as a mixin and is kept as a
 * small transformer, see {@code SoundManagerSyncTransformer}.
 * </p>
 */
@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {

    @Inject(method = "getURLForSoundResource", at = @At("HEAD"), cancellable = true, require = 1)
    private static void dynamicsurroundings$getURLForSoundResource(ResourceLocation resource,
        CallbackInfoReturnable<URL> cir) {
        cir.setReturnValue(SoundCache.getURLForSoundResource(resource));
    }

    @Inject(method = "getNormalizedPitch", at = @At("HEAD"), cancellable = true, require = 1)
    private void dynamicsurroundings$getNormalizedPitch(ISound sound, SoundPoolEntry poolEntry,
        CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(
            com.mrfuzzihead.dynamicsurroundings.client.sound.DynamicSoundManager.getNormalizedPitch(sound, poolEntry));
    }

    @Inject(method = "getNormalizedVolume", at = @At("HEAD"), cancellable = true, require = 1)
    private void dynamicsurroundings$getNormalizedVolume(ISound sound, SoundPoolEntry poolEntry, SoundCategory category,
        CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(
            com.mrfuzzihead.dynamicsurroundings.client.sound.DynamicSoundManager
                .getNormalizedVolume(sound, poolEntry, category));
    }

    @ModifyConstant(method = "playSound", constant = @Constant(intValue = 20), require = 1)
    private int dynamicsurroundings$zeroPlayTime(int original) {
        return 0;
    }

    @Redirect(
        method = "playSound",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/audio/SoundManager$SoundSystemStarterThread;play(Ljava/lang/String;)V",
            remap = false),
        require = 1)
    private void dynamicsurroundings$playAndFlush(@Coerce SoundSystem soundSystem, String id) {
        soundSystem.play(id);
        soundSystem.CommandQueue(null);
    }
}
