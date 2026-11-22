package com.mrfuzzihead.dynamicsurroundings.client.fog;

import javax.annotation.Nonnull;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;

import com.mrfuzzihead.dynamicsurroundings.client.EnvironStateHandler;
import com.mrfuzzihead.dynamicsurroundings.client.weather.Weather;
import com.mrfuzzihead.dynamicsurroundings.data.BiomeRegistry;
import com.mrfuzzihead.dynamicsurroundings.util.MathStuff;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BiomeFogRangeCalculator extends VanillaFogRangeCalculator {

    protected static final int DISTANCE = 20;
    protected static final float DUST_FOG_IMPACT = 0.9F;
    protected final BiomeFogRangeCalculator.Context[] context = new BiomeFogRangeCalculator.Context[] {
        new BiomeFogRangeCalculator.Context(), new BiomeFogRangeCalculator.Context(),
        new BiomeFogRangeCalculator.Context() };

    private int getIdx(@Nonnull RenderFogEvent event) {
        return event.fogMode < 0 ? 2 : event.fogMode;
    }

    @Nonnull
    @Override
    public FogResult calculate(@Nonnull RenderFogEvent event) {
        EntityLivingBase player = EnvironStateHandler.EnvironState.getPlayer();
        World world = EnvironStateHandler.EnvironState.getWorld();
        int playerX = MathStuff.floor(player.posX);
        int playerY = MathStuff.floor(player.posY);
        int playerZ = MathStuff.floor(player.posZ);
        float rainStr = Weather.getIntensityLevel();
        BiomeFogRangeCalculator.Context ctx = this.context[this.getIdx(event)];
        if (ctx.returnCached(playerX, playerZ, rainStr, event)) {
            return ctx.cached;
        } else {
            float fpDistanceBiomeFog = 0.0F;
            float weightBiomeFog = 0.0F;
            boolean isRaining = Weather.isRaining();
            ctx.rain = rainStr;
            ctx.doScan = false;

            for (int x = -20; x <= 20; x++) {
                for (int z = -20; z <= 20; z++) {
                    int theX = playerX + x;
                    int theZ = playerZ + z;
                    BiomeGenBase biome = world.getBiomeGenForCoords(theX, theZ);
                    float distancePart = 1.0F;
                    float weightPart = 1.0F;
                    ctx.doScan = ctx.doScan | !world.blockExists(theX, playerY, theZ);
                    if (isRaining && BiomeRegistry.hasDust(biome)) {
                        distancePart = 1.0F - 0.9F * rainStr;
                    } else if (BiomeRegistry.hasFog(biome)) {
                        distancePart = BiomeRegistry.getFogDensity(biome);
                    }

                    fpDistanceBiomeFog += distancePart;
                    weightBiomeFog++;
                }
            }

            float weightMixed = 1681.0F;
            float weightDefault = 1681.0F - weightBiomeFog;
            float fpDistanceBiomeFogAvg = weightBiomeFog == 0.0F ? 0.0F : fpDistanceBiomeFog / weightBiomeFog;
            float farPlaneDistance = (fpDistanceBiomeFog * 240.0F + event.farPlaneDistance * weightDefault) / 1681.0F;
            float farPlaneDistanceScaleBiome = 0.1F * (1.0F - fpDistanceBiomeFogAvg) + 0.75F * fpDistanceBiomeFogAvg;
            float farPlaneDistanceScale = (farPlaneDistanceScaleBiome * weightBiomeFog + 0.75F * weightDefault)
                / 1681.0F;
            ctx.posX = playerX;
            ctx.posZ = playerZ;
            ctx.lastFarPlane = event.farPlaneDistance;
            farPlaneDistance = Math.min(farPlaneDistance, event.farPlaneDistance);
            float reference = event.farPlaneDistance > 0.0F ? event.farPlaneDistance : 1.0F;
            ctx.cached.set(
                event.fogMode,
                (farPlaneDistance * farPlaneDistanceScale) / (reference * FogResult.DEFAULT_PLANE_SCALE),
                farPlaneDistance / reference);
            return ctx.cached;
        }
    }

    private static class Context {

        public int posX;
        public int posZ;
        public float rain;
        public float lastFarPlane;
        public boolean doScan = true;
        public final FogResult cached = new FogResult();

        private Context() {}

        public boolean returnCached(int pX, int pZ, float r, @Nonnull RenderFogEvent event) {
            return !this.doScan && pX == this.posX
                && pZ == this.posZ
                && r == this.rain
                && this.lastFarPlane == event.farPlaneDistance
                && this.cached.isValid(event);
        }
    }
}
