package com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.resources.IResourcePack;

import org.junit.Test;

public class ResourceLoaderTest {

    @Test
    public void everyReadablePackIsVisited() {
        IResourcePack first = pack("first");
        IResourcePack second = pack("second");
        AtomicInteger visited = new AtomicInteger();

        ResourceLoader.forEachPack(
            Arrays.asList(first, second),
            "test",
            pack -> new ByteArrayInputStream(new byte[] { 1 }),
            stream -> visited.incrementAndGet());

        assertEquals(2, visited.get());
    }

    @Test
    public void nullStreamsAndFailuresDoNotStopLaterPacks() {
        IResourcePack missing = pack("missing");
        IResourcePack broken = pack("broken");
        IResourcePack good = pack("good");
        AtomicInteger visited = new AtomicInteger();

        ResourceLoader.forEachPack(Arrays.asList(missing, broken, good), "test", pack -> {
            if (pack == missing) {
                return null;
            }
            if (pack == broken) {
                throw new IOException("expected test failure");
            }
            return new ByteArrayInputStream(new byte[] { 1 });
        }, stream -> visited.incrementAndGet());

        assertEquals(1, visited.get());
    }

    private static IResourcePack pack(String name) {
        IResourcePack pack = mock(IResourcePack.class);
        when(pack.getPackName()).thenReturn(name);
        return pack;
    }
}
