package com.mrfuzzihead.dynamicsurroundings.mixins.early.paulscode;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mrfuzzihead.dynamicsurroundings.client.sound.fix.SoundFixMethods;

import paulscode.sound.Library;
import paulscode.sound.Source;

/**
 * Port of the {@code SoundCrashFixLibrary} coremod patch. When a streaming source is removed, paulscode calls
 * {@code Source.cleanup()} immediately; cleaning up a streaming source while the streaming thread still holds it is
 * the crash this patch avoids. The call is redirected to {@link SoundFixMethods#cleanupSource} which defers
 * streaming sources instead of cleaning them up.
 */
@Mixin(value = Library.class, remap = false)
public abstract class LibraryMixin {

    @Redirect(
        method = "removeSource",
        at = @At(value = "INVOKE", target = "Lpaulscode/sound/Source;cleanup()V"),
        require = 1)
    private void dynamicsurroundings$cleanupSource(Source source) {
        SoundFixMethods.cleanupSource(source);
    }
}
