package com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors;

import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Replaces the {@code EntityLivingBase.field_70703_bu} (isJumping) entry from the old access transformer. The
 * footstep engine reads it to detect jumps.
 */
@Mixin(EntityLivingBase.class)
public interface EntityLivingBaseAccessor {

    @Accessor("isJumping")
    boolean dynamicsurroundings$isJumping();
}
