package com.mrfuzzihead.dynamicsurroundings.compat.angelica;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;
import net.minecraftforge.client.IRenderHandler;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.weather.WeatherBackend;
import com.mrfuzzihead.dynamicsurroundings.compat.Compat;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Installs DS's Angelica weather provider.
 *
 * <p>
 * This class deliberately contains no Angelica types. The actual {@code WeatherColumnProvider} implementation lives
 * in {@link AngelicaWeatherProviderFactory}, which is resolved only after the mod-id and world checks. That keeps the
 * class loadable on servers and on clients without Angelica.
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class AngelicaWeatherProvider {

    private static final Map<World, IRenderHandler> handlers = new IdentityHashMap<>();
    private static final Map<World, IRenderHandler> previous = new IdentityHashMap<>();

    private AngelicaWeatherProvider() {}

    public static void install(World world) {
        if (!(world instanceof WorldClient) || !Compat.isAngelicaLoaded() || ModOptions.disableWeatherEffects) {
            return;
        }
        if (!DimensionRegistry.hasWeather(world) || WeatherBackend.downfallDistance() <= 0) {
            return;
        }
        if (world.provider.getWeatherRenderer() != null || handlers.containsKey(world)) {
            return;
        }

        try {
            IRenderHandler provider = AngelicaWeatherProviderFactory.create((WorldClient) world);
            if (provider == null) {
                return;
            }

            previous.put(world, world.provider.getWeatherRenderer());
            handlers.put(world, provider);
            world.provider.setWeatherRenderer(provider);
        } catch (LinkageError | RuntimeException e) {
            ModLog.debug("Angelica weather provider unavailable: %s", e.toString());
        }
    }

    public static boolean isDsProvider(IRenderHandler provider) {
        if (provider == null) {
            return false;
        }

        for (IRenderHandler installed : handlers.values()) {
            if (installed == provider) {
                return true;
            }
        }

        return false;
    }

    public static void uninstall(World world) {
        IRenderHandler provider = handlers.remove(world);
        if (provider == null) {
            return;
        }

        IRenderHandler old = previous.remove(world);
        if (world.provider.getWeatherRenderer() == provider) {
            world.provider.setWeatherRenderer(old);
        }
    }
}
