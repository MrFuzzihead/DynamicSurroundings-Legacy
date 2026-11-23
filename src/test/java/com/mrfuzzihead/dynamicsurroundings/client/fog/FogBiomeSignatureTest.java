package com.mrfuzzihead.dynamicsurroundings.client.fog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import org.junit.Test;

public class FogBiomeSignatureTest {

    @Test
    public void identicalBiomeMixProducesTheSameSignature() {
        World world = mock(World.class);
        when(world.getBiomeGenForCoords(anyInt(), anyInt())).thenReturn(BiomeGenBase.plains);

        assertEquals(FogBiomeSignature.of(world, 0, 0), FogBiomeSignature.of(world, 8, 8));
    }

    @Test
    public void aDifferentCardinalBiomeChangesTheSignature() {
        World world = mock(World.class);
        when(world.getBiomeGenForCoords(anyInt(), anyInt())).thenReturn(BiomeGenBase.plains);
        when(world.getBiomeGenForCoords(25, 0)).thenReturn(BiomeGenBase.desert);

        assertNotEquals(FogBiomeSignature.of(world, 0, 0), FogBiomeSignature.of(world, 5, 0));
    }
}
