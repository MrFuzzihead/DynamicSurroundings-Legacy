package com.mrfuzzihead.dynamicsurroundings.client.sound;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.compat.BlockPos;
import com.mrfuzzihead.dynamicsurroundings.testutil.EntityTestSupport;
import com.mrfuzzihead.dynamicsurroundings.testutil.EnvironStateTestSupport;

public class SpotSoundTest {

    private final float originalScale = ModOptions.masterSoundScaleFactor;

    @Before
    public void installPlayer() {
        EntityPlayer player = mock(EntityPlayer.class);
        player.posX = 10.0D;
        player.posY = 64.0D;
        player.posZ = 20.0D;
        EntityTestSupport.setBoundingBox(player, AxisAlignedBB.getBoundingBox(9.0D, 64.0D, 19.0D, 11.0D, 66.0D, 21.0D));
        EnvironStateTestSupport.setPlayer(player);
        EnvironStateTestSupport.setTickCounter(100);
    }

    @After
    public void reset() {
        EnvironStateTestSupport.reset();
        ModOptions.masterSoundScaleFactor = this.originalScale;
    }

    @Test
    public void delayedSpotSoundUsesTheBlockCentreAndDelay() throws Exception {
        SoundEffect effect = new SoundEffect("dynamicsurroundings:test", 0.75F, 1.0F);
        SpotSound sound = new SpotSound(new BlockPos(1, 2, 3), effect, 5);

        assertEquals(1.5F, EntityTestSupport.getFloat(sound, "xPosF"), 0.0F);
        assertEquals(2.5F, EntityTestSupport.getFloat(sound, "yPosF"), 0.0F);
        assertEquals(3.5F, EntityTestSupport.getFloat(sound, "zPosF"), 0.0F);
        assertEquals(-5, sound.getTickAge());

        EnvironStateTestSupport.setTickCounter(105);
        assertEquals(0, sound.getTickAge());
        assertEquals(effect, sound.getSoundEffect());
    }

    @Test
    public void playerSpotSoundStaysNearThePlayerAndHonoursTheMasterScale() {
        SpotSound sound = new SpotSound(
            mock(EntityPlayer.class),
            new SoundEffect("dynamicsurroundings:test", 0.5F, 1.0F));

        assertEquals(0.5F, sound.getVolume(), 0.0F);
        ModOptions.masterSoundScaleFactor = 0.5F;
        assertEquals(0.25F, sound.getVolume(), 0.0F);
        assertEquals(0, sound.getTickAge());
    }
}
