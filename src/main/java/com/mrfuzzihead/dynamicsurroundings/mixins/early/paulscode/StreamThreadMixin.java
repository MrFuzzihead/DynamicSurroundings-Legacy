package com.mrfuzzihead.dynamicsurroundings.mixins.early.paulscode;

import java.util.ListIterator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mrfuzzihead.dynamicsurroundings.client.sound.fix.SoundFixMethods;

import paulscode.sound.Source;
import paulscode.sound.StreamThread;

/**
 * Port of the {@code SoundCrashFixStreamThread} coremod patch. The streaming thread iterates its source list and
 * must skip sources that were removed while it was not looking; {@link SoundFixMethods#removeSource} returns
 * {@code null} for those (a {@code null} makes the loop drop the entry from the iterator).
 *
 * <p>
 * The old transformer located the {@code ListIterator.next()} call and inserted the filter around the local that
 * stores the result. Redirecting the call itself is the same operation without depending on local variable names.
 * </p>
 */
@Mixin(value = StreamThread.class, remap = false)
public abstract class StreamThreadMixin {

    @Redirect(
        method = "run",
        at = @At(value = "INVOKE", target = "Ljava/util/ListIterator;next()Ljava/lang/Object;"),
        require = 1)
    private Object dynamicsurroundings$removeSource(ListIterator<?> iterator) {
        return SoundFixMethods.removeSource((Source) iterator.next());
    }
}
