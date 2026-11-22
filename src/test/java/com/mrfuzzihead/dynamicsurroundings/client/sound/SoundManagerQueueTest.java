package com.mrfuzzihead.dynamicsurroundings.client.sound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.Test;

public class SoundManagerQueueTest {

    @After
    public void cleanup() {
        SoundManager.clearSounds();
    }

    @Test
    public void queueDoesNotMutateTheCallersList() {
        SoundEffect effect = new SoundEffect("dynamicsurroundings:test");
        SoundManager.queueAmbientSounds(Collections.unmodifiableList(Arrays.asList(effect)));
        assertEquals(
            1,
            SoundManager.getSounds()
                .size());
    }

    @Test
    public void duplicateNamesCollapseToOneEmitter() {
        SoundEffect quiet = new SoundEffect("dynamicsurroundings:duplicate", 0.2F, 1.0F);
        SoundEffect loud = new SoundEffect("dynamicsurroundings:duplicate", 0.8F, 1.0F);

        SoundManager.queueAmbientSounds(Arrays.asList(quiet, loud));

        assertEquals(
            1,
            SoundManager.getSounds()
                .size());
    }

    @Test
    public void channelCapacityCountsStreamingChannels() {
        assertTrue(SoundManager.canFitSound(24, 28, 4));
        assertFalse(SoundManager.canFitSound(26, 28, 4));
    }
}
