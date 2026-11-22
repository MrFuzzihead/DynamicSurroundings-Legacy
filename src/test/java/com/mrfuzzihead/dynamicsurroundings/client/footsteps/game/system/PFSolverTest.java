package com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayer;

import org.junit.Test;

public class PFSolverTest {

    @Test
    public void flatGroundIsNotTreatedAsStationaryAirborne() {
        EntityPlayer player = player(false, 0.0);
        player.onGround = true;
        assertFalse(PFSolver.isStationaryAirborne(player));
    }

    @Test
    public void airborneWithNoVerticalMotionIsSkipped() {
        assertTrue(PFSolver.isStationaryAirborne(player(false, 0.0)));
        assertFalse(PFSolver.isStationaryAirborne(player(false, 0.2)));
    }

    @Test
    public void waterAndLaddersAreNotSkipped() {
        EntityPlayer inWater = player(false, 0.0);
        when(inWater.isInWater()).thenReturn(true);
        assertFalse(PFSolver.isStationaryAirborne(inWater));

        EntityPlayer onLadder = player(false, 0.0);
        when(onLadder.isOnLadder()).thenReturn(true);
        assertFalse(PFSolver.isStationaryAirborne(onLadder));
    }

    private static EntityPlayer player(boolean onGround, double motionY) {
        EntityPlayer player = mock(EntityPlayer.class);
        player.onGround = onGround;
        player.motionY = motionY;
        return player;
    }
}
