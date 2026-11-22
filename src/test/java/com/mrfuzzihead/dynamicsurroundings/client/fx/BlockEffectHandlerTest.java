package com.mrfuzzihead.dynamicsurroundings.client.fx;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import net.minecraft.entity.player.EntityPlayer;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.compat.BlockPos;

public class BlockEffectHandlerTest {

    @Test
    public void sampleCountIsDerivedFromTheConfiguredRange() {
        assertEquals(999, BlockEffectHandler.checkCountForRange(16));
        assertEquals(113, BlockEffectHandler.checkCountForRange(8));
    }

    @Test
    public void stepSoundSamplesTheBlockUnderTheFeet() {
        EntityPlayer player = mock(EntityPlayer.class);
        player.posX = 0.5D;
        player.posZ = 0.5D;
        player.posY = 64.0D;

        BlockPos pos = BlockEffectHandler.stepBlockPos(player);

        assertEquals(0, pos.getX());
        assertEquals(63, pos.getY());
        assertEquals(0, pos.getZ());
    }
}
