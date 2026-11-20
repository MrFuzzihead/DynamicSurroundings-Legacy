package com.mrfuzzihead.dynamicsurroundings.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;

import org.junit.Before;
import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.util.DiurnalUtils.DayCycle;

/**
 * The aurora, fog and rain code all branch on the time of day. {@link DiurnalUtils} derives it from the world's sun
 * brightness, falling back to the celestial angle for the twilight window, so each branch is driven here through a
 * mocked world (Mockito is only needed because the World surface is large).
 */
public class DiurnalUtilsTest {

    private World world;
    private WorldProvider provider;

    @Before
    public void setUp() {
        world = mock(World.class);
        provider = mock(WorldProvider.class);
        // World.provider is a plain field, so it is assigned rather than stubbed.
        world.provider = provider;
    }

    @Test
    public void worldsWithoutASkyAreReportedAsSuch() {
        provider.hasNoSky = true;
        assertEquals(DayCycle.NO_SKY, DiurnalUtils.getCycle(world));
        assertFalse(DiurnalUtils.isDaytime(world));
        assertFalse(DiurnalUtils.isNighttime(world));
        assertFalse(DiurnalUtils.isSunrise(world));
        assertFalse(DiurnalUtils.isSunset(world));
        // Auroras are only hidden at sunrise and during the day.
        assertTrue(DiurnalUtils.isAuroraVisible(world));
        assertFalse(DiurnalUtils.isAuroraInvisible(world));
    }

    @Test
    public void brightSunIsDaytime() {
        provider.hasNoSky = false;
        when(provider.getSunBrightnessFactor(1.0F)).thenReturn(0.9F);
        assertEquals(DayCycle.DAYTIME, DiurnalUtils.getCycle(world));
        assertTrue(DiurnalUtils.isDaytime(world));
        assertFalse(DiurnalUtils.isNighttime(world));
        assertTrue(DiurnalUtils.isAuroraInvisible(world));
    }

    @Test
    public void darkSkyIsNighttime() {
        provider.hasNoSky = false;
        when(provider.getSunBrightnessFactor(1.0F)).thenReturn(0.05F);
        assertEquals(DayCycle.NIGHTTIME, DiurnalUtils.getCycle(world));
        assertTrue(DiurnalUtils.isNighttime(world));
        assertFalse(DiurnalUtils.isDaytime(world));
        assertTrue(DiurnalUtils.isAuroraVisible(world));
    }

    @Test
    public void twilightIsSplitByTheCelestialAngle() {
        provider.hasNoSky = false;
        when(provider.getSunBrightnessFactor(1.0F)).thenReturn(0.5F);

        // sin(angle) > 0 -> setting sun.
        when(world.getCelestialAngleRadians(1.0F)).thenReturn(1.0F);
        assertEquals(DayCycle.SUNSET, DiurnalUtils.getCycle(world));
        assertTrue(DiurnalUtils.isSunset(world));
        assertFalse(DiurnalUtils.isAuroraInvisible(world));

        // sin(angle) < 0 -> rising sun, which is when the aurora has to disappear.
        when(world.getCelestialAngleRadians(1.0F)).thenReturn(3.5F);
        assertEquals(DayCycle.SUNRISE, DiurnalUtils.getCycle(world));
        assertTrue(DiurnalUtils.isSunrise(world));
        assertTrue(DiurnalUtils.isAuroraInvisible(world));
    }

    @Test
    public void brightestThresholdIsInclusiveOfDaytime() {
        provider.hasNoSky = false;
        // Exactly 0.6 is not > 0.6, so it falls through to the twilight branch.
        when(provider.getSunBrightnessFactor(1.0F)).thenReturn(0.6F);
        when(world.getCelestialAngleRadians(1.0F)).thenReturn(1.0F);
        assertEquals(DayCycle.SUNSET, DiurnalUtils.getCycle(world));

        when(provider.getSunBrightnessFactor(1.0F)).thenReturn(0.61F);
        assertEquals(DayCycle.DAYTIME, DiurnalUtils.getCycle(world));
    }

    @Test
    public void clockTimeIsTheDayOffset() {
        when(world.getWorldTime()).thenReturn(24000L * 3 + 1000L);
        assertEquals(1000L, DiurnalUtils.getClockTime(world));
    }

    @Test
    public void moonPhaseIsDelegatedAndNullSafe() {
        when(world.getCurrentMoonPhaseFactor()).thenReturn(0.75F);
        assertEquals(0.75F, DiurnalUtils.getMoonPhaseFactor(world), 0.0F);
        assertEquals(0.0F, DiurnalUtils.getMoonPhaseFactor(null), 0.0F);
    }

    @Test
    public void nullWorldHasNoSkyCycle() {
        assertEquals(DayCycle.NO_SKY, DiurnalUtils.getCycle(null));
    }
}
