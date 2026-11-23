package com.mrfuzzihead.dynamicsurroundings.client;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.client.fog.FogResult;

public class FogEffectHandlerTest {

    @Test
    public void scalesTheExternalBaseInsteadOfReplacingIt() {
        FogResult result = new FogResult(0, 0.5F, 0.8F);

        assertArrayEquals(new float[] { 0.5F, 80.0F }, FogEffectHandler.computeRange(result, 1.0F, 100.0F), 0.0F);
    }

    @Test
    public void keepsNotFineTerrainFogEffectivelyOff() {
        FogResult result = new FogResult(0, 0.4F, 0.8F);

        float[] range = FogEffectHandler.computeRange(result, 15_000_000.0F, 16_000_000.0F);

        assertArrayEquals(new float[] { 6_000_000.0F, 12_800_000.0F }, range, 0.0F);
    }

    @Test
    public void rejectsAnEmptyOrInvertedBase() {
        FogResult result = new FogResult(0, 0.5F, 0.8F);
        assertNull(FogEffectHandler.computeRange(result, 100.0F, 100.0F));
        assertNull(FogEffectHandler.computeRange(result, 100.0F, 50.0F));
        assertNull(FogEffectHandler.computeRange(result, 0.0F, 0.0F));
    }

    @Test
    public void rejectsANoOpResultWhenTheRangeWouldStayEmpty() {
        assertNull(FogEffectHandler.computeRange(new FogResult(0, 1.0F, 1.0F), 1.0F, 100.0F));
    }
}
