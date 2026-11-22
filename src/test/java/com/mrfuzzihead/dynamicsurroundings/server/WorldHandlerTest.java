package com.mrfuzzihead.dynamicsurroundings.server;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.testutil.WorldTestSupport;

public class WorldHandlerTest {

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
