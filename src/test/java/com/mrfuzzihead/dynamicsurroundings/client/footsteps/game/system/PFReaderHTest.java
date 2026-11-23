package com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayer;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.EventType;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.implem.NormalVariator;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces.IIsolator;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces.ISolver;
import com.mrfuzzihead.dynamicsurroundings.util.MyUtils;

public class PFReaderHTest {

    @Test
    public void aFullStepProducesOneWalkAssociation() {
        IIsolator isolator = mock(IIsolator.class);
        ISolver solver = mock(ISolver.class);
        EntityPlayer player = player(0.96F);
        Association association = mock(Association.class);
        when(isolator.getSolver()).thenReturn(solver);
        when(solver.hasSpecialStoppingConditions(player)).thenReturn(false);
        when(solver.playSpecialStoppingConditions(player)).thenReturn(false);
        when(solver.findAssociationForPlayer(eq(player), anyDouble(), anyBoolean())).thenReturn(association);
        when(solver.findAssociationForBlock(anyInt(), anyInt(), anyInt(), anyString())).thenReturn(null);

        TestReader reader = new TestReader(isolator);
        reader.VAR.PLAY_WANDER = false;
        reader.setYPosition(64.0D);
        reader.generateFootsteps(player);

        verify(solver).playAssociation(player, association, EventType.RUN);
    }

    @Test
    public void speedDisambiguatorUsesTheSquaredHorizontalSpeed() {
        TestReader reader = new TestReader(mock(IIsolator.class));
        EntityPlayer walking = mock(EntityPlayer.class);
        walking.motionX = 0.1D;
        walking.motionZ = 0.0D;
        EntityPlayer running = mock(EntityPlayer.class);
        running.motionX = 0.2D;
        running.motionZ = 0.0D;

        assertEquals(EventType.WALK, reader.speedDisambiguator(walking, EventType.WALK, EventType.RUN));
        assertEquals(EventType.RUN, reader.speedDisambiguator(running, EventType.WALK, EventType.RUN));
    }

    @Test
    public void immobileDetectionOnlyFiresAfterTheDuration() {
        TestReader reader = new TestReader(mock(IIsolator.class));
        reader.VAR.IMMOBILE_DURATION = 1;
        reader.setLastReference(1.0F);
        reader.setImmobile(true);
        reader.setTimeImmobile(MyUtils.currentTimeMillis() - 10L);

        assertTrue(reader.stoppedImmobile(2.0F));
        assertFalse(reader.stoppedImmobile(2.0F));
    }

    @Test
    public void scalingClampsOutsideTheWindow() {
        TestReader reader = new TestReader(mock(IIsolator.class));

        assertEquals(0.0F, reader.scalex(-1.0F, 0.0F, 10.0F), 0.0F);
        assertEquals(1.0F, reader.scalex(11.0F, 0.0F, 10.0F), 0.0F);
        assertEquals(0.5F, reader.scalex(5.0F, 0.0F, 10.0F), 0.0F);
    }

    @Test
    public void reevaluateDistanceKeepsTheConfiguredDistance() {
        TestReader reader = new TestReader(mock(IIsolator.class));
        assertEquals(0.75F, reader.reevaluateDistance(EventType.WALK, 0.75F), 0.0F);
    }

    private static EntityPlayer player(float distance) {
        EntityPlayer player = mock(EntityPlayer.class);
        player.distanceWalkedOnStepModified = distance;
        player.onGround = true;
        player.motionX = 0.2D;
        player.motionZ = 0.0D;
        player.posX = 0.0D;
        player.posY = 64.0D;
        player.posZ = 0.0D;
        when(player.isInWater()).thenReturn(false);
        when(player.isOnLadder()).thenReturn(false);
        when(player.isSneaking()).thenReturn(false);
        when(player.getYOffset()).thenReturn(0.0D);
        return player;
    }

    private static final class TestReader extends PFReaderH {

        private TestReader(IIsolator isolator) {
            super(isolator);
            this.VAR = new NormalVariator();
        }

        private void setLastReference(float value) {
            this.lastReference = value;
        }

        private void setImmobile(boolean value) {
            this.isImmobile = value;
        }

        private void setTimeImmobile(long value) {
            this.timeImmobile = value;
        }

        private void setYPosition(double value) {
            this.yPosition = value;
        }
    }
}
