package com.mrfuzzihead.dynamicsurroundings.client.sound.fix;

import paulscode.sound.Source;

/**
 * The paulscode SoundSystem crash fixes. Streaming sources cannot be cleaned up from the thread that removes them:
 * the streaming thread still holds the source and will walk its (now disposed) buffers. Instead the removal is
 * recorded on the source (see {@link ISourceRemoved}, installed by the {@code Source} mixin) and the streaming
 * thread performs the cleanup itself (see the {@code StreamThread} mixin).
 *
 * <p>
 * The old implementation reached a field injected by an ASM transformer through {@code ReflectionHelper}; the mixin
 * field is exposed through {@link ISourceRemoved} instead, so no reflection is involved.
 * </p>
 */
public final class SoundFixMethods {

    private SoundFixMethods() {}

    /**
     * Called by the streaming thread for every source it dequeues. A source whose removal was requested is cleaned
     * up here and dropped from the list by returning {@code null}.
     *
     * @param source the dequeued source, may be {@code null}
     * @return the source to keep processing, or {@code null} to drop it
     */
    public static Source removeSource(Source source) {
        if (source == null) {
            return null;
        }
        if (((ISourceRemoved) source).dynamicsurroundings$isRemoved()) {
            source.cleanup();
            return null;
        }
        return source;
    }

    /**
     * Called from {@code Library.removeSource}. A streaming source is only flagged; a normal source is cleaned up
     * immediately, exactly like the vanilla call it replaces.
     *
     * @param source the source being removed
     */
    public static void cleanupSource(Source source) {
        if (source == null) {
            return;
        }
        if (source.toStream) {
            ((ISourceRemoved) source).dynamicsurroundings$setRemoved(true);
        } else {
            source.cleanup();
        }
    }
}
