package com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.item.ItemStack;

/** Caches ore-dictionary lookups so repeated reloads do not re-probe the same ore names. */
final class OreLookupCache {

    private final Map<String, List<ItemStack>> cache = new HashMap<>();

    List<ItemStack> get(String oreName, Function<String, List<ItemStack>> loader) {
        return this.cache.computeIfAbsent(oreName, loader);
    }

    void clear() {
        this.cache.clear();
    }
}
