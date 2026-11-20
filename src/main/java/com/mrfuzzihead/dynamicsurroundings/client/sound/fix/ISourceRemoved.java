package com.mrfuzzihead.dynamicsurroundings.client.sound.fix;

/**
 * Duck interface implemented by the {@code paulscode.sound.Source} mixin. The old coremod added a {@code removed}
 * field to {@code Source} with ASM and then reached it through {@code ReflectionHelper}; the mixin adds the field
 * and exposes it through this interface instead.
 */
public interface ISourceRemoved {

    boolean dynamicsurroundings$isRemoved();

    void dynamicsurroundings$setRemoved(boolean removed);
}
