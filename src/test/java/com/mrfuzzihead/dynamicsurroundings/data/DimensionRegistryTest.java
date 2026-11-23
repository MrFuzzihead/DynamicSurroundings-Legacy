package com.mrfuzzihead.dynamicsurroundings.data;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.testutil.WorldTestSupport;

public class DimensionRegistryTest {

    @Test
    public void flatOverworldUsesSeaLevelZeroWithoutClientState() {
        World world = mock(World.class);
        WorldProvider provider = mock(WorldProvider.class);
        provider.dimensionId = 0;
        WorldTestSupport.setProvider(world, provider);
        WorldInfo info = mock(WorldInfo.class);
        when(world.getWorldInfo()).thenReturn(info);
        when(info.getTerrainType()).thenReturn(WorldType.FLAT);

        assertEquals(0, DimensionRegistry.getSeaLevel(world));
    }

    @Test
    public void conditionsAreCachedUntilTheContextChanges() {
        World world = mock(World.class);
        WorldProvider provider = mock(WorldProvider.class);
        provider.dimensionId = 7;
        WorldTestSupport.setProvider(world, provider);
        when(world.getWorldInfo()).thenReturn(mock(WorldInfo.class));
        when(provider.getDimensionName()).thenReturn("Test");
        when(provider.getSunBrightnessFactor(1.0F)).thenReturn(0.9F);
        when(world.getRainStrength(1.0F)).thenReturn(0.0F);
        when(world.getWorldTime()).thenReturn(6000L);

        String first = DimensionRegistry.getConditions(world);
        String second = DimensionRegistry.getConditions(world);

        assertEquals(first, second);
    }
}
