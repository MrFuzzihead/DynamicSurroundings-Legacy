package com.mrfuzzihead.dynamicsurroundings.client.sound;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundPoolEntry;
import net.minecraft.util.ResourceLocation;

import org.junit.Test;

/**
 * The two sound-manager entry points the mixin replaces are clamped arithmetic; the rest of the class is the sound
 * queue. These cover the arithmetic without a GL/OpenAL context, which is where a bad clamp would otherwise only show
 * up in-game.
 */
public class SoundManagerMathTest {

    @Test
    public void pitchIsClampedToTheVanillaRange() {
        ISound sound = sound(2.0F, 1.0F, "minecraft:test");
        SoundPoolEntry entry = poolEntry(1.0, 1.0);

        assertEquals(
            2.0F,
            com.mrfuzzihead.dynamicsurroundings.client.sound.DynamicSoundManager.getNormalizedPitch(sound, entry),
            0.0F);
    }

    @Test
    public void pitchIsRaisedWhenTheProductIsTooLow() {
        ISound sound = sound(0.1F, 1.0F, "minecraft:test");
        SoundPoolEntry entry = poolEntry(1.0, 1.0);

        assertEquals(
            0.5F,
            com.mrfuzzihead.dynamicsurroundings.client.sound.DynamicSoundManager.getNormalizedPitch(sound, entry),
            0.0F);
    }

    @Test
    public void pitchIsTheProductInsideTheClamp() {
        ISound sound = sound(0.8F, 1.0F, "minecraft:test");
        SoundPoolEntry entry = poolEntry(1.25, 1.0);

        assertEquals(
            1.0F,
            com.mrfuzzihead.dynamicsurroundings.client.sound.DynamicSoundManager.getNormalizedPitch(sound, entry),
            0.0001F);
    }

    @Test
    public void pitchDegradesToZeroForMissingInputs() {
        SoundPoolEntry entry = poolEntry(1.0, 1.0);
        assertEquals(
            0.0F,
            com.mrfuzzihead.dynamicsurroundings.client.sound.DynamicSoundManager.getNormalizedPitch(null, entry),
            0.0F);
        assertEquals(
            0.0F,
            com.mrfuzzihead.dynamicsurroundings.client.sound.DynamicSoundManager
                .getNormalizedPitch(sound(1.0F, 1.0F, "minecraft:test"), null),
            0.0F);
    }

    @Test
    public void volumeDegradesToZeroForMissingInputs() {
        SoundPoolEntry entry = poolEntry(1.0, 1.0);
        assertEquals(
            0.0F,
            com.mrfuzzihead.dynamicsurroundings.client.sound.DynamicSoundManager
                .getNormalizedVolume(null, entry, SoundCategory.MASTER),
            0.0F);
        assertEquals(
            0.0F,
            com.mrfuzzihead.dynamicsurroundings.client.sound.DynamicSoundManager
                .getNormalizedVolume(sound(1.0F, 1.0F, "minecraft:test"), null, SoundCategory.MASTER),
            0.0F);
        assertEquals(
            0.0F,
            com.mrfuzzihead.dynamicsurroundings.client.sound.DynamicSoundManager
                .getNormalizedVolume(sound(1.0F, 1.0F, "minecraft:test"), entry, null),
            0.0F);
    }

    private static ISound sound(float pitch, float volume, String name) {
        ISound sound = mock(ISound.class);
        when(sound.getPitch()).thenReturn(pitch);
        when(sound.getVolume()).thenReturn(volume);
        when(sound.getPositionedSoundLocation()).thenReturn(new ResourceLocation(name));
        return sound;
    }

    private static SoundPoolEntry poolEntry(double pitch, double volume) {
        SoundPoolEntry entry = mock(SoundPoolEntry.class);
        when(entry.getPitch()).thenReturn(pitch);
        when(entry.getVolume()).thenReturn(volume);
        return entry;
    }
}
