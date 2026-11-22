package com.mrfuzzihead.dynamicsurroundings.client.sound;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SoundEffectTest {

    @Test
    public void programmaticEffectsAlwaysMatchBecauseTheirPatternIsCompiled() {
        SoundEffect effect = new SoundEffect("dynamicsurroundings:test", 0.5F, 1.0F);
        assertTrue(effect.matches("#day#Overworld#"));
        assertTrue(effect.matches("anything"));
    }
}
