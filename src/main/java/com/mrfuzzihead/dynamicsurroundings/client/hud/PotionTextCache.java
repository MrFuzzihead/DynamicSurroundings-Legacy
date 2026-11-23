package com.mrfuzzihead.dynamicsurroundings.client.hud;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

/** Caches the immutable potion name and the once-per-second duration string. */
final class PotionTextCache {

    private static final Map<Integer, NameEntry> NAMES = new HashMap<>();
    private static final Map<Integer, DurationEntry> DURATIONS = new HashMap<>();

    private PotionTextCache() {}

    static String name(Potion potion, PotionEffect effect) {
        int key = key(effect);
        NameEntry cached = NAMES.get(key);
        if (cached != null && cached.potion == potion) {
            return cached.value;
        }

        String value = I18n.format(potion.getName(), new Object[0]);
        if (effect.getAmplifier() == 1) {
            value = value + " " + I18n.format("enchantment.level.2", new Object[0]);
        } else if (effect.getAmplifier() == 2) {
            value = value + " " + I18n.format("enchantment.level.3", new Object[0]);
        } else if (effect.getAmplifier() == 3) {
            value = value + " " + I18n.format("enchantment.level.4", new Object[0]);
        }

        NAMES.put(key, new NameEntry(potion, value));
        return value;
    }

    static String duration(PotionEffect effect) {
        int key = effect.getPotionID();
        int seconds = effect.getDuration() / 20;
        DurationEntry cached = DURATIONS.get(key);
        if (cached != null && cached.seconds == seconds) {
            return cached.value;
        }

        String value = Potion.getDurationString(effect);
        DURATIONS.put(key, new DurationEntry(seconds, value));
        return value;
    }

    static void clear() {
        NAMES.clear();
        DURATIONS.clear();
    }

    private static int key(PotionEffect effect) {
        return effect.getPotionID() * 4 + Math.min(3, Math.max(0, effect.getAmplifier()));
    }

    private static final class NameEntry {

        private final Potion potion;
        private final String value;

        private NameEntry(Potion potion, String value) {
            this.potion = potion;
            this.value = value;
        }
    }

    private static final class DurationEntry {

        private final int seconds;
        private final String value;

        private DurationEntry(int seconds, String value) {
            this.seconds = seconds;
            this.value = value;
        }
    }
}
