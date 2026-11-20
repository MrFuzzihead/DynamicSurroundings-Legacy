package com.mrfuzzihead.dynamicsurroundings.mixins.late.bop;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import com.mrfuzzihead.dynamicsurroundings.data.compat.IBOPBiome;

/**
 * Replaces the optional-mod reflection in {@code BiomeRegistry}: Biomes O' Plenty stores its fog colour and density
 * on the biome itself, in fields that are not part of the {@code BiomeGenBase} API.
 *
 * <p>
 * The mixin is applied late, only when Biomes O' Plenty is loaded. {@code @Pseudo} plus a string target means the mod
 * has no compile-time dependency on BOP. {@code remap = false} because BOP has no obfuscation mappings.
 * </p>
 */
@Pseudo
@Mixin(targets = "biomesoplenty.common.biome.BOPBiome", remap = false)
public abstract class BOPBiomeMixin implements IBOPBiome {

    @Shadow
    private int fogColor;

    @Shadow
    private float fogDensity;

    @Override
    public int dynamicsurroundings$getFogColor() {
        return this.fogColor;
    }

    @Override
    public float dynamicsurroundings$getFogDensity() {
        return this.fogDensity;
    }
}
