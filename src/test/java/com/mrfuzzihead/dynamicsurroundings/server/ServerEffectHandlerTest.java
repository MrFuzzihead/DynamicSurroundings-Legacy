package com.mrfuzzihead.dynamicsurroundings.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.data.AuroraData;

public class ServerEffectHandlerTest {

    @Test
    public void auroraAtTheSpawnTargetIsInRange() {
        Set<AuroraData> data = Collections.singleton(new AuroraData(0, 0, -150, 1L, 0, 0));
        assertTrue(ServerEffectHandler.isAuroraInRange(0, 0, 150, data));
    }

    @Test
    public void auroraBeyondTheRadiusIsOutOfRange() {
        Set<AuroraData> data = Collections.singleton(new AuroraData(0, 0, 500, 1L, 0, 0));
        assertFalse(ServerEffectHandler.isAuroraInRange(0, 0, 150, data));
    }

    @Test
    public void emptyAuroraSetIsOutOfRange() {
        assertFalse(ServerEffectHandler.isAuroraInRange(0, 0, 150, new HashSet<AuroraData>()));
    }

    @Test
    public void auroraSetIsCapped() {
        assertTrue(ServerEffectHandler.canAddAurora(0));
        assertTrue(ServerEffectHandler.canAddAurora(31));
        assertFalse(ServerEffectHandler.canAddAurora(32));
    }

    @Test
    public void auroraCapHonoursTheConfiguredLimit() {
        int old = ModOptions.auroraMaxCount;
        try {
            ModOptions.auroraMaxCount = 2;
            assertTrue(ServerEffectHandler.canAddAurora(1));
            assertFalse(ServerEffectHandler.canAddAurora(2));
        } finally {
            ModOptions.auroraMaxCount = old;
        }
    }

    @Test
    public void rainIntensityIsSentOnChangeAndOnTheKeepalive() {
        assertTrue(ServerEffectHandler.shouldSendRainIntensity(0.5F, Float.NaN, 1));
        assertFalse(ServerEffectHandler.shouldSendRainIntensity(0.5F, 0.5F, 1));
        assertTrue(ServerEffectHandler.shouldSendRainIntensity(0.5F, 0.4F, 1));
        assertTrue(ServerEffectHandler.shouldSendRainIntensity(0.5F, 0.5F, 40));
    }
}
