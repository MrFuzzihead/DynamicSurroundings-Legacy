package com.mrfuzzihead.dynamicsurroundings.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

/**
 * The rain intensity stored per dimension drives the client rain volume and is written to the world's NBT. The
 * setters clamp against each other (a min above the max, or a max below the min, would invert the randomizer), and
 * the NBT round trip has to preserve the aurora list.
 */
public class DimensionEffectDataTest {

    @Test
    public void intensityIsClampedToTheUnitRange() {
        DimensionEffectData data = new DimensionEffectData(0);
        data.setRainIntensity(2.0F);
        assertEquals(1.0F, data.getRainIntensity(), 0.0F);
        data.setRainIntensity(-1.0F);
        assertEquals(0.0F, data.getRainIntensity(), 0.0F);
        data.setRainIntensity(0.25F);
        assertEquals(0.25F, data.getRainIntensity(), 0.0F);
    }

    @Test
    public void minCannotExceedTheMax() {
        DimensionEffectData data = new DimensionEffectData(7);
        data.setMaxRainIntensity(0.5F);
        data.setMinRainIntensity(0.9F);
        assertEquals(0.5F, data.getMinRainIntensity(), 0.0F);
        assertEquals(0.5F, data.getMaxRainIntensity(), 0.0F);
    }

    @Test
    public void maxCannotDropBelowTheMin() {
        DimensionEffectData data = new DimensionEffectData(7);
        data.setMinRainIntensity(0.6F);
        data.setMaxRainIntensity(0.2F);
        assertEquals(0.6F, data.getMaxRainIntensity(), 0.0F);
        assertEquals(0.6F, data.getMinRainIntensity(), 0.0F);
    }

    @Test
    public void randomRainUsesTheConfiguredWindow() {
        DimensionEffectData data = new DimensionEffectData(0);
        data.setMinRainIntensity(0.0F);
        data.setMaxRainIntensity(1.0F);
        for (int i = 0; i < 200; i++) {
            data.randomizeRain();
            float intensity = data.getRainIntensity();
            // The randomizer clamps to a 1% floor so that rain always renders when it starts.
            assertTrue("intensity " + intensity, intensity >= 0.01F && intensity <= 1.0F);
        }
    }

    @Test
    public void randomRainCollapsesToTheMinWhenTheWindowIsClosed() {
        DimensionEffectData data = new DimensionEffectData(0);
        data.setMinRainIntensity(0.4F);
        data.setMaxRainIntensity(0.4F);
        data.randomizeRain();
        assertEquals(0.4F, data.getRainIntensity(), 0.0F);
    }

    @Test
    public void nbtRoundTripPreservesEverything() {
        DimensionEffectData original = new DimensionEffectData(3);
        original.setRainIntensity(0.75F);
        original.setMinRainIntensity(0.25F);
        original.setMaxRainIntensity(0.85F);
        original.getAuroraList()
            .add(new AuroraData(3, 100, -200, 1234L, 2, 1));

        NBTTagCompound nbt = new NBTTagCompound();
        original.writeToNBT(nbt);

        DimensionEffectData restored = new DimensionEffectData();
        restored.readFromNBT(nbt);

        assertEquals(3, restored.getDimensionId());
        assertEquals(0.75F, restored.getRainIntensity(), 0.0F);
        assertEquals(0.25F, restored.getMinRainIntensity(), 0.0F);
        assertEquals(0.85F, restored.getMaxRainIntensity(), 0.0F);
        assertEquals(
            1,
            restored.getAuroraList()
                .size());
        // AuroraData overrides equals but not hashCode, so the set can only be searched by iterating.
        AuroraData restoredAurora = restored.getAuroraList()
            .iterator()
            .next();
        assertTrue(restoredAurora.equals(new AuroraData(3, 100, -200, 0L, 0, 0)));
        assertEquals(1234L, restoredAurora.seed);
    }

    @Test
    public void readingNbtWithoutOptionalKeysKeepsTheDefaults() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("d", 9);
        nbt.setFloat("s", 0.5F);

        DimensionEffectData data = new DimensionEffectData();
        data.readFromNBT(nbt);
        assertEquals(9, data.getDimensionId());
        assertEquals(0.5F, data.getRainIntensity(), 0.0F);
        assertTrue(data.getMinRainIntensity() <= data.getMaxRainIntensity());
        assertTrue(
            data.getAuroraList()
                .isEmpty());
    }

    @Test
    public void toStringReportsDimensionIntensityAndAuroras() {
        DimensionEffectData data = new DimensionEffectData(12);
        data.setRainIntensity(0.5F);
        data.setMinRainIntensity(0.1F);
        data.setMaxRainIntensity(0.9F);

        String text = data.toString();
        assertTrue(text, text.startsWith("dim 12: intensity: 50"));
        assertTrue(text, text.contains("[10,90]"));
        assertTrue(text, text.contains("auroras: 0"));
        assertFalse(text.contains("null"));
    }
}
