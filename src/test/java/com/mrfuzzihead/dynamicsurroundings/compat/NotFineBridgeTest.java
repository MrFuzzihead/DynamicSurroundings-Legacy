package com.mrfuzzihead.dynamicsurroundings.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.compat.notfine.NotFineBridge;

/**
 * Same contract as the Angelica bridge: NotFine is not on the unit-test classpath, so every accessor must fall back
 * to the caller-supplied vanilla value.
 */
public class NotFineBridgeTest {

    @Test
    public void absentNotFineUsesTheVanillaFallback() {
        assertFalse(NotFineBridge.isAvailable());
        assertEquals(10, NotFineBridge.downfallDistance(10));
        assertEquals(5, NotFineBridge.downfallDistance(5));
        assertEquals(0, NotFineBridge.downfallDistance(0));
    }
}
