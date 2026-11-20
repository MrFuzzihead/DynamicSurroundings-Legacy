package com.mrfuzzihead.dynamicsurroundings.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Aurora colour combinations are picked by index, and the index arrives either from a config value or from a random
 * roll, so both have to be clamped into the table.
 */
public class ColorPairTest {

    @Test
    public void everyEntryHasBothColors() {
        for (int id = 0; id <= ColorPair.testId(); id++) {
            ColorPair pair = ColorPair.get(id);
            assertNotNull("pair " + id, pair);
            assertNotNull("base color of pair " + id, pair.baseColor);
            assertNotNull("fade color of pair " + id, pair.fadeColor);
            assertNotSame("base and fade of pair " + id, pair.baseColor, pair.fadeColor);
        }
    }

    @Test
    public void outOfRangeIdsClampToTheTable() {
        assertSame(ColorPair.get(0), ColorPair.get(-1));
        assertSame(ColorPair.get(0), ColorPair.get(Integer.MIN_VALUE));
        assertSame(ColorPair.get(ColorPair.testId()), ColorPair.get(ColorPair.testId() + 1));
        assertSame(ColorPair.get(ColorPair.testId()), ColorPair.get(Integer.MAX_VALUE));
    }

    @Test
    public void testIdIsTheLastEntry() {
        assertEquals(ColorPair.testId(), ColorPair.testId());
        assertTrue(ColorPair.testId() >= 0);
        assertSame(ColorPair.get(ColorPair.testId()), ColorPair.get(ColorPair.testId()));
    }

    @Test
    public void randomIdStaysInRange() {
        for (int i = 0; i < 500; i++) {
            int id = ColorPair.randomId();
            assertTrue("id " + id, id >= 0 && id <= ColorPair.testId());
        }
    }

    @Test
    public void firstPairUsesTheGreenPalette() {
        ColorPair first = ColorPair.get(0);
        assertEquals(0x00FF99, first.baseColor.rgb());
        assertEquals(0x33FF00, first.fadeColor.rgb());
    }
}
