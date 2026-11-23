package com.mrfuzzihead.dynamicsurroundings.client.fog;

import javax.annotation.Nonnull;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;

import com.mrfuzzihead.dynamicsurroundings.client.EnvironStateHandler;
import com.mrfuzzihead.dynamicsurroundings.client.EnvironmentSnapshot;
import com.mrfuzzihead.dynamicsurroundings.client.weather.Weather;
import com.mrfuzzihead.dynamicsurroundings.data.BiomeRegistry;
import com.mrfuzzihead.dynamicsurroundings.util.MathStuff;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BiomeFogRangeCalculator extends VanillaFogRangeCalculator {

    protected static final int DISTANCE = 20;
    protected static final int COARSE_SHIFT = 2;
    protected static final int MISSING_CHUNK_RETRY_TICKS = 20;
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
        EnvironmentSnapshot snapshot = EnvironStateHandler.EnvironState.getSnapshot();
        EntityLivingBase player = snapshot.getPlayer();
        if (player == null) {
            player = EnvironStateHandler.EnvironState.getPlayer();
        }
        World world = snapshot.getWorld();
        if (world == null) {
            world = EnvironStateHandler.EnvironState.getWorld();
        }
        int playerX = MathStuff.floor(player.posX);
        int playerY = MathStuff.floor(player.posY);
        int playerZ = MathStuff.floor(player.posZ);
        float rainStr = Weather.getIntensityLevel();
        int scanX = playerX >> COARSE_SHIFT;
        int scanZ = playerZ >> COARSE_SHIFT;
        int tick = snapshot.getTickCounter();
        int signature = FogBiomeSignature.of(world, playerX, playerZ);
        BiomeFogRangeCalculator.Context ctx = this.context[this.getIdx(event)];
        if (ctx.returnCached(scanX, scanZ, signature, rainStr, event, tick)) {
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
                        distancePart = 1.0F - DUST_FOG_IMPACT * rainStr;
                    } else if (BiomeRegistry.hasFog(biome)) {
                        distancePart = BiomeRegistry.getFogDensity(biome);
                    }

                    fpDistanceBiomeFog += distancePart;
                    weightBiomeFog++;
                }
            }

            float weightMixed = (DISTANCE * 2 + 1) * (DISTANCE * 2 + 1);
            float weightDefault = weightMixed - weightBiomeFog;
            float fpDistanceBiomeFogAvg = weightBiomeFog == 0.0F ? 0.0F : fpDistanceBiomeFog / weightBiomeFog;
            float farPlaneDistance = (fpDistanceBiomeFog * 240.0F + event.farPlaneDistance * weightDefault)
                / weightMixed;
            float farPlaneDistanceScaleBiome = 0.1F * (1.0F - fpDistanceBiomeFogAvg) + 0.75F * fpDistanceBiomeFogAvg;
            float farPlaneDistanceScale = (farPlaneDistanceScaleBiome * weightBiomeFog + 0.75F * weightDefault)
                / weightMixed;
            ctx.posX = scanX;
            ctx.posZ = scanZ;
            ctx.signature = signature;
            ctx.lastScanTick = tick;
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
        public int lastScanTick = Integer.MIN_VALUE;
        public int signature;
        public boolean doScan = true;
        public final FogResult cached = new FogResult();

        private Context() {}

        public boolean returnCached(int pX, int pZ, int signature, float r, @Nonnull RenderFogEvent event, int tick) {
            return signature == this.signature && r == this.rain
                && this.lastFarPlane == event.farPlaneDistance
                && this.cached.isValid(event)
                && (!this.doScan || this.lastScanTick == Integer.MIN_VALUE
                    || tick - this.lastScanTick < MISSING_CHUNK_RETRY_TICKS);
        }
    }
}
