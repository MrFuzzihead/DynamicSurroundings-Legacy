package com.mrfuzzihead.dynamicsurroundings.util.random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

/**
 * The mod's random source (used for footstep selection, aurora placement and weather timing) is a xorshift128+
 * generator rather than {@link java.util.Random}, so its determinism and ranges are part of the behaviour.
 */
public class XorShiftRandomTest {

    @Test
    public void sameSeedProducesTheSameSequence() {
        XorShiftRandom a = new XorShiftRandom(12345L);
        XorShiftRandom b = new XorShiftRandom(12345L);
        for (int i = 0; i < 100; i++) {
            assertEquals(a.nextLong(), b.nextLong());
        }
    }

    @Test
    public void differentSeedsProduceDifferentSequences() {
        XorShiftRandom a = new XorShiftRandom(1L);
        XorShiftRandom b = new XorShiftRandom(2L);
        assertNotEquals(a.nextLong(), b.nextLong());
    }

    @Test
    public void seedOfZeroIsStillUsable() {
        // The constructor remaps an all-zero state; without that the generator would emit zeros forever.
        XorShiftRandom random = new XorShiftRandom(0L);
        long first = random.nextLong();
        long second = random.nextLong();
        assertNotEquals(0L, first);
        assertNotEquals(first, second);
    }

    @Test
    public void setSeedRestartsTheSequence() {
        XorShiftRandom random = new XorShiftRandom(99L);
        long first = random.nextLong();
        random.setSeed(99L);
        assertEquals(first, random.nextLong());

        // The override must not re-seed during construction (s0/s1 are still zero then).
        XorShiftRandom fresh = new XorShiftRandom(7L);
        assertEquals(new XorShiftRandom(7L).nextLong(), fresh.nextLong());
    }

    @Test
    public void boundedIntStaysInRange() {
        XorShiftRandom random = new XorShiftRandom(4242L);
        for (int bound : new int[] { 1, 2, 7, 100, 1000 }) {
            for (int i = 0; i < 500; i++) {
                int value = random.nextInt(bound);
                assertTrue("value " + value + " out of [0," + bound + ")", value >= 0 && value < bound);
            }
        }
    }

    @Test
    public void doublesAndFloatsStayInTheUnitInterval() {
        XorShiftRandom random = new XorShiftRandom(31L);
        for (int i = 0; i < 1000; i++) {
            double d = random.nextDouble();
            float f = random.nextFloat();
            assertTrue("double " + d, d >= 0.0D && d < 1.0D);
            assertTrue("float " + f, f >= 0.0F && f < 1.0F);
        }
    }

    @Test
    public void nextBytesFillsTheWholeArray() {
        XorShiftRandom random = new XorShiftRandom(8L);
        byte[] bytes = new byte[37];
        random.nextBytes(bytes);

        byte[] zeros = new byte[37];
        assertTrue("nextBytes left the buffer untouched", !Arrays.equals(zeros, bytes));
    }

    @Test
    public void nextBytesHandlesPartialEightByteBlocks() {
        XorShiftRandom a = new XorShiftRandom(5L);
        XorShiftRandom b = new XorShiftRandom(5L);
        byte[] left = new byte[17];
        byte[] right = new byte[17];
        a.nextBytes(left);
        b.nextBytes(right);
        assertArrayEquals(left, right);
    }

    @Test
    public void gaussianIsCachedAndThenRegenerated() {
        XorShiftRandom random = new XorShiftRandom(77L);
        double first = random.nextGaussian();
        double second = random.nextGaussian();
        double third = random.nextGaussian();
        // Pairs are independent, but no value repeats three times in a row for this seed.
        assertNotEquals(first, third);
        assertNotEquals(second, third);
        assertTrue(Math.abs(first) < 10.0D);
        assertTrue(Math.abs(second) < 10.0D);
    }

    @Test
    public void currentIsOneInstancePerThread() {
        assertSame(XorShiftRandom.current(), XorShiftRandom.current());
    }

    @Test
    public void unboundedIntUsesTheWholeSignRange() {
        XorShiftRandom random = new XorShiftRandom(11L);
        boolean sawNegative = false;
        boolean sawPositive = false;
        for (int i = 0; i < 200 && !(sawNegative && sawPositive); i++) {
            int value = random.nextInt();
            sawNegative |= value < 0;
            sawPositive |= value > 0;
        }
        assertTrue("the generator should not be stuck on one side of zero", sawNegative && sawPositive);
    }
}
