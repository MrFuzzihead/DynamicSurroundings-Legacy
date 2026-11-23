package com.mrfuzzihead.dynamicsurroundings.client.sound;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class SoundManagerChannelTest {

    @Test
    public void autoConfigurationSplitsTheDeviceChannels() {
        assertArrayEquals(new int[] { 77, 19, 128 }, DynamicSoundManager.resolveChannels(128, true, 28, 4));
    }

    @Test
    public void manualOrSmallDevicesKeepTheConfiguredCounts() {
        assertArrayEquals(new int[] { 28, 4, 128 }, DynamicSoundManager.resolveChannels(128, false, 28, 4));
        assertArrayEquals(new int[] { 28, 4, 64 }, DynamicSoundManager.resolveChannels(64, true, 28, 4));
    }
}
