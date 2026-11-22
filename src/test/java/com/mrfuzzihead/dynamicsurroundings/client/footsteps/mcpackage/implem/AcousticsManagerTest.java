package com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.implem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

public class AcousticsManagerTest {

    @Test
    public void delayStaysInsideTheConfiguredWindow() {
        Random random = new Random(1234L);
        for (int i = 0; i < 1000; i++) {
            long delay = AcousticsManager.randomDelay(random, 100L, 200L);
            assertTrue("delay " + delay, delay >= 100L && delay <= 200L);
        }
    }

    @Test
    public void invertedOrEqualBoundsCollapseToTheLowerBound() {
        Random random = new Random(1L);
        assertEquals(100L, AcousticsManager.randomDelay(random, 100L, 100L));
        assertEquals(200L, AcousticsManager.randomDelay(random, 200L, 100L));
    }
}
