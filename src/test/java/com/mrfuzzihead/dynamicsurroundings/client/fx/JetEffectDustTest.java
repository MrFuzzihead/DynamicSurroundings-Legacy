package com.mrfuzzihead.dynamicsurroundings.client.fx;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.compat.BlockPos;

public class JetEffectDustTest {

    @Test
    public void dustRisesFromASolidSurfaceIntoAir() {
        World world = mock(World.class);
        when(world.isAirBlock(0, 1, 0)).thenReturn(true);
        Random random = mock(Random.class);
        when(random.nextInt(1)).thenReturn(0);

        assertTrue(new JetEffect.Dust(1).trigger(mock(Block.class), world, new BlockPos(0, 0, 0), random));
    }

    @Test
    public void dustDoesNotTriggerUnderACeiling() {
        World world = mock(World.class);
        when(world.isAirBlock(0, 1, 0)).thenReturn(false);
        Random random = mock(Random.class);
        when(random.nextInt(1)).thenReturn(0);

        assertFalse(new JetEffect.Dust(1).trigger(mock(Block.class), world, new BlockPos(0, 0, 0), random));
    }
}
