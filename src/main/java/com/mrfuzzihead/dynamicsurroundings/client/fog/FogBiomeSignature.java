package com.mrfuzzihead.dynamicsurroundings.client.fog;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

/** Cheap biome-mix signature: centre plus the four cardinal samples at the fog radius. */
final class FogBiomeSignature {

    private static final int RADIUS = 20;

    private FogBiomeSignature() {}

    static int of(World world, int x, int z) {
        int hash = 1;
        hash = 31 * hash + biomeId(world, x, z);
        hash = 31 * hash + biomeId(world, x - RADIUS, z);
        hash = 31 * hash + biomeId(world, x + RADIUS, z);
        hash = 31 * hash + biomeId(world, x, z - RADIUS);
        hash = 31 * hash + biomeId(world, x, z + RADIUS);
        return hash;
    }

    private static int biomeId(World world, int x, int z) {
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        return biome == null ? Integer.MIN_VALUE : biome.biomeID;
    }
}
