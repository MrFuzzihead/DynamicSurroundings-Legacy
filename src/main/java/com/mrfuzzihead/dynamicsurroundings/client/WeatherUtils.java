package com.mrfuzzihead.dynamicsurroundings.client;

import net.minecraft.world.biome.BiomeGenBase;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.weather.Weather;
import com.mrfuzzihead.dynamicsurroundings.data.BiomeRegistry;

public class WeatherUtils {

    public static boolean biomeHasDust(BiomeGenBase biome) {
        return ModOptions.allowDesertFog && BiomeRegistry.hasDust(biome) && !Weather.doVanilla();
    }
}
