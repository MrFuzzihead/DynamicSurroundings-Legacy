package com.mrfuzzihead.dynamicsurroundings.client.sound;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class SoundManagerQueueTest {

    @Test
    public void queueDoesNotMutateTheCallersList() {
        SoundEffect effect = new SoundEffect("dynamicsurroundings:test");
        SoundManager.queueAmbientSounds(Collections.unmodifiableList(Arrays.asList(effect)));
        SoundManager.clearSounds();
    }
}
