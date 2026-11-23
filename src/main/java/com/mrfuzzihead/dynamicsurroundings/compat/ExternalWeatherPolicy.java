package com.mrfuzzihead.dynamicsurroundings.compat;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;

/** Decides whether another weather mod should own the weather state and render path. */
public final class ExternalWeatherPolicy {

    private static volatile Boolean deferred;
    private static volatile String activeMod;

    private ExternalWeatherPolicy() {}

    public static boolean isDeferred() {
        Boolean cached = deferred;
        if (cached == null) {
            cached = isDeferred(
                Compat.isBetterRainLoaded() && ModOptions.deferToBetterRain,
                Compat.isWeather2Loaded() && ModOptions.deferToWeather2);
            deferred = cached;
        }
        return cached;
    }

    public static void invalidate() {
        deferred = null;
        activeMod = null;
    }

    static boolean isDeferred(boolean betterRain, boolean weather2) {
        return betterRain || weather2;
    }

    public static String activeMod() {
        String cached = activeMod;
        if (cached == null) {
            if (Compat.isBetterRainLoaded() && ModOptions.deferToBetterRain) {
                cached = "Better Rain";
            } else if (Compat.isWeather2Loaded() && ModOptions.deferToWeather2) {
                cached = "Weather2";
            } else {
                cached = "none";
            }
            activeMod = cached;
        }
        return cached;
    }
}
