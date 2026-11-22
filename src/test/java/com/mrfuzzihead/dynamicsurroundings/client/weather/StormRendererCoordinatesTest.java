package com.mrfuzzihead.dynamicsurroundings.client.weather;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StormRendererCoordinatesTest {

    @Test
    public void centreColumnHasFiniteCoordinates() {
        assertTrue(Float.isFinite(StormRenderer.rainXAt(16, 16)));
        assertTrue(Float.isFinite(StormRenderer.rainYAt(16, 16)));
    }
}
