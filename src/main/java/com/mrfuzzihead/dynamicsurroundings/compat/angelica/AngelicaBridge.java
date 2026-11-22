package com.mrfuzzihead.dynamicsurroundings.compat.angelica;

import java.lang.reflect.Field;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.compat.Compat;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Guarded, reflection-only bridge to Angelica's public configuration. No Angelica type appears in this class's
 * signature, so loading it without Angelica is safe.
 *
 * <p>
 * Angelica is detected by mod id; the configuration class is resolved lazily and its handles are cached. If anything
 * is missing (a different Angelica version, a changed config field), the bridge reports the vanilla/default behaviour
 * instead of failing the render thread.
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class AngelicaBridge {

    private static final String CONFIG_CLASS = "com.gtnewhorizons.angelica.config.AngelicaConfig";
    private static final String INSTANCED_WEATHER_FIELD = "enableInstancedWeather";

    private static volatile boolean resolved;
    private static volatile boolean available;
    private static volatile Field instancedWeatherField;

    private AngelicaBridge() {}

    public static boolean isAvailable() {
        resolve();
        return available;
    }

    /**
     * Mirrors the gate at the top of Angelica's {@code WeatherRenderer.render}. When true, Angelica owns the
     * rain/snow frame and Dynamic Surroundings must not cancel it.
     */
    public static boolean isInstancedWeatherEnabled() {
        resolve();
        if (!available || instancedWeatherField == null) {
            return false;
        }

        try {
            return instancedWeatherField.getBoolean(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            ModLog.debug("Angelica weather bridge unavailable: %s", e.toString());
            return false;
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }

        synchronized (AngelicaBridge.class) {
            if (resolved) {
                return;
            }

            available = Compat.isAngelicaLoaded();
            if (available) {
                try {
                    Class<?> config = Class.forName(CONFIG_CLASS, true, AngelicaBridge.class.getClassLoader());
                    Field field = config.getDeclaredField(INSTANCED_WEATHER_FIELD);
                    field.setAccessible(true);
                    instancedWeatherField = field;
                } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                    available = false;
                    ModLog.debug("Angelica weather bridge unavailable: %s", e.toString());
                }
            }

            resolved = true;
        }
    }
}
