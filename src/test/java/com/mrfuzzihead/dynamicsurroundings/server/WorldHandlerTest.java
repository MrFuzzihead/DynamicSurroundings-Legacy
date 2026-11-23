package com.mrfuzzihead.dynamicsurroundings.server;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.testutil.WorldTestSupport;

public class WorldHandlerTest {

    @Test
    public void weatherIntervalsUseTheInjectedRng() {
        Random random = mock(Random.class);
        when(random.nextInt(12000)).thenReturn(100);
        assertEquals(3700, WorldHandler.nextThunderInterval(true, random));

        when(random.nextInt(168000)).thenReturn(200);
        assertEquals(12200, WorldHandler.nextRainInterval(false, random));
    }

    @Test
    public void weatherlessNoSkyDimensionsAreLeftAlone() {
        World world = mock(World.class);
        WorldProvider provider = mock(WorldProvider.class);
        provider.dimensionId = 9999;
        provider.hasNoSky = true;
        WorldTestSupport.setProvider(world, provider);
        world.isRemote = false;

        WorldHandler.updateWeatherBody(world);

        verify(world, never()).getWorldInfo();
    }
}
