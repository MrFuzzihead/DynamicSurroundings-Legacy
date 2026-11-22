package com.mrfuzzihead.dynamicsurroundings.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.world.biome.BiomeGenBase;

import org.junit.Test;

public class BiomeSurveyHandlerTest {

    @Test
    public void yMovementDoesNotInvalidateTheSurvey() {
        assertFalse(
            BiomeSurveyHandler.shouldSurvey(0, 10, 20, BiomeGenBase.plains, 0, 10, 20, BiomeGenBase.plains, 20, 0));
    }

    @Test
    public void movementIsThrottledUntilTheSurveyInterval() {
        assertFalse(
            BiomeSurveyHandler.shouldSurvey(0, 11, 20, BiomeGenBase.plains, 0, 10, 20, BiomeGenBase.plains, 1, 0));
        assertTrue(
            BiomeSurveyHandler.shouldSurvey(0, 11, 20, BiomeGenBase.plains, 0, 10, 20, BiomeGenBase.plains, 20, 0));
    }

    @Test
    public void dimensionPositionOrBiomeChangeInvalidatesTheSurvey() {
        assertTrue(
            BiomeSurveyHandler.shouldSurvey(1, 10, 20, BiomeGenBase.plains, 0, 10, 20, BiomeGenBase.plains, 1, 0));
        assertFalse(
            BiomeSurveyHandler.shouldSurvey(0, 11, 20, BiomeGenBase.plains, 0, 10, 20, BiomeGenBase.plains, 1, 0));
        assertFalse(
            BiomeSurveyHandler.shouldSurvey(0, 10, 21, BiomeGenBase.plains, 0, 10, 20, BiomeGenBase.plains, 1, 0));
        assertTrue(
            BiomeSurveyHandler.shouldSurvey(0, 10, 20, BiomeGenBase.desert, 0, 10, 20, BiomeGenBase.plains, 1, 0));
    }
}
