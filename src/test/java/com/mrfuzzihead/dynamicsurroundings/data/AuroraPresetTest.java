package com.mrfuzzihead.dynamicsurroundings.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AuroraPresetTest {

    @Test
    public void outOfRangeIdsClampToTheTable() {
        assertSame(AuroraPreset.get(0), AuroraPreset.get(-5));
        assertSame(AuroraPreset.get(AuroraPreset.testId()), AuroraPreset.get(AuroraPreset.testId() + 5));
    }

    @Test
    public void everyPresetIsUsable() {
        for (int id = 0; id <= AuroraPreset.testId(); id++) {
            AuroraPreset preset = AuroraPreset.get(id);
            assertTrue("length of preset " + id, preset.length == 64 || preset.length == 128);
            assertTrue("node length of preset " + id, preset.nodeLength > 0.0F);
            assertTrue("node width of preset " + id, preset.nodeWidth > 0.0F);
            assertTrue("band offset of preset " + id, preset.bandOffset >= 0);
        }
    }

    @Test
    public void randomIdStaysInRange() {
        for (int i = 0; i < 500; i++) {
            int id = AuroraPreset.randomId();
            assertTrue("id " + id, id >= 0 && id <= AuroraPreset.testId());
        }
    }

    @Test
    public void firstPresetIsTheLongWideOne() {
        AuroraPreset first = AuroraPreset.get(0);
        assertEquals(128, first.length);
        assertEquals(30.0F, first.nodeLength, 0.0F);
        assertEquals(2.0F, first.nodeWidth, 0.0F);
        assertEquals(45, first.bandOffset);
    }

    @Test
    public void presetsAreSingletons() {
        assertSame(AuroraPreset.get(1), AuroraPreset.get(1));
        assertNotSame(AuroraPreset.get(0), AuroraPreset.get(1));
    }
}
