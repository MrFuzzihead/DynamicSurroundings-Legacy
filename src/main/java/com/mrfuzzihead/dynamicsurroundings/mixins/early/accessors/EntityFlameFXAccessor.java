package com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors;

import net.minecraft.client.particle.EntityFlameFX;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Replaces the {@code EntityFlameFX.field_70562_a} (flameScale) entry from the old access transformer. The fire jet
 * particle scales the vanilla flame it spawns.
 */
@Mixin(EntityFlameFX.class)
public interface EntityFlameFXAccessor {

    @Accessor("flameScale")
    float dynamicsurroundings$getFlameScale();

    @Accessor("flameScale")
    void dynamicsurroundings$setFlameScale(float flameScale);
}
