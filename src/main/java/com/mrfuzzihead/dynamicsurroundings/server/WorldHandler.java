package com.mrfuzzihead.dynamicsurroundings.server;

import java.util.Random;

import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionEffectData;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionEffectDataFile;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;
import com.mrfuzzihead.dynamicsurroundings.util.random.XorShiftRandom;

public class WorldHandler {

    private static final float STRENGTH_ADJUST = 0.01F;
    private static final Random random = new XorShiftRandom();

    static int nextThunderInterval(boolean isThundering, Random rng) {
        return rng.nextInt(isThundering ? 12000 : 168000) + (isThundering ? 3600 : 12000);
    }

    static int nextRainInterval(boolean isRaining, Random rng) {
        return rng.nextInt(isRaining ? 12000 : 168000) + 12000;
    }

    private static int nextThunderInterval(boolean isThundering) {
        return nextThunderInterval(isThundering, random);
    }

    private static int nextRainInterval(boolean isRaining) {
        return nextRainInterval(isRaining, random);
    }

    public static void updateWeatherBody(World world) {
        if (!world.isRemote && (!world.provider.hasNoSky || DimensionRegistry.hasWeather(world))) {
            int dimensionId = world.provider.dimensionId;
            DimensionEffectData data = DimensionEffectDataFile.get(world);
            WorldInfo info = world.getWorldInfo();
            int i = info.getThunderTime();
            if (i <= 0) {
                info.setThunderTime(nextThunderInterval(info.isThundering()));
            } else {
                info.setThunderTime(--i);
                if (i <= 0) {
                    info.setThundering(!info.isThundering());
                }
            }

            world.prevThunderingStrength = world.thunderingStrength;
            world.thunderingStrength = world.thunderingStrength
                + (info.isThundering() ? STRENGTH_ADJUST : -STRENGTH_ADJUST);
            world.thunderingStrength = MathHelper.clamp_float(world.thunderingStrength, 0.0F, 1.0F);
            int j = info.getRainTime();
            if (j <= 0) {
                info.setRainTime(nextRainInterval(info.isRaining()));
            } else {
                info.setRainTime(--j);
                if (j <= 0) {
                    info.setRaining(!info.isRaining());
                }
            }

            if (info.isRaining() && data.getRainIntensity() == 0.0F) {
                data.randomizeRain();
                ModLog.debug(String.format("dim %d rain strength set to %f", dimensionId, data.getRainIntensity()));
            }

            world.prevRainingStrength = world.rainingStrength;
            if (info.isRaining()) {
                if (world.rainingStrength > data.getRainIntensity()) {
                    world.rainingStrength -= STRENGTH_ADJUST;
                    if (world.rainingStrength < 0.0F) {
                        world.rainingStrength = 0.0F;
                    }
                } else if (world.rainingStrength < data.getRainIntensity()) {
                    world.rainingStrength += STRENGTH_ADJUST;
                    if (world.rainingStrength > data.getRainIntensity()) {
                        world.rainingStrength = data.getRainIntensity();
                    }
                }
            } else if (world.rainingStrength > 0.0F) {
                world.rainingStrength -= STRENGTH_ADJUST;
                if (world.rainingStrength < 0.0F) {
                    world.rainingStrength = 0.0F;
                }
            } else if (data.getRainIntensity() > 0.0F) {
                data.setRainIntensity(0.0F);
                ModLog.debug(String.format("dim %d rain has stopped", dimensionId));
            }
        }
    }
}
