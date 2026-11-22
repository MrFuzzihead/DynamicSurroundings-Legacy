package com.mrfuzzihead.dynamicsurroundings.client.weather;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;

import com.mrfuzzihead.dynamicsurroundings.client.WeatherUtils;
import com.mrfuzzihead.dynamicsurroundings.data.BiomeRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gnu.trove.map.hash.TIntFloatHashMap;

/**
 * Shared per-tick cache for the biome/precipitation/height data used by both the storm quads and splash particles.
 *
 * <p>
 * The cache is keyed by absolute column and invalidated when the dimension or the 20-tick bucket changes. That keeps
 * a single frame's 441-column storm scan cheap without making weather react to stale biome data for more than one
 * second.
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class WeatherColumnCache {

    public static final int BUCKET_TICKS = 20;
    public static final WeatherColumnCache INSTANCE = new WeatherColumnCache();

    private final Map<Long, Column> columns = new HashMap<>();
    private int dimensionId = Integer.MIN_VALUE;
    private long bucket = Long.MIN_VALUE;

    private WeatherColumnCache() {}

    public static final class Column {

        public final BiomeGenBase biome;
        public final int x;
        public final int z;
        public final int precipitationHeight;
        public final float temperature;
        public final boolean hasDust;
        public final boolean hasPrecipitation;
        private final TIntFloatHashMap temperatures = new TIntFloatHashMap();

        private Column(BiomeGenBase biome, int x, int z, int precipitationHeight, float temperature, boolean hasDust,
            boolean hasPrecipitation) {
            this.biome = biome;
            this.x = x;
            this.z = z;
            this.precipitationHeight = precipitationHeight;
            this.temperature = temperature;
            this.hasDust = hasDust;
            this.hasPrecipitation = hasPrecipitation;
            this.temperatures.put(precipitationHeight, temperature);
        }

        public float temperatureAt(World world, int y) {
            float cached = this.temperatures.get(y);
            if (this.temperatures.containsKey(y)) {
                return cached;
            }

            WorldChunkManager chunkManager = world.getWorldChunkManager();
            float value = chunkManager
                .getTemperatureAtHeight(this.biome.getFloatTemperature(this.x, y, this.z), this.precipitationHeight);
            this.temperatures.put(y, value);
            return value;
        }
    }

    public Column get(World world, int x, int z) {
        invalidateIfNeeded(world);
        long key = key(x, z);
        Column column = columns.get(key);
        if (column != null) {
            return column;
        }

        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        int height = world.provider.dimensionId == -1 ? 0 : world.getPrecipitationHeight(x, z);
        WorldChunkManager chunkManager = world.getWorldChunkManager();
        float temperature = chunkManager.getTemperatureAtHeight(biome.getFloatTemperature(x, height, z), height);
        column = new Column(
            biome,
            x,
            z,
            height,
            temperature,
            WeatherUtils.biomeHasDust(biome),
            BiomeRegistry.hasPrecipitation(biome));
        columns.put(key, column);
        return column;
    }

    public void clear() {
        columns.clear();
        dimensionId = Integer.MIN_VALUE;
        bucket = Long.MIN_VALUE;
    }

    public int size() {
        return columns.size();
    }

    private void invalidateIfNeeded(World world) {
        int dimension = world.provider.dimensionId;
        long currentBucket = world.getTotalWorldTime() / BUCKET_TICKS;
        if (dimension != dimensionId || currentBucket != bucket) {
            columns.clear();
            dimensionId = dimension;
            bucket = currentBucket;
        }
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }
}
