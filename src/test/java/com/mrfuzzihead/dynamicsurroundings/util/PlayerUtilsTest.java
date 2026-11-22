package com.mrfuzzihead.dynamicsurroundings.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.junit.Test;

public class PlayerUtilsTest {

    @Test
    public void openSkyIsOutsideAndTheCoverageThresholdDetectsACeiling() {
        World world = mock(World.class);
        EntityPlayer player = mock(EntityPlayer.class);
        when(player.getEntityWorld()).thenReturn(world);
        player.posX = 0.0;
        player.posY = 64.0;
        player.posZ = 0.0;

        when(world.getTopSolidOrLiquidBlock(anyInt(), anyInt())).thenReturn(100);
        assertTrue(PlayerUtils.isReallyInside(player));

        when(world.getTopSolidOrLiquidBlock(anyInt(), anyInt())).thenReturn(64);
        assertFalse(PlayerUtils.isReallyInside(player));
    }
}
