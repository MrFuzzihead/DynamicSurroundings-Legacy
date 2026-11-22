package com.mrfuzzihead.dynamicsurroundings.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.junit.Test;

public class PotionParticleScrubHandlerTest {

    @Test
    public void alreadyScrubbedPlayersAreLeftAlone() {
        DataWatcher watcher = mock(DataWatcher.class);
        when(watcher.getWatchableObjectInt(7)).thenReturn(0);
        EntityPlayer player = mock(EntityPlayer.class);
        when(player.getDataWatcher()).thenReturn(watcher);

        new PotionParticleScrubHandler().process(mock(World.class), player);

        verify(watcher, never()).updateObject(7, 0);
    }

    @Test
    public void colouredPotionDataIsScrubbed() {
        DataWatcher watcher = mock(DataWatcher.class);
        when(watcher.getWatchableObjectInt(7)).thenReturn(0x123456);
        assertTrue(PotionParticleScrubHandler.needsScrub(watcher));
        EntityPlayer player = mock(EntityPlayer.class);
        when(player.getDataWatcher()).thenReturn(watcher);

        new PotionParticleScrubHandler().process(mock(World.class), player);

        verify(watcher).updateObject(7, 0);
    }

    @Test
    public void zeroPotionDataDoesNotNeedScrubbing() {
        DataWatcher watcher = mock(DataWatcher.class);
        when(watcher.getWatchableObjectInt(7)).thenReturn(0);
        assertFalse(PotionParticleScrubHandler.needsScrub(watcher));
    }
}
