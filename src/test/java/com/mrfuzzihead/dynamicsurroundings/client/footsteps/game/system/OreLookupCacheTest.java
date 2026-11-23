package com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.item.ItemStack;

import org.junit.Test;

public class OreLookupCacheTest {

    @Test
    public void repeatedLookupsReuseTheCachedList() {
        OreLookupCache cache = new OreLookupCache();
        AtomicInteger loads = new AtomicInteger();
        List<ItemStack> value = Collections.emptyList();

        List<ItemStack> first = cache.get("oreIron", name -> {
            loads.incrementAndGet();
            return value;
        });
        List<ItemStack> second = cache.get("oreIron", name -> {
            loads.incrementAndGet();
            return value;
        });

        assertEquals(1, loads.get());
        assertSame(value, first);
        assertSame(first, second);
    }

    @Test
    public void clearForcesAFreshLookup() {
        OreLookupCache cache = new OreLookupCache();
        AtomicInteger loads = new AtomicInteger();
        cache.get("oreIron", name -> {
            loads.incrementAndGet();
            return Collections.emptyList();
        });
        cache.clear();
        cache.get("oreIron", name -> {
            loads.incrementAndGet();
            return Collections.emptyList();
        });

        assertEquals(2, loads.get());
    }
}
