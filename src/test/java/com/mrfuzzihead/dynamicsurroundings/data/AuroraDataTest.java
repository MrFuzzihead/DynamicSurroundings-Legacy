package com.mrfuzzihead.dynamicsurroundings.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class AuroraDataTest {

    @Test
    public void nbtRoundTripPreservesEveryField() {
        AuroraData original = new AuroraData(-1, 320, -640, 987654321L, 3, 2);
        NBTTagCompound nbt = new NBTTagCompound();
        original.writeToNBT(nbt);

        AuroraData restored = new AuroraData();
        restored.readFromNBT(nbt);

        assertEquals(-1, restored.dimensionId);
        assertEquals(320, restored.posX);
        assertEquals(-640, restored.posZ);
        assertEquals(987654321L, restored.seed);
        assertEquals(3, restored.colorSet);
        assertEquals(2, restored.preset);
    }

    @Test
    public void equalityIsByDimensionAndColumnOnly() {
        AuroraData a = new AuroraData(0, 10, 20, 1L, 0, 0);
        AuroraData b = new AuroraData(0, 10, 20, 999L, 5, 5);
        AuroraData otherDimension = new AuroraData(1, 10, 20, 1L, 0, 0);
        AuroraData otherColumn = new AuroraData(0, 11, 20, 1L, 0, 0);

        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertFalse(a.equals(otherDimension));
        assertFalse(a.equals(otherColumn));
        assertFalse(a.equals(null));
        assertFalse(a.equals("not aurora data"));
    }

    @Test
    public void sameColumnIgnoresMutableAuroraDetails() {
        AuroraData a = new AuroraData(0, 10, 20, 1L, 0, 0);
        AuroraData b = new AuroraData(0, 10, 20, 999L, 5, 5);
        assertTrue(a.sameColumn(b));
        assertFalse(a.sameColumn(new AuroraData(0, 11, 20, 1L, 0, 0)));
    }

    @Test
    public void equalColumnsHaveEqualHashCodes() {
        AuroraData a = new AuroraData(0, 10, 20, 1L, 0, 0);
        AuroraData b = new AuroraData(0, 10, 20, 999L, 5, 5);
        AuroraData otherDimension = new AuroraData(1, 10, 20, 1L, 0, 0);
        AuroraData otherColumn = new AuroraData(0, 11, 20, 1L, 0, 0);

        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(a.hashCode() != otherDimension.hashCode() || a.hashCode() != otherColumn.hashCode());
    }

    @Test
    public void toStringReportsTheColumn() {
        String text = new AuroraData(0, 15, -30, 42L, 1, 2).toString();
        assertTrue(text, text.contains("[x: 15, z: -30]"));
        assertTrue(text, text.contains("color: 1"));
        assertTrue(text, text.contains("preset: 2"));
        assertTrue(text, text.contains("seed: 42"));
    }
}
