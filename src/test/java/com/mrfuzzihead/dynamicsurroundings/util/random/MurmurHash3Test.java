package com.mrfuzzihead.dynamicsurroundings.util.random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * {@link MurmurHash3} seeds the xorshift generator; it is package-private and only used for that, so the tests pin
 * the two properties the generator relies on: the hash of zero must be zero-safe (handled by the generator) and the
 * output must be deterministic and well distributed for nearby inputs.
 */
public class MurmurHash3Test {

    @Test
    public void isDeterministic() {
        for (int i = -1000; i <= 1000; i++) {
            assertEquals(MurmurHash3.hash(i), MurmurHash3.hash(i));
        }
        for (long i = -1000L; i <= 1000L; i++) {
            assertEquals(MurmurHash3.hash(i), MurmurHash3.hash(i));
        }
    }

    @Test
    public void zeroHashesToZero() {
        assertEquals(0, MurmurHash3.hash(0));
        assertEquals(0L, MurmurHash3.hash(0L));
    }

    @Test
    public void nearbyInputsAvalanche() {
        assertNotEquals(MurmurHash3.hash(1), MurmurHash3.hash(2));
        assertNotEquals(MurmurHash3.hash(0), MurmurHash3.hash(1));
        assertNotEquals(MurmurHash3.hash(1_000_000L), MurmurHash3.hash(1_000_001L));
    }

    @Test
    public void intAndLongHashesDoNotCollideOnTheSameValue() {
        // They are different functions over different widths.
        assertNotEquals((long) MurmurHash3.hash(1), MurmurHash3.hash(1L));
    }
}
