package com.mrfuzzihead.dynamicsurroundings.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import org.junit.Test;

public class EnvironmentSnapshotTest {

    @Test
    public void builderCopiesEveryFieldIntoAnImmutableView() {
        EntityPlayer player = mock(EntityPlayer.class);
        World world = mock(World.class);
        BiomeGenBase biome = mock(BiomeGenBase.class);

        EnvironmentSnapshot snapshot = EnvironmentSnapshot.builder()
            .player(player)
            .world(world)
            .playerBiome(biome)
            .biomeName("Test Biome")
            .dimensionId(7)
            .dimensionName("Test Dimension")
            .conditions("#day#Test#")
            .freezing(true)
            .foggy(true)
            .humid(true)
            .dry(true)
            .inside(true)
            .temperatureCategory("tcwarm")
            .tickCounter(42)
            .playerOnGround(true)
            .playerMoving(true)
            .build();

        assertSame(player, snapshot.getPlayer());
        assertSame(world, snapshot.getWorld());
        assertSame(biome, snapshot.getPlayerBiome());
        assertEquals("Test Biome", snapshot.getBiomeName());
        assertEquals(7, snapshot.getDimensionId());
        assertEquals("Test Dimension", snapshot.getDimensionName());
        assertEquals("#day#Test#", snapshot.getConditions());
        assertTrue(snapshot.isFreezing());
        assertTrue(snapshot.isFoggy());
        assertTrue(snapshot.isHumid());
        assertTrue(snapshot.isDry());
        assertTrue(snapshot.isInside());
        assertEquals("tcwarm", snapshot.getTemperatureCategory());
        assertEquals(42, snapshot.getTickCounter());
        assertTrue(snapshot.isPlayerOnGround());
        assertTrue(snapshot.isPlayerMoving());
    }

    @Test
    public void toBuilderPreservesTheExistingState() {
        EnvironmentSnapshot snapshot = EnvironmentSnapshot.builder()
            .biomeName("Original")
            .tickCounter(3)
            .playerOnGround(true)
            .build();

        EnvironmentSnapshot changed = snapshot.toBuilder()
            .biomeName("Changed")
            .build();

        assertEquals("Original", snapshot.getBiomeName());
        assertEquals("Changed", changed.getBiomeName());
        assertEquals(3, changed.getTickCounter());
        assertTrue(changed.isPlayerOnGround());
        assertFalse(EnvironmentSnapshot.EMPTY.isPlayerOnGround());
    }
}
