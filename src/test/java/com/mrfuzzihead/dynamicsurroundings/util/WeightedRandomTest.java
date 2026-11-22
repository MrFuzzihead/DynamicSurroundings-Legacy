package com.mrfuzzihead.dynamicsurroundings.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public class WeightedRandomTest {

    @Test
    public void zeroAndNegativeChancesNeverRoll() {
        Random random = mock(Random.class);
        assertFalse(WeightedRandom.roll(random, 0));
        assertFalse(WeightedRandom.roll(random, -5));
        verify(random, never()).nextInt();
    }

    @Test
    public void aPositiveChanceRollsZero() {
        Random random = mock(Random.class);
        when(random.nextInt(10)).thenReturn(0);
        assertTrue(WeightedRandom.roll(random, 10));
    }

    @Test
    public void pickIndexRejectsAnEmptyWeightTable() {
        Random random = mock(Random.class);
        assertEquals(-1, WeightedRandom.pickIndex(new int[] { 0, -1 }, random));
        verify(random, never()).nextInt();
    }

    @Test
    public void pickIndexIgnoresNegativeWeights() {
        Random random = mock(Random.class);
        when(random.nextInt(5)).thenReturn(4);
        assertEquals(1, WeightedRandom.pickIndex(new int[] { -100, 5 }, random));
    }

    @Test
    public void pickIndexUsesTheExpectedInverseCumulativeBuckets() {
        Random random = new Random(12345L);
        int[] counts = new int[2];
        for (int i = 0; i < 11000; i++) {
            counts[WeightedRandom.pickIndex(new int[] { 10, 1 }, random)]++;
        }

        assertTrue("weight 10 should dominate", counts[0] > counts[1] * 7);
        assertTrue("weight 1 should still appear", counts[1] > 0);
    }

    @Test
    public void pickReturnsNullWhenNoCandidateMatches() {
        Random random = mock(Random.class);
        assertNull(WeightedRandom.pick(Arrays.asList("a", "b"), s -> 0, random));
        assertNull(WeightedRandom.pick(Arrays.asList(), s -> 1, random));
    }

    @Test
    public void pickSelectsTheOnlyPositiveWeight() {
        Random random = mock(Random.class);
        when(random.nextInt(3)).thenReturn(2);
        assertEquals("b", WeightedRandom.pick(Arrays.asList("a", "b"), s -> "b".equals(s) ? 3 : 0, random));
    }
}
