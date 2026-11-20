package com.mrfuzzihead.dynamicsurroundings.util.random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LCGRandomTest {

    @Test
    public void sameSeedProducesTheSameSequence() {
        LCGRandom a = new LCGRandom(1234);
        LCGRandom b = new LCGRandom(1234);
        for (int i = 0; i < 100; i++) {
            assertEquals(a.nextInt(1000), b.nextInt(1000));
        }
    }

    @Test
    public void differentSeedsProduceDifferentSequences() {
        assertNotEquals(new LCGRandom(1).nextInt(Integer.MAX_VALUE), new LCGRandom(2).nextInt(Integer.MAX_VALUE));
    }

    @Test
    public void boundedResultStaysBelowTheBound() {
        LCGRandom random = new LCGRandom(9);
        for (int bound : new int[] { 2, 5, 17, 512 }) {
            for (int i = 0; i < 500; i++) {
                int value = random.nextInt(bound);
                assertTrue("value " + value + " out of [0," + bound + ")", value >= 0 && value < bound);
            }
        }
    }

    @Test
    public void zeroSeedIsUsable() {
        LCGRandom random = new LCGRandom(0);
        int first = random.nextInt(100);
        int second = random.nextInt(100);
        // The recurrence advances the state even when the seed is zero.
        assertNotEquals(new LCGRandom(0).nextInt(1000), new LCGRandom(1).nextInt(1000));
        assertTrue(first >= 0 && first < 100);
        assertTrue(second >= 0 && second < 100);
    }
}
