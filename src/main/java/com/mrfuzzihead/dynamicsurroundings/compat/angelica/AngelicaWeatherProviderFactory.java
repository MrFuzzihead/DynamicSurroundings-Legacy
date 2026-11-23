package com.mrfuzzihead.dynamicsurroundings.compat.angelica;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraftforge.client.IRenderHandler;

import com.gtnewhorizons.angelica.api.weather.WeatherColumnProvider;
import com.gtnewhorizons.angelica.api.weather.WeatherColumnSink;
import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.RenderWeather;
import com.mrfuzzihead.dynamicsurroundings.client.weather.Weather;
import com.mrfuzzihead.dynamicsurroundings.client.weather.WeatherBackend;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors.EntityRendererAccessor;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Direct Angelica {@link WeatherColumnProvider} implementation.
 *
 * <p>
 * This class and {@link Provider} reference Angelica types, so they must only be loaded by
 * {@link AngelicaWeatherProvider#install(World)} after the mod-id check. The outer installer has no Angelica types in
 * its bytecode.
 * </p>
 */
@SideOnly(Side.CLIENT)
final class AngelicaWeatherProviderFactory {

    private AngelicaWeatherProviderFactory() {}

    static IRenderHandler create(WorldClient world) {
        return new Provider(world);
    }

    private static float alphaRatio(float rainStrength) {
        if (Weather.isRaining() && Weather.getMaxIntensityLevel() > 0.0F) {
            return Weather.getIntensityLevel() / Weather.getMaxIntensityLevel();
        }
        return rainStrength;
    }

    private static final class Provider extends IRenderHandler implements WeatherColumnProvider {

        private WorldClient world;
        private int revision;

        private Provider(WorldClient world) {
            this.world = world;
        }

        @Override
        public void render(float partialTicks, WorldClient world, Minecraft mc) {
            if (!ModOptions.disableWeatherEffects) {
                RenderWeather.renderRainSnow(mc.entityRenderer, partialTicks);
            }
        }

        @Override
        public boolean rendersVanillaWeather() {
            return !DimensionRegistry.hasWeather(this.world);
        }

        @Override
        public int beginFrame(WorldClient world, EntityLivingBase view, float partialTicks) {
            this.world = world;
            Weather.setTextures();
            this.revision = (int) (world.getTotalWorldTime() / 20L) * 31 + (int) (world.getRainStrength(1.0F) * 100.0F);
            return ((EntityRendererAccessor) Minecraft.getMinecraft().entityRenderer)
                .dynamicsurroundings$getRendererUpdateCount();
        }

        @Override
        public int columnRevision() {
            return this.revision;
        }

        @Override
        public float fadeRadius() {
            return Math.max(1.0F, WeatherBackend.downfallDistance());
        }

        @Override
        public void collectColumns(WeatherColumnSink sink) {
            Minecraft mc = Minecraft.getMinecraft();
            EntityLivingBase view = mc.renderViewEntity;
            if (view == null || this.world == null || !DimensionRegistry.hasWeather(this.world)) {
                return;
            }

            int configuredRadius = WeatherBackend.downfallDistance();
            if (configuredRadius <= 0) {
                return;
            }

            float rainStrength = this.world.getRainStrength(1.0F);
            if (rainStrength <= 0.0F) {
                return;
            }

            WeatherColumnCollector
                .collect(this.world, view, configuredRadius, rainStrength, alphaRatio(rainStrength), sink);
        }
    }
}
