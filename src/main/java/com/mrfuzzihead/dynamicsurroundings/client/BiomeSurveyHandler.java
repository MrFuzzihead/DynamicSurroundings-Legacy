package com.mrfuzzihead.dynamicsurroundings.client;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import com.mrfuzzihead.dynamicsurroundings.data.FakeBiome;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gnu.trove.map.hash.TObjectIntHashMap;

@SideOnly(Side.CLIENT)
public final class BiomeSurveyHandler implements IClientEffectHandler {

    private static final int BIOME_SURVEY_RANGE = 6;
    private static final int SURVEY_INTERVAL_TICKS = 20;
    private static int area;
    private static final TObjectIntHashMap<BiomeGenBase> weights = new TObjectIntHashMap();
    private static BiomeGenBase lastPlayerBiome = null;
    private static int lastDimension = 0;
    private static int lastPlayerX = 0;
    private static int lastPlayerZ = 0;
    private static int lastSurveyTick = -SURVEY_INTERVAL_TICKS;

    public static int getArea() {
        return area;
    }

    public static TObjectIntHashMap<BiomeGenBase> getBiomes() {
        return weights;
    }

    private static void doSurvey(EntityPlayer player, int range) {
        area = 0;
        weights.clear();
        if (EnvironStateHandler.EnvironState.getPlayerBiome() instanceof FakeBiome) {
            area = 1;
            weights.put(EnvironStateHandler.EnvironState.getPlayerBiome(), 1);
        } else {
            int x = MathHelper.floor_double(player.posX);
            int z = MathHelper.floor_double(player.posZ);

            for (int dX = -range; dX <= range; dX++) {
                for (int dZ = -range; dZ <= range; dZ++) {
                    area++;
                    BiomeGenBase biome = player.worldObj.getBiomeGenForCoords(x + dX, z + dZ);
                    weights.adjustOrPutValue(biome, 1, 1);
                }
            }
        }
    }

    static boolean shouldSurvey(int dimension, int playerX, int playerZ, BiomeGenBase playerBiome, int lastDimension,
        int lastX, int lastZ, BiomeGenBase lastBiome, int tick, int lastTick) {
        boolean moved = playerX != lastX || playerZ != lastZ;
        boolean contextChanged = dimension != lastDimension || lastBiome != playerBiome;
        return contextChanged || moved && tick - lastTick >= SURVEY_INTERVAL_TICKS;
    }

    @Override
    public void process(World world, EntityPlayer player) {
        int playerX = MathHelper.floor_double(player.posX);
        int playerZ = MathHelper.floor_double(player.posZ);
        BiomeGenBase playerBiome = EnvironStateHandler.EnvironState.getPlayerBiome();
        if (shouldSurvey(
            EnvironStateHandler.EnvironState.getDimensionId(),
            playerX,
            playerZ,
            playerBiome,
            lastDimension,
            lastPlayerX,
            lastPlayerZ,
            lastPlayerBiome,
            EnvironStateHandler.EnvironState.getTickCounter(),
            lastSurveyTick)) {
            lastPlayerBiome = playerBiome;
            lastSurveyTick = EnvironStateHandler.EnvironState.getTickCounter();
            lastDimension = EnvironStateHandler.EnvironState.getDimensionId();
            lastPlayerX = playerX;
            lastPlayerZ = playerZ;
            doSurvey(player, BIOME_SURVEY_RANGE);
        }
    }

    @Override
    public boolean hasEvents() {
        return false;
    }
}
