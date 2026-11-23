package com.mrfuzzihead.dynamicsurroundings.client.sound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.testutil.EntityTestSupport;
import com.mrfuzzihead.dynamicsurroundings.testutil.EnvironStateTestSupport;

public class PlayerSoundTest {

    private final float originalScale = ModOptions.masterSoundScaleFactor;

    @Before
    public void installPlayer() {
        EntityPlayer player = mock(EntityPlayer.class);
        when(player.isEntityAlive()).thenReturn(true);
        EntityTestSupport.setBoundingBox(player, AxisAlignedBB.getBoundingBox(0.0D, 64.0D, 0.0D, 1.0D, 66.0D, 1.0D));
        EnvironStateTestSupport.setPlayer(player);
        EnvironStateTestSupport.setTickCounter(100);
    }

    @After
    public void reset() {
        EnvironStateTestSupport.reset();
        ModOptions.masterSoundScaleFactor = this.originalScale;
    }

    @Test
    public void volumeRampsUpAndHonoursTheMasterScale() {
        SoundEffect effect = new SoundEffect("dynamicsurroundings:test", 0.5F, 1.0F);
        PlayerSound sound = new PlayerSound(effect);
        assertEquals(0.002F, sound.getVolume(), 1.0e-6F);

        sound.update();
        assertEquals(0.017F, sound.getVolume(), 1.0e-6F);

        ModOptions.masterSoundScaleFactor = 0.5F;
        assertEquals(0.0085F, sound.getVolume(), 1.0e-6F);
    }

    @Test
    public void fadeAwayEventuallyStopsTheSound() {
        PlayerSound sound = new PlayerSound(new SoundEffect("dynamicsurroundings:test", 0.5F, 1.0F));
        sound.fadeAway();
        assertTrue(sound.isFading());

        for (int tick = 100; tick < 200; tick++) {
            EnvironStateTestSupport.setTickCounter(tick);
            sound.update();
        }

        assertTrue(sound.isDonePlaying());
        assertEquals(0.0F, sound.getVolume(), 0.0F);
    }

    @Test
    public void aFadingSoundIgnoresVolumeIncreases() {
        PlayerSound sound = new PlayerSound(new SoundEffect("dynamicsurroundings:test", 0.5F, 1.0F));
        sound.fadeAway();
        sound.setVolume(1.0F);
        assertEquals(0.002F, sound.getVolume(), 1.0e-6F);
        assertFalse(sound.sameSound(new SoundEffect("dynamicsurroundings:other")));
    }
}
