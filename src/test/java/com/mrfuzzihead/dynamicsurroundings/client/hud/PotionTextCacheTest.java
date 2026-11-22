package com.mrfuzzihead.dynamicsurroundings.client.hud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import net.minecraft.potion.PotionEffect;

import org.junit.Test;

public class PotionTextCacheTest {

    @Test
    public void durationChangesOnlyWhenTheDisplayedSecondChanges() {
        PotionTextCache.clear();
        PotionEffect first = new PotionEffect(1, 100, 0);
        String firstText = PotionTextCache.duration(first);

        assertEquals(firstText, PotionTextCache.duration(new PotionEffect(1, 101, 0)));
        assertNotEquals(firstText, PotionTextCache.duration(new PotionEffect(1, 120, 0)));
    }

}
