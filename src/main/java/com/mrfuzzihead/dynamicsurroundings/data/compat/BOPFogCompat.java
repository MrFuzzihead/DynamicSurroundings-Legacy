package com.mrfuzzihead.dynamicsurroundings.data.compat;

import net.minecraft.world.biome.BiomeGenBase;

import biomesoplenty.client.fog.IBiomeFog;

/**
 * Bridges BOP's public {@link IBiomeFog} API. BOP biomes that support fog implement that interface directly; the
 * per-biome colour and density are returned for the biome, not read from fields. In this BOP version every
 * implementation returns constants and ignores the coordinates (the interface is coordinate-based for future
 * flexibility), so querying at the origin matches what BOP's own fog handler sees.
 *
 * <p>
 * This class references BOP types and must only be loaded when BOP is present; {@link BOPFogIntegration} is the
 * guarded entry point.
 * </p>
 */
public final class BOPFogCompat implements IBOPFog {

    @Override
    public boolean isFogBiome(BiomeGenBase biome) {
        return biome instanceof IBiomeFog;
    }

    @Override
    public int getFogColor(BiomeGenBase biome) {
        return ((IBiomeFog) biome).getFogColour(0, 0, 0);
    }

    @Override
    public float getFogDensity(BiomeGenBase biome) {
        return ((IBiomeFog) biome).getFogDensity(0, 0, 0);
    }
}
