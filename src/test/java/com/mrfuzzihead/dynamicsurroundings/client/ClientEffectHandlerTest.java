package com.mrfuzzihead.dynamicsurroundings.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.junit.Test;

public class ClientEffectHandlerTest {

    @Test
    public void handlersOnlyRunWithBothAWorldAndAPlayer() {
        assertFalse(ClientEffectHandler.canProcess(null, null));
        assertFalse(ClientEffectHandler.canProcess(mock(World.class), null));
        assertFalse(ClientEffectHandler.canProcess(null, mock(EntityPlayer.class)));
        assertTrue(ClientEffectHandler.canProcess(mock(World.class), mock(EntityPlayer.class)));
    }
}
