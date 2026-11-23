package com.mrfuzzihead.dynamicsurroundings.compat.angelica;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;

import com.gtnewhorizons.angelica.api.weather.WeatherColumnSink;
import com.mrfuzzihead.dynamicsurroundings.client.WeatherUtils;
import com.mrfuzzihead.dynamicsurroundings.client.weather.StormRenderer;
import com.mrfuzzihead.dynamicsurroundings.data.BiomeRegistry;
import com.mrfuzzihead.dynamicsurroundings.util.RgbColor;
import com.mrfuzzihead.dynamicsurroundings.util.random.XorShiftRandom;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Builds DS's per-biome rain/snow/dust columns for Angelica's {@link WeatherColumnSink}.
 *
 * <p>
 * This is separate from the provider so the column rules can be unit-tested with a fake sink. It deliberately takes
 * the already-resolved weather strength and fade ratio; the provider owns the Minecraft/Weather lookups.
 * </p>
 */
@SideOnly(Side.CLIENT)
final class WeatherColumnCollector {

    private static final int RAIN_PERIOD = 32;
    private static final int SNOW_PERIOD = 512;
    private static final float RAIN_FADE = 0.5F;
    private static final float SNOW_FADE = 0.3F;
    private static final float DUST_U_DRIFT = 0.2F;
    private static final float DUST_V_DRIFT = 0.001F;
    private static final XorShiftRandom RANDOM = new XorShiftRandom();

    private WeatherColumnCollector() {}

    static void collect(WorldClient world, EntityLivingBase view, int radius, float rainStrength, float alphaRatio,
        WeatherColumnSink sink) {
        if (view == null || world == null || radius <= 0 || rainStrength <= 0.0F) {
            return;
        }

        int playerX = MathHelper.floor_double(view.posX);
        int playerY = MathHelper.floor_double(view.posY);
        int playerZ = MathHelper.floor_double(view.posZ);
        int locY = MathHelper.floor_double(view.posY);
        WorldChunkManager chunkManager = world.getWorldChunkManager();
        ResourceLocation currentTexture = null;

        for (int z = playerZ - radius; z <= playerZ + radius; z++) {
            for (int x = playerX - radius; x <= playerX + radius; x++) {
                BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
                boolean hasDust = WeatherUtils.biomeHasDust(biome);
                if (!hasDust && !BiomeRegistry.hasPrecipitation(biome)) {
                    continue;
                }

                int height = precipitationHeight(world, x, z);
                int bottom = Math.max(playerY - radius, height);
                int top = Math.max(playerY + radius, height);
                if (bottom == top) {
                    continue;
                }

                int lightY = Math.max(height, locY);
                int light = world.getLightBrightnessForSkyBlocks(x, lightY, z, 0);
                RANDOM.setSeed(x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761);
                float temperature = chunkManager
                    .getTemperatureAtHeight(biome.getFloatTemperature(x, bottom, z), height);
                boolean rain = !hasDust && temperature >= 0.15F;
                ResourceLocation texture;
                int period;
                float uDriftScale;
                float vDriftScale;
                float fadeScale;
                if (rain) {
                    texture = StormRenderer.locationRainPng;
                    period = RAIN_PERIOD;
                    uDriftScale = 0.0F;
                    vDriftScale = 0.0F;
                    fadeScale = RAIN_FADE;
                } else {
                    boolean dust = hasDust && temperature >= 0.15F;
                    texture = dust ? StormRenderer.locationDustPng : StormRenderer.locationSnowPng;
                    period = SNOW_PERIOD;
                    uDriftScale = dust ? DUST_U_DRIFT : 0.01F;
                    vDriftScale = dust ? DUST_V_DRIFT : 0.001F;
                    fadeScale = SNOW_FADE;
                }

                if (!texture.equals(currentTexture)) {
                    sink.beginLayer(texture, period, uDriftScale, vDriftScale, fadeScale);
                    currentTexture = texture;
                }

                float uBase = rain ? 0.0F : RANDOM.nextFloat();
                float uDrift = rain ? 0.0F : RANDOM.nextFloat();
                float vBase = rain ? 0.0F : RANDOM.nextFloat();
                float vDrift = rain ? 0.0F : RANDOM.nextFloat();
                int scrollOffset = rain ? (x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761) : 0;
                float scrollScale = rain ? 3.0F + RANDOM.nextFloat() : 1.0F;
                RgbColor color = new RgbColor(1.0F, 1.0F, 1.0F);
                if (world.provider.dimensionId == -1) {
                    RgbColor dustColor = BiomeRegistry.getDustColor(biome);
                    if (dustColor != null) {
                        color.mix(dustColor);
                    }
                }
                int brightness = rain ? light : (light * 3 + 15728880) / 4;

                // Angelica's shader applies the fade/radius itself; the column colour carries only the strength.
                sink.column(
                    x,
                    z,
                    bottom,
                    top,
                    uBase,
                    uDrift,
                    vBase,
                    vDrift,
                    brightness,
                    scrollOffset,
                    scrollScale,
                    color.red,
                    color.green,
                    color.blue,
                    alphaRatio);
            }
        }
    }

    private static int precipitationHeight(World world, int x, int z) {
        return world.provider.dimensionId == -1 ? 0 : world.getPrecipitationHeight(x, z);
    }
}
