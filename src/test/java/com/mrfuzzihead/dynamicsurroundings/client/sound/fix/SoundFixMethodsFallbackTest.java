package com.mrfuzzihead.dynamicsurroundings.client.sound.fix;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import paulscode.sound.Source;

public class SoundFixMethodsFallbackTest {

    @Test
    public void unmodifiedStreamingSourceFallsBackToCleanup() {
        Source source = mock(Source.class);
        source.toStream = true;

        SoundFixMethods.cleanupSource(source);

        verify(source).cleanup();
    }

    @Test
    public void unmodifiedSourceStillPassesThroughRemoveSource() {
        Source source = mock(Source.class);
        assertSame(source, SoundFixMethods.removeSource(source));
    }

    @Test
    public void nullSourceIsTolerated() {
        assertNull(SoundFixMethods.removeSource(null));
        SoundFixMethods.cleanupSource(null);
    }
}
