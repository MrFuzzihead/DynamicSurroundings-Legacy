package com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors;

import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Replaces the {@code Entity.field_70150_b} (nextStepDistance) entry from the old access transformer. The footsteps
 * module parks the vanilla step distance while its own footstep engine runs.
 */
@Mixin(Entity.class)
public interface EntityAccessor {

    @Accessor("nextStepDistance")
    int dynamicsurroundings$getNextStepDistance();

    @Accessor("nextStepDistance")
    void dynamicsurroundings$setNextStepDistance(int nextStepDistance);
}
