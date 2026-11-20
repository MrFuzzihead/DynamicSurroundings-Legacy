package com.mrfuzzihead.dynamicsurroundings.data.compat;

import net.minecraft.world.biome.BiomeGenBase;

/**
 * Optional Biomes O' Plenty fog integration. BOP versions that expose fog through
 * {@code biomesoplenty.client.fog.IBiomeFog} are bridged by {@link BOPFogCompat}; when BOP is absent (or its API
 * does not match) {@link #NONE} keeps callers working without loading any BOP class.
 */
public interface IBOPFog {

    IBOPFog NONE = new IBOPFog() {

        @Override
        public boolean isFogBiome(BiomeGenBase biome) {
            return false;
        }

        @Override
        public int getFogColor(BiomeGenBase biome) {
            return 0;
        }

        @Override
        public float getFogDensity(BiomeGenBase biome) {
            return 0.0F;
        }
    };

    boolean isFogBiome(BiomeGenBase biome);

    int getFogColor(BiomeGenBase biome);

    float getFogDensity(BiomeGenBase biome);
}
