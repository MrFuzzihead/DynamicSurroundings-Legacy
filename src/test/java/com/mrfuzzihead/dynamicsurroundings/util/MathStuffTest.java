package com.mrfuzzihead.dynamicsurroundings.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The mod replaces the JDK trig helpers with lookup tables (as vanilla Minecraft does) so that the client-side
 * effect math stays cheap. The tables have exact values at the quadrant boundaries and are approximate in between,
 * which is what these tests pin down.
 */
public class MathStuffTest {

    private static final float EPSILON = 1.0e-3F;

    @Test
    public void tableIsExactAtTheQuadrants() {
        assertEquals(0.0F, MathStuff.sin(0.0F), EPSILON);
        assertEquals(1.0F, MathStuff.sin((float) (Math.PI / 2)), EPSILON);
        assertEquals(0.0F, MathStuff.sin((float) Math.PI), EPSILON);
        assertEquals(-1.0F, MathStuff.sin((float) (Math.PI * 3 / 2)), EPSILON);
        assertEquals(1.0F, MathStuff.cos(0.0F), EPSILON);
        assertEquals(0.0F, MathStuff.cos((float) (Math.PI / 2)), EPSILON);
    }

    @Test
    public void sinAndCosAreApproximateAcrossAFullTurn() {
        for (int degrees = 0; degrees < 360; degrees += 15) {
            double radians = Math.toRadians(degrees);
            assertEquals("sin(" + degrees + ")", Math.sin(radians), MathStuff.sin((float) radians), 5.0e-3);
            assertEquals("cos(" + degrees + ")", Math.cos(radians), MathStuff.cos((float) radians), 5.0e-3);
        }
    }

    @Test
    public void doubleOverloadsMatchTheFloatOnes() {
        float radians = 0.75F;
        assertEquals(MathStuff.sin(radians), MathStuff.sin((double) radians), EPSILON);
        assertEquals(MathStuff.cos(radians), MathStuff.cos((double) radians), EPSILON);
    }

    @Test
    public void tanIsSineOverCosine() {
        float radians = 0.4F;
        assertEquals(MathStuff.sin(radians) / MathStuff.cos(radians), MathStuff.tan(radians), EPSILON);
    }

    @Test
    public void atan2IsApproximateButCorrectlySigned() {
        assertEquals(Math.PI / 4, MathStuff.atan2(1.0F, 1.0F), 0.02);
        assertEquals(-Math.PI / 4, MathStuff.atan2(-1.0F, 1.0F), 0.02);
        assertEquals(3 * Math.PI / 4, MathStuff.atan2(1.0F, -1.0F), 0.02);
        assertEquals(-3 * Math.PI / 4, MathStuff.atan2(-1.0F, -1.0F), 0.02);
        // The double overload funnels through the float table.
        assertEquals(MathStuff.atan2(1.0F, 2.0F), MathStuff.atan2(1.0D, 2.0D), EPSILON);
    }

    @Test
    public void degreesConvertBothWays() {
        assertEquals((float) Math.PI, MathStuff.toRadians(180.0F), EPSILON);
        assertEquals(180.0F, MathStuff.toDegrees((float) Math.PI), EPSILON);
        assertEquals(45.0F, MathStuff.toDegrees(MathStuff.toRadians(45.0F)), EPSILON);
    }

    @Test
    public void wrapDegreesKeepsAnglesInTheHalfOpenRange() {
        assertEquals(0.0F, MathStuff.wrapDegrees(0.0F), EPSILON);
        assertEquals(10.0F, MathStuff.wrapDegrees(370.0F), EPSILON);
        // 180 is normalized to -180 because the positive half is exclusive.
        assertEquals(-180.0F, MathStuff.wrapDegrees(180.0F), EPSILON);
        assertEquals(-170.0F, MathStuff.wrapDegrees(190.0F), EPSILON);
        assertEquals(170.0F, MathStuff.wrapDegrees(-190.0F), EPSILON);
        assertEquals(0.0F, MathStuff.wrapDegrees(360.0F), EPSILON);
        assertEquals(0.0D, MathStuff.wrapDegrees(720.0D), EPSILON);

        for (float angle = -1000.0F; angle <= 1000.0F; angle += 7.0F) {
            float wrapped = MathStuff.wrapDegrees(angle);
            assertTrue("wrapped " + wrapped + " out of range", wrapped >= -180.0F && wrapped < 180.0F);
        }
    }

    @Test
    public void absHandlesEveryPrimitiveType() {
        assertEquals(4.0F, MathStuff.abs(-4.0F), EPSILON);
        assertEquals(4.0D, MathStuff.abs(-4.0D), EPSILON);
        assertEquals(4L, MathStuff.abs(-4L));
        assertEquals(4, MathStuff.abs(-4));
        assertEquals(0, MathStuff.abs(0));
        assertEquals(7, MathStuff.abs(7));
    }

    @Test
    public void floorRoundsTowardsNegativeInfinity() {
        assertEquals(-1, MathStuff.floor(-0.5D));
        assertEquals(-1, MathStuff.floor(-1.0D));
        assertEquals(2, MathStuff.floor(2.9D));
        assertEquals(-3, MathStuff.floor(-2.1F));
    }

    @Test
    public void clampLimitsToTheBounds() {
        assertEquals(0.0F, MathStuff.clamp(-1.0F, 0.0F, 10.0F), EPSILON);
        assertEquals(10.0F, MathStuff.clamp(11.0F, 0.0F, 10.0F), EPSILON);
        assertEquals(5.0F, MathStuff.clamp(5.0F, 0.0F, 10.0F), EPSILON);
        assertEquals(0.0D, MathStuff.clamp(-1.0D, 0.0D, 10.0D), EPSILON);
        assertEquals(10, MathStuff.clamp(11, 0, 10));
        assertEquals(0, MathStuff.clamp(-1, 0, 10));
    }

    @Test
    public void sqrtReturnsAFloatPrecisionResult() {
        assertEquals(3.0F, MathStuff.sqrt(9.0F), EPSILON);
        assertEquals(3.0F, (float) MathStuff.sqrt(9.0D), EPSILON);
    }

    @Test
    public void logUsesExactMathsOnlyForSmallInputs() {
        // Below the 0.03 cut-over the JDK implementation is used, above it the polynomial approximation.
        assertEquals(Math.log(0.01D), MathStuff.log(0.01D), 1.0e-6);
        assertEquals(Math.log(2.0D), MathStuff.log(2.0D), 1.0e-3);
        // The polynomial's relative error grows away from 1.0; it is ~0.8% at 10.
        assertEquals(Math.log(10.0D), MathStuff.log(10.0D), 0.02);
    }

    @Test
    public void constantsMatchTheJdk() {
        assertEquals((float) Math.PI, MathStuff.PI_F, 0.0F);
        assertEquals((float) Math.E, MathStuff.E_F, 0.0F);
    }
}
