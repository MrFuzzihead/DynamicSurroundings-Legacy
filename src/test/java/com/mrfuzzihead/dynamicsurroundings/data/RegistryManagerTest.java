package com.mrfuzzihead.dynamicsurroundings.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class RegistryManagerTest {

    @Test
    public void aTargetedReloadOnlyRunsTheNamedRegistry() {
        AtomicInteger alpha = new AtomicInteger();
        AtomicInteger beta = new AtomicInteger();
        RegistryManager.register("test.alpha", alpha::incrementAndGet);
        RegistryManager.register("test.beta", beta::incrementAndGet);

        RegistryManager.reload("test.alpha");

        assertEquals(1, alpha.get());
        assertEquals(0, beta.get());
    }

    @Test
    public void aFailingRegistryDoesNotBlockTheOthers() {
        AtomicInteger healthy = new AtomicInteger();
        RegistryManager.register("test.failing", () -> { throw new IllegalStateException("expected test failure"); });
        RegistryManager.register("test.healthy", healthy::incrementAndGet);

        RegistryManager.reload("test.failing", "test.healthy");

        assertEquals(1, healthy.get());
    }

    @Test
    public void builtInRegistriesAreRegistered() {
        assertTrue(
            RegistryManager.ids()
                .contains("biomes"));
        assertTrue(
            RegistryManager.ids()
                .contains("dimensions"));
        assertTrue(
            RegistryManager.ids()
                .contains("sounds"));
    }
}
