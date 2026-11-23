package com.mrfuzzihead.dynamicsurroundings.client.weather;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.testutil.WorldTestSupport;

public class NetherSplashRendererTest {

    @Test
    public void findsTheFirstSolidBelowTheViewEntity() {
        World world = mock(World.class);
        WorldProvider provider = mock(WorldProvider.class);
        provider.dimensionId = -1;
        WorldTestSupport.setProvider(world, provider);
        when(world.isAirBlock(anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> {
            int y = ((Number) invocation.getArguments()[1]).intValue();
            return y >= 64;
        });
        Block stone = mock(Block.class);
        when(stone.getMaterial()).thenReturn(Material.rock);
        when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(stone);

        EntityLivingBase view = mock(EntityLivingBase.class);
        view.posY = 64.0;

        assertEquals(64, new NetherSplashRenderer().getPrecipitationHeight(world, 5, 0, 0, view));
    }
}
