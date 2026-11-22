package com.mrfuzzihead.dynamicsurroundings.compat.angelica;

import com.gtnewhorizons.angelica.config.AngelicaConfig;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Direct reference to Angelica's config. This class must only be loaded after Angelica's mod id has been observed;
 * {@link AngelicaBridge} is the guarded entry point.
 */
@SideOnly(Side.CLIENT)
final class AngelicaConfigHook {

    private AngelicaConfigHook() {}

    static boolean isInstancedWeatherEnabled() {
        return AngelicaConfig.enableInstancedWeather;
    }
}
