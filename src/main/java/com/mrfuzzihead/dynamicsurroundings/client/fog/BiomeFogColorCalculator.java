package com.mrfuzzihead.dynamicsurroundings.client.fog;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.client.event.EntityViewRenderEvent.FogColors;
import net.minecraftforge.common.ForgeModContainer;

import com.mrfuzzihead.dynamicsurroundings.client.EnvironStateHandler;
import com.mrfuzzihead.dynamicsurroundings.client.EnvironmentSnapshot;
import com.mrfuzzihead.dynamicsurroundings.client.weather.Weather;
import com.mrfuzzihead.dynamicsurroundings.compat.BlockPos;
import com.mrfuzzihead.dynamicsurroundings.compat.notfine.NotFineBridge;
import com.mrfuzzihead.dynamicsurroundings.data.BiomeRegistry;
import com.mrfuzzihead.dynamicsurroundings.util.MathStuff;
import com.mrfuzzihead.dynamicsurroundings.util.RgbColor;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BiomeFogColorCalculator extends VanillaFogColorCalculator {

    protected static final int COARSE_SHIFT = 2;
    protected static final int MISSING_CHUNK_RETRY_TICKS = 20;
    protected int posX;
    protected int posZ;
    protected int lastScanTick = Integer.MIN_VALUE;
    protected int signature;
    protected double weightBiomeFog;
    protected RgbColor biomeFogColor;
    protected boolean doScan = true;

    @Nonnull
    @Override
    public RgbColor calculate(@Nonnull FogColors event) {
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
        int playerZ = MathStuff.floor(player.posZ);
        GameSettings settings = Minecraft.getMinecraft().gameSettings;
        int[] ranges = ForgeModContainer.blendRanges;
        int distance = 6;
        if (settings.fancyGraphics && ranges.length > 0) {
            distance = ranges[MathStuff.clamp(settings.renderDistanceChunks, 0, ranges.length - 1)];
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(0, 0, 0);
        int scanX = playerX >> COARSE_SHIFT;
        int scanZ = playerZ >> COARSE_SHIFT;
        int tick = snapshot.getTickCounter();
        this.doScan = this.doScan | (this.posX != scanX || this.posZ != scanZ);
        if (this.doScan
            && (this.lastScanTick == Integer.MIN_VALUE || tick - this.lastScanTick >= MISSING_CHUNK_RETRY_TICKS)) {
            this.doScan = false;
            this.posX = scanX;
            this.posZ = scanZ;
            this.lastScanTick = tick;
            this.biomeFogColor = new RgbColor(0, 0, 0);
            this.weightBiomeFog = 0.0;

            for (int x = -distance; x <= distance; x++) {
                for (int z = -distance; z <= distance; z++) {
                    pos.setPos(playerX + x, 0, playerZ + z);
                    this.doScan = this.doScan | !world.blockExists(pos.getX(), pos.getY(), pos.getZ());
                    BiomeGenBase biome = world.getBiomeGenForCoords(pos.getX(), pos.getZ());
                    RgbColor color;
                    if (BiomeRegistry.hasDust(biome)) {
                        color = BiomeRegistry.getDustColor(biome);
                    } else if (BiomeRegistry.hasFog(biome)) {
                        color = BiomeRegistry.getFogColor(biome);
                    } else {
                        color = null;
                    }

                    if (color != null) {
                        this.biomeFogColor.add(color);
                        this.weightBiomeFog++;
                    }
                }
            }
        }

        if (this.weightBiomeFog != 0.0 && distance != 0) {
            float partialTicks = (float) event.renderPartialTicks;
            float celestialAngle = world.getCelestialAngle(partialTicks);
            float baseScale = MathStuff
                .clamp(MathStuff.cos(celestialAngle * (float) Math.PI * 2.0F) * 2.0F + 0.5F, 0.0F, 1.0F);
            double rScale = baseScale * 0.94F + 0.06F;
            double gScale = baseScale * 0.94F + 0.06F;
            double bScale = baseScale * 0.91F + 0.09F;
            float rainStrength = Weather.getIntensityLevel();
            if (rainStrength > 0.0F) {
                rScale *= 1.0F - rainStrength * 0.5F;
                gScale *= 1.0F - rainStrength * 0.5F;
                bScale *= 1.0F - rainStrength * 0.4F;
            }

            float thunderStrength = Weather.getThunderStrength();
            if (thunderStrength > 0.0F) {
                rScale *= 1.0F - thunderStrength * 0.5F;
                gScale *= 1.0F - thunderStrength * 0.5F;
                bScale *= 1.0F - thunderStrength * 0.5F;
            }

            RgbColor fogColor = new RgbColor(this.biomeFogColor);
            fogColor.scale(
                (float) (rScale / this.weightBiomeFog),
                (float) (gScale / this.weightBiomeFog),
                (float) (bScale / this.weightBiomeFog));
            RgbColor processedColor = this.applyPlayerEffects(world, player, fogColor, partialTicks);
            double weightMixed = (distance * 2 + 1) * (distance * 2 + 1);
            double weightDefault = weightMixed - this.weightBiomeFog;
            RgbColor vanillaColor = super.calculate(event);
            processedColor.scale((float) this.weightBiomeFog);
            vanillaColor.scale((float) weightDefault);
            return processedColor.add(vanillaColor)
                .scale((float) (1.0 / weightMixed));
        } else {
            return super.calculate(event);
        }
    }

    protected RgbColor applyPlayerEffects(@Nonnull World world, @Nonnull EntityLivingBase player,
        @Nonnull RgbColor fogColor, float renderPartialTicks) {
        float darkScale = 1.0F;
        if (NotFineBridge.isVoidFogEnabled()) {
            darkScale = (float) ((player.lastTickPosY + (player.posY - player.lastTickPosY) * renderPartialTicks)
                * world.provider.getVoidFogYFactor());
        }

        if (player.isPotionActive(Potion.blindness)) {
            int duration = player.getActivePotionEffect(Potion.blindness)
                .getDuration();
            darkScale *= duration < 20 ? 1.0F - duration / 20.0F : 0.0F;
        }

        if (darkScale < 1.0F) {
            darkScale = darkScale < 0.0F ? 0.0F : darkScale * darkScale;
            fogColor.scale(darkScale);
        }

        if (player.isPotionActive(Potion.nightVision)) {
            int duration = player.getActivePotionEffect(Potion.nightVision)
                .getDuration();
            float brightness = duration > 200 ? 1.0F
                : 0.7F + MathStuff.sin((duration - renderPartialTicks) * (float) Math.PI * 0.2F) * 0.3F;
            float scale = 1.0F / fogColor.red;
            scale = Math.min(scale, 1.0F / fogColor.green);
            scale = Math.min(scale, 1.0F / fogColor.blue);
            return fogColor.scale(1.0F - brightness + scale * brightness);
        } else {
            return fogColor;
        }
    }
}
