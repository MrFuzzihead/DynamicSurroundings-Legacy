package com.mrfuzzihead.dynamicsurroundings.client.fx;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.compat.BlockPos;

public class BlockEffectTest {

    @Test
    public void zeroChanceNeverCallsNextInt() {
        Random random = mock(Random.class);
        BlockEffect effect = new BlockEffect(0) {

            @Override
            public void doEffect(Block block, World world, BlockPos pos, Random rng) {}
        };

        assertFalse(effect.trigger(null, null, null, random));
        verify(random, never()).nextInt();
    }

    @Test
    public void positiveChanceCanTrigger() {
        Random random = mock(Random.class);
        when(random.nextInt(5)).thenReturn(0);
        BlockEffect effect = new BlockEffect(5) {

            @Override
            public void doEffect(Block block, World world, BlockPos pos, Random rng) {}
        };

        assertTrue(effect.trigger(null, null, null, random));
    }
}
