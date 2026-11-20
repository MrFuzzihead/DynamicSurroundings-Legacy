package com.mrfuzzihead.dynamicsurroundings.mixins.early.paulscode;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.mrfuzzihead.dynamicsurroundings.client.sound.fix.ISourceRemoved;

import paulscode.sound.Source;

/**
 * Port of the {@code SoundCrashFixSource} coremod patch. paulscode's {@code Source} has no way to remember that a
 * streaming source was asked to be removed, so the mod adds a flag and defers the actual {@code cleanup()} to the
 * streaming thread (see {@link StreamThreadMixin}).
 *
 * <p>
 * {@code remap = false} because paulscode is not a Minecraft library and has no obfuscation mappings.
 * </p>
 */
@Mixin(value = Source.class, remap = false)
public abstract class SourceMixin implements ISourceRemoved {

    @Unique
    private boolean dynamicsurroundings$removed;

    @Override
    public boolean dynamicsurroundings$isRemoved() {
        return this.dynamicsurroundings$removed;
    }

    @Override
    public void dynamicsurroundings$setRemoved(boolean removed) {
        this.dynamicsurroundings$removed = removed;
    }
}
