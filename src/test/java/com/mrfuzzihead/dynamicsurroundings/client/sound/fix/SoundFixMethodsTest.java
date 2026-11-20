package com.mrfuzzihead.dynamicsurroundings.client.sound.fix;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.junit.Test;

import paulscode.sound.Source;

/**
 * The crash fixes only make sense when the "removal requested" flag and the deferral actually agree. These tests use
 * a mock {@link Source} that also implements the duck interface the mixin adds, which is exactly how the target class
 * looks after the mixin has been applied.
 */
public class SoundFixMethodsTest {

    private static Source source() {
        return mock(Source.class, withSettings().extraInterfaces(ISourceRemoved.class));
    }

    @Test
    public void removeSourceDropsAFlaggedSourceAfterCleaningItUp() {
        Source source = source();
        ISourceRemoved accessor = (ISourceRemoved) source;
        when(accessor.dynamicsurroundings$isRemoved()).thenReturn(true);

        assertNull(SoundFixMethods.removeSource(source));
        verify(source).cleanup();
    }

    @Test
    public void removeSourceKeepsALiveSource() {
        Source source = source();
        ISourceRemoved accessor = (ISourceRemoved) source;
        when(accessor.dynamicsurroundings$isRemoved()).thenReturn(false);

        assertSame(source, SoundFixMethods.removeSource(source));
        verify(source, never()).cleanup();
    }

    @Test
    public void removeSourceToleratesNull() {
        assertNull(SoundFixMethods.removeSource(null));
    }

    @Test
    public void cleanupSourceDefersAStreamingSource() {
        Source source = source();
        ISourceRemoved accessor = (ISourceRemoved) source;
        source.toStream = true;

        SoundFixMethods.cleanupSource(source);

        verify(accessor).dynamicsurroundings$setRemoved(true);
        verify(source, never()).cleanup();
    }

    @Test
    public void cleanupSourceCleansANormalSourceImmediately() {
        Source source = source();
        ISourceRemoved accessor = (ISourceRemoved) source;
        source.toStream = false;

        SoundFixMethods.cleanupSource(source);

        verify(source).cleanup();
        verify(accessor, never()).dynamicsurroundings$setRemoved(true);
    }

    @Test
    public void cleanupSourceToleratesNull() {
        SoundFixMethods.cleanupSource(null);
    }
}
