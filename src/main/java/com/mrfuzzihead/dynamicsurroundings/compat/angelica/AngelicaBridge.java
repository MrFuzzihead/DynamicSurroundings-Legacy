package com.mrfuzzihead.dynamicsurroundings.compat.angelica;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.compat.Compat;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Guarded entry point for Angelica configuration. The actual references live in {@link AngelicaConfigHook}, which is
 * only loaded when Angelica is present. This keeps the class loadable (and testable) without Angelica on the
 * classpath.
 */
@SideOnly(Side.CLIENT)
public final class AngelicaBridge {

    private AngelicaBridge() {}

    public static boolean isAvailable() {
        return Compat.isAngelicaLoaded();
    }

    /**
     * Mirrors the gate at the top of Angelica's {@code WeatherRenderer.render}. When true, Angelica owns the
     * rain/snow frame and Dynamic Surroundings must not cancel it.
     */
    public static boolean isInstancedWeatherEnabled() {
        if (!isAvailable()) {
            return false;
        }

        try {
            return AngelicaConfigHook.isInstancedWeatherEnabled();
        } catch (LinkageError | RuntimeException e) {
            ModLog.debug("Angelica weather bridge unavailable: %s", e.toString());
            return false;
        }
    }
}
