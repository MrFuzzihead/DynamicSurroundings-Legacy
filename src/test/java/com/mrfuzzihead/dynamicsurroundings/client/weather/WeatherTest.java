package com.mrfuzzihead.dynamicsurroundings.client.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;

public class WeatherTest {

    @After
    public void resetWeather() {
        Weather.setIntensity(-10.0F);
        ModOptions.disableWeatherEffects = false;
        ModOptions.soundLevel = 1.0F;
    }

    @Test
    public void vanillaIsTheResetSentinel() {
        Weather.setIntensity(0.5F);
        Weather.setIntensity(-10.0F);

        assertEquals(Weather.VANILLA, Weather.getIntensity());
        assertEquals(0.0F, Weather.getMaxIntensityLevel(), 0.0F);
        assertTrue(Weather.doVanilla());
    }

    @Test
    public void thresholdsSelectTheExpectedBand() {
        Weather.setIntensity(0.0F);
        assertEquals(Weather.NONE, Weather.getIntensity());

        Weather.setIntensity(0.05F);
        assertEquals(Weather.CALM, Weather.getIntensity());

        Weather.setIntensity(0.2F);
        assertEquals(Weather.LIGHT, Weather.getIntensity());

        Weather.setIntensity(0.5F);
        assertEquals(Weather.NORMAL, Weather.getIntensity());

        Weather.setIntensity(1.0F);
        assertEquals(Weather.HEAVY, Weather.getIntensity());
    }

    @Test
    public void intensityIsClampedToTheUnitRange() {
        Weather.setIntensity(-1.0F);
        assertEquals(0.0F, Weather.getMaxIntensityLevel(), 0.0F);
        assertEquals(Weather.NONE, Weather.getIntensity());

        Weather.setIntensity(2.0F);
        assertEquals(1.0F, Weather.getMaxIntensityLevel(), 0.0F);
        assertEquals(Weather.HEAVY, Weather.getIntensity());
    }

    @Test
    public void disablingWeatherFallsBackToVanillaRendering() {
        Weather.setIntensity(0.5F);
        assertFalse(Weather.doVanilla());

        ModOptions.disableWeatherEffects = true;
        assertTrue(Weather.doVanilla());
    }

    @Test
    public void volumeUsesTheConfiguredSoundLevel() {
        Weather.setIntensity(0.5F);
        ModOptions.soundLevel = 0.5F;
        assertEquals(0.25F, Weather.getCurrentVolume(), 0.0F);
    }

    @Test
    public void texturesFollowTheSelectedBand() {
        Weather.setIntensity(1.0F);
        Weather.setTextures();

        assertTrue(
            StormRenderer.locationRainPng.getResourcePath()
                .endsWith("rain_heavy.png"));
        assertTrue(
            StormRenderer.locationDustPng.getResourcePath()
                .endsWith("dust_heavy.png"));
    }
}
