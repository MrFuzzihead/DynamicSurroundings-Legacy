package com.mrfuzzihead.dynamicsurroundings.client.weather;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.testutil.WorldTestSupport;

public class WeatherColumnCacheTest {

    private WorldClient world;
    private WorldProvider provider;

    @Before
    public void setUp() {
        WeatherColumnCache.INSTANCE.clear();
        world = mock(WorldClient.class);
        provider = mock(WorldProvider.class);
        provider.dimensionId = 0;
        WorldTestSupport.setProvider(world, provider);
        WorldChunkManager chunkManager = mock(WorldChunkManager.class);
        when(world.getWorldChunkManager()).thenReturn(chunkManager);
        when(world.getBiomeGenForCoords(anyInt(), anyInt())).thenReturn(BiomeGenBase.plains);
        when(world.getPrecipitationHeight(anyInt(), anyInt())).thenReturn(64);
        when(world.getTotalWorldTime()).thenReturn(0L);
        when(world.getLightBrightnessForSkyBlocks(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(0xABCDEF);
        when(chunkManager.getTemperatureAtHeight(anyFloat(), anyInt())).thenReturn(0.2F);
    }

    @After
    public void tearDown() {
        WeatherColumnCache.INSTANCE.clear();
    }

    @Test
    public void reusesAColumnWithinTheTickBucket() {
        WeatherColumnCache.Column first = WeatherColumnCache.INSTANCE.get(world, 10, 20);
        WeatherColumnCache.Column second = WeatherColumnCache.INSTANCE.get(world, 10, 20);

        assertEquals(first.biome, second.biome);
        assertEquals(64, first.precipitationHeight);
        assertEquals(0.2F, first.temperature, 0.0F);
        assertEquals(1, WeatherColumnCache.INSTANCE.size());
        verify(world, times(1)).getBiomeGenForCoords(10, 20);
    }

    @Test
    public void cachesLightAtTheRequestedHeight() {
        WeatherColumnCache.Column column = WeatherColumnCache.INSTANCE.get(world, 10, 20);
        assertEquals(0xABCDEF, column.lightAt(world, 70));
        assertEquals(0xABCDEF, column.lightAt(world, 70));

        verify(world, times(1)).getLightBrightnessForSkyBlocks(10, 70, 20, 0);
    }

    @Test
    public void invalidatesOnTheNextBucket() {
        WeatherColumnCache.INSTANCE.get(world, 10, 20);
        when(world.getTotalWorldTime()).thenReturn(20L);
        WeatherColumnCache.INSTANCE.get(world, 10, 20);

        verify(world, times(2)).getBiomeGenForCoords(10, 20);
    }

    @Test
    public void invalidatesOnDimensionChange() {
        WeatherColumnCache.INSTANCE.get(world, 10, 20);
        provider.dimensionId = 1;
        WeatherColumnCache.INSTANCE.get(world, 10, 20);

        verify(world, times(2)).getBiomeGenForCoords(10, 20);
    }
}
