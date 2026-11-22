package com.mrfuzzihead.dynamicsurroundings.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

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
}
