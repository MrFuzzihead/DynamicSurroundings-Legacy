package com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.implem;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.EventType;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.IAcoustic;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.ISoundPlayer;

public class ProbabilityWeightsAcousticTest {

    @Test
    public void onlyPositiveWeightsCanBeSelected() {
        IAcoustic zero = mock(IAcoustic.class);
        IAcoustic positive = mock(IAcoustic.class);
        ISoundPlayer player = player();

        new ProbabilityWeightsAcoustic(Arrays.asList(zero, positive), Arrays.asList(0, 1))
            .playSound(player, "location", EventType.WALK, null);

        verify(zero, never()).playSound(any(), anyString(), any(), any());
        verify(positive).playSound(player, "location", EventType.WALK, null);
    }

    @Test
    public void negativeWeightsAreIgnored() {
        IAcoustic negative = mock(IAcoustic.class);
        IAcoustic positive = mock(IAcoustic.class);
        ISoundPlayer player = player();

        new ProbabilityWeightsAcoustic(Arrays.asList(negative, positive), Arrays.asList(-5, 1))
            .playSound(player, "location", EventType.WALK, null);

        verify(negative, never()).playSound(any(), anyString(), any(), any());
        verify(positive).playSound(player, "location", EventType.WALK, null);
    }

    @Test
    public void emptyOrShortTablesDoNotThrow() {
        ISoundPlayer player = player();

        new ProbabilityWeightsAcoustic(Collections.emptyList(), Collections.emptyList())
            .playSound(player, "location", EventType.WALK, null);
        new ProbabilityWeightsAcoustic(Collections.singletonList(mock(IAcoustic.class)), Collections.singletonList(1))
            .playSound(player, "location", EventType.WALK, null);
    }

    private static ISoundPlayer player() {
        ISoundPlayer player = mock(ISoundPlayer.class);
        when(player.getRNG()).thenReturn(new Random(0L));
        return player;
    }
}
