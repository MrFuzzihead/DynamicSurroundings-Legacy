package com.mrfuzzihead.dynamicsurroundings.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

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
    public void floatTrigStaysWithinToleranceForRandomAngles() {
        Random random = new Random(0x51A7AB1EL);
        for (int i = 0; i < 1000; i++) {
            double radians = (random.nextDouble() - 0.5D) * Math.PI * 8.0D;
            assertEquals(Math.sin(radians), MathStuff.sin((float) radians), 5.0e-3);
            assertEquals(Math.cos(radians), MathStuff.cos((float) radians), 5.0e-3);
        }
    }

    @Test
    public void doubleOverloadsUseTheAccurateJdkFunctions() {
        double radians = 0.75D;
        assertEquals(Math.sin(radians), MathStuff.sin(radians), 0.0D);
        assertEquals(Math.cos(radians), MathStuff.cos(radians), 0.0D);
        assertEquals(Math.tan(radians), MathStuff.tan(radians), 0.0D);
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
        assertEquals(Integer.MAX_VALUE, MathStuff.abs(Integer.MIN_VALUE));
        assertEquals(Long.MAX_VALUE, MathStuff.abs(Long.MIN_VALUE));
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
    public void sqrtPreservesDoublePrecision() {
        assertEquals(3.0F, MathStuff.sqrt(9.0F), EPSILON);
        assertEquals(Math.sqrt(2.0D), MathStuff.sqrt(2.0D), 1.0e-15);
    }

    @Test
    public void logUsesTheJdkImplementation() {
        assertEquals(Math.log(0.01D), MathStuff.log(0.01D), 0.0D);
        assertEquals(Math.log(2.0D), MathStuff.log(2.0D), 0.0D);
        assertEquals(Math.log(10.0D), MathStuff.log(10.0D), 0.0D);
    }

    @Test
    public void constantsMatchTheJdk() {
        assertEquals((float) Math.PI, MathStuff.PI_F, 0.0F);
        assertEquals((float) Math.E, MathStuff.E_F, 0.0F);
    }
}
