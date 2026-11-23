package com.mrfuzzihead.dynamicsurroundings.compat;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.compat.angelica.AngelicaBridge;

/**
 * The bridge is reflection-only on purpose: this test suite does not put Angelica on the classpath, which is also the
 * production case for every non-Angelica pack. The bridge must report "not available" rather than throw.
 */
public class AngelicaBridgeTest {

    @Test
    public void absentAngelicaIsANoOp() {
        assertFalse(AngelicaBridge.isAvailable());
        assertFalse(AngelicaBridge.isInstancedWeatherEnabled());
    }
}
