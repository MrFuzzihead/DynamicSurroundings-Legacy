package com.mrfuzzihead.dynamicsurroundings.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.compat.angelica.AngelicaBridge;
import com.mrfuzzihead.dynamicsurroundings.compat.angelica.AngelicaWeatherProvider;
import com.mrfuzzihead.dynamicsurroundings.compat.notfine.NotFineBridge;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Single source of truth for who renders the rain/snow frame.
 *
 * <p>
 * The old code had two independent owners: Dynamic Surroundings' {@code EntityRendererMixin} and any external weather
 * renderer (Angelica's instanced weather is one). Both cancelled {@code renderRainSnow} at {@code HEAD}, so the result
 * depended on mixin application order. The backend decides once, per frame, and the DS mixin only cancels when DS is
 * the owner. Every other owner is allowed to run normally.
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class WeatherBackend {

    public enum Owner {
        /** A third-party {@code IRenderHandler} owns the weather; vanilla calls it. */
        EXTERNAL_PROVIDER,
        /** Angelica's instanced weather renderer owns the frame. */
        ANGELICA_INSTANCED,
        /** Dynamic Surroundings' legacy renderer owns the frame. */
        DYNAMIC_SURROUNDINGS,
        /** Nothing replaced the vanilla path. */
        VANILLA
    }

    private WeatherBackend() {}

    /**
     * Pure ownership decision, kept free of Minecraft calls so the matrix can be unit-tested.
     *
     * @param dsEnabled         false when {@link ModOptions#disableWeatherEffects} is on
     * @param hasWeather        the dimension's DS weather flag ({@link DimensionRegistry#hasWeather})
     * @param angelicaInstanced Angelica's instanced weather gate
     * @param externalProvider  whether the world provider has a non-DS weather renderer
     * @param downfallDistance  effective NotFine/vanilla weather radius; 0 means no downfall
     */
    public static Owner resolve(boolean dsEnabled, boolean hasWeather, boolean angelicaInstanced,
        boolean externalProvider, int downfallDistance) {
        if (angelicaInstanced) {
            return Owner.ANGELICA_INSTANCED;
        }
        if (externalProvider) {
            return Owner.EXTERNAL_PROVIDER;
        }
        if (dsEnabled && hasWeather && downfallDistance > 0) {
            return Owner.DYNAMIC_SURROUNDINGS;
        }
        return Owner.VANILLA;
    }

    public static Owner owner(WorldClient world) {
        if (world == null || world.provider == null) {
            return Owner.VANILLA;
        }

        boolean externalProvider = world.provider.getWeatherRenderer() != null
            && !AngelicaWeatherProvider.isDsProvider(world.provider.getWeatherRenderer());
        return resolve(
            !ModOptions.disableWeatherEffects,
            DimensionRegistry.hasWeather(world),
            AngelicaBridge.isInstancedWeatherEnabled(),
            externalProvider,
            downfallDistance());
    }

    public static boolean isDsOwner(WorldClient world) {
        return owner(world) == Owner.DYNAMIC_SURROUNDINGS;
    }

    /**
     * Effective weather radius. NotFine owns this setting when installed; otherwise mirror the vanilla fancy/fast
     * choice. A value of 0 disables downfall rendering, matching NotFine's "OFF" option.
     */
    public static int downfallDistance() {
        Minecraft mc = Minecraft.getMinecraft();
        int fallback = mc != null && mc.gameSettings != null && mc.gameSettings.fancyGraphics ? 10 : 5;
        return NotFineBridge.downfallDistance(fallback);
    }
}
