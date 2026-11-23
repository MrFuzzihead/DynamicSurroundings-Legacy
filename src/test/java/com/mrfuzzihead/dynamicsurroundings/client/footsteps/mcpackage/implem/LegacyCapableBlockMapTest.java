package com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.implem;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LegacyCapableBlockMapTest {

    @Test
    public void recognisesEverySupportedLegacySeparator() {
        assertEquals(1, LegacyCapableBlockMap.findLegacySeparator("1:0"));
        assertEquals(1, LegacyCapableBlockMap.findLegacySeparator("1^0"));
        assertEquals(1, LegacyCapableBlockMap.findLegacySeparator("1.0"));
        assertEquals(1, LegacyCapableBlockMap.findLegacySeparator("1"));
    }
}
