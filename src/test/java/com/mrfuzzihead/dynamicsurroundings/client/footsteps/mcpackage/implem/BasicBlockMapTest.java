package com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.implem;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class BasicBlockMapTest {

    @Test
    public void parsesBlockNameMetaAndSubstrate() {
        assertArrayEquals(
            new String[] { "minecraft:stone", "0", "solid" },
            BasicBlockMap.parseKey("minecraft:stone^0+solid"));
        assertArrayEquals(new String[] { "minecraft:stone", "12", null }, BasicBlockMap.parseKey("minecraft:stone^12"));
        assertArrayEquals(
            new String[] { "minecraft:stone", null, "solid" },
            BasicBlockMap.parseKey("minecraft:stone+solid"));
        assertArrayEquals(new String[] { "minecraft:stone", null, null }, BasicBlockMap.parseKey("minecraft:stone"));
    }

    @Test
    public void malformedKeysAreRejected() {
        assertNull(BasicBlockMap.parseKey("minecraft:stone^not-a-number"));
        assertNull(BasicBlockMap.parseKey(""));
    }
}
