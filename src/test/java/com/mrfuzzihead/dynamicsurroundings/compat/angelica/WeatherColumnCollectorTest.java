package com.mrfuzzihead.dynamicsurroundings.compat.angelica;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;

import org.junit.Test;

import com.gtnewhorizons.angelica.api.weather.WeatherColumnSink;
import com.mrfuzzihead.dynamicsurroundings.client.weather.StormRenderer;
import com.mrfuzzihead.dynamicsurroundings.testutil.WorldTestSupport;

public class WeatherColumnCollectorTest {

    @Test
    public void emitsRainColumnsWithTheVanillaLayerPeriod() {
        WorldClient world = mock(WorldClient.class);
        WorldProvider provider = mock(WorldProvider.class);
        provider.dimensionId = 0;
        WorldTestSupport.setProvider(world, provider);
        WorldChunkManager chunkManager = mock(WorldChunkManager.class);
        when(world.getWorldChunkManager()).thenReturn(chunkManager);
        when(world.getBiomeGenForCoords(anyInt(), anyInt())).thenReturn(BiomeGenBase.plains);
        when(world.getPrecipitationHeight(anyInt(), anyInt())).thenReturn(64);
        when(world.getLightBrightnessForSkyBlocks(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(0xFFFFFF);
        when(chunkManager.getTemperatureAtHeight(anyFloat(), anyInt())).thenReturn(0.2F);

        EntityLivingBase view = mock(EntityLivingBase.class);
        view.posX = 0.0;
        view.posY = 64.0;
        view.posZ = 0.0;

        RecordingSink sink = new RecordingSink();
        StormRenderer.locationRainPng = new ResourceLocation("test:rain");
        WeatherColumnCollector.collect(world, view, 1, 1.0F, 1.0F, sink);

        assertEquals(1, sink.layers.size());
        assertEquals(32, sink.layers.get(0).period);
        assertEquals(9, sink.columns.size());
        assertTrue("the column should span the precipitation height", sink.columns.get(0).bottom == 64.0F);
        assertEquals(1.0F, sink.columns.get(0).alpha, 0.0F);
    }

    @Test
    public void emitsSnowColumnsWithTheSnowLayerPeriodBelowFreezing() {
        WorldClient world = mock(WorldClient.class);
        WorldProvider provider = mock(WorldProvider.class);
        provider.dimensionId = 0;
        WorldTestSupport.setProvider(world, provider);
        WorldChunkManager chunkManager = mock(WorldChunkManager.class);
        when(world.getWorldChunkManager()).thenReturn(chunkManager);
        when(world.getBiomeGenForCoords(anyInt(), anyInt())).thenReturn(BiomeGenBase.plains);
        when(world.getPrecipitationHeight(anyInt(), anyInt())).thenReturn(64);
        when(world.getLightBrightnessForSkyBlocks(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(0xFFFFFF);
        when(chunkManager.getTemperatureAtHeight(anyFloat(), anyInt())).thenReturn(0.0F);

        EntityLivingBase view = mock(EntityLivingBase.class);
        view.posX = 0.0;
        view.posY = 64.0;
        view.posZ = 0.0;

        RecordingSink sink = new RecordingSink();
        StormRenderer.locationSnowPng = new ResourceLocation("test:snow");
        WeatherColumnCollector.collect(world, view, 1, 1.0F, 1.0F, sink);

        assertEquals(1, sink.layers.size());
        assertEquals(512, sink.layers.get(0).period);
        assertEquals(9, sink.columns.size());
    }

    @Test
    public void emitsNothingWithoutRainOrRadius() {
        RecordingSink sink = new RecordingSink();
        WeatherColumnCollector.collect(null, null, 1, 1.0F, 1.0F, sink);
        WeatherColumnCollector.collect(null, null, 0, 1.0F, 1.0F, sink);
        assertEquals(0, sink.layers.size());
        assertEquals(0, sink.columns.size());
    }

    private static final class Layer {

        private final int period;

        private Layer(int period) {
            this.period = period;
        }
    }

    private static final class Column {

        private final float bottom;
        private final float alpha;

        private Column(float bottom, float alpha) {
            this.bottom = bottom;
            this.alpha = alpha;
        }
    }

    private static final class RecordingSink implements WeatherColumnSink {

        private final List<Layer> layers = new ArrayList<>();
        private final List<Column> columns = new ArrayList<>();

        @Override
        public void beginLayer(ResourceLocation texture, int scrollPeriod, float uDriftScale, float vDriftScale,
            float fadeScale) {
            layers.add(new Layer(scrollPeriod));
        }

        @Override
        public void column(int x, int z, float bottom, float top, float uBase, float uDrift, float vBase, float vDrift,
            int brightness, int scrollOffset, float scrollScale, float r, float g, float b, float a) {
            columns.add(new Column(bottom, a));
        }
    }
}
