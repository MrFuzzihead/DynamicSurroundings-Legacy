package com.mrfuzzihead.dynamicsurroundings.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.dynamicsurroundings.client.IAtmosRenderer;
import com.mrfuzzihead.dynamicsurroundings.client.WeatherUtils;
import com.mrfuzzihead.dynamicsurroundings.data.BiomeRegistry;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors.EntityRendererAccessor;
import com.mrfuzzihead.dynamicsurroundings.util.Color;
import com.mrfuzzihead.dynamicsurroundings.util.random.XorShiftRandom;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class StormRenderer implements IAtmosRenderer {

    private static final XorShiftRandom random = new XorShiftRandom();
    public static ResourceLocation locationRainPng = new ResourceLocation("textures/environment/rain.png");
    public static ResourceLocation locationSnowPng = new ResourceLocation("textures/environment/snow.png");
    public static ResourceLocation locationDustPng = new ResourceLocation(
        "dynamicsurroundings",
        "textures/environment/dust.png");
    private static final float[] RAIN_X_COORDS = new float[1024];
    private static final float[] RAIN_Y_COORDS = new float[1024];

    private static int getPrecipitationHeight(World world, int x, int z) {
        return world.provider.dimensionId == -1 ? 0 : world.getPrecipitationHeight(x, z);
    }

    @Override
    public void render(EntityRenderer renderer, float partialTicks) {
        Weather.setTextures();
        Minecraft mc = Minecraft.getMinecraft();
        int rendererUpdateCount = ((EntityRendererAccessor) renderer).dynamicsurroundings$getRendererUpdateCount();
        WorldClient world = mc.theWorld;
        if (DimensionRegistry.hasWeather(world)) {
            int radius = WeatherBackend.downfallDistance();
            if (radius <= 0) {
                return;
            }

            float rainStrength = world.getRainStrength(partialTicks);
            if (!(rainStrength <= 0.0F)) {
                float alphaRatio;
                if (Weather.isRaining()) {
                    alphaRatio = Weather.getIntensityLevel() / Weather.getMaxIntensityLevel();
                } else {
                    alphaRatio = rainStrength;
                }

                renderer.enableLightmap(partialTicks);
                EntityLivingBase entity = mc.renderViewEntity;
                int playerX = MathHelper.floor_double(entity.posX);
                int playerY = MathHelper.floor_double(entity.posY);
                int playerZ = MathHelper.floor_double(entity.posZ);
                GL11.glDisable(2884);
                GL11.glNormal3f(0.0F, 1.0F, 0.0F);
                GL11.glEnable(3042);
                OpenGlHelper.glBlendFunc(770, 771, 1, 0);
                GL11.glAlphaFunc(516, 0.1F);
                double spawnX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
                double spawnY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
                double spawnZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
                int locY = MathHelper.floor_double(spawnY);
                byte b1 = -1;
                float f5 = rendererUpdateCount + partialTicks;
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                Tessellator tessellator = Tessellator.instance;

                for (int locZ = playerZ - radius; locZ <= playerZ + radius; locZ++) {
                    for (int locX = playerX - radius; locX <= playerX + radius; locX++) {
                        int idx = (locZ - playerZ + 16) * 32 + locX - playerX + 16;
                        float f6 = RAIN_X_COORDS[idx] * 0.5F;
                        float f7 = RAIN_Y_COORDS[idx] * 0.5F;
                        BiomeGenBase biome = world.getBiomeGenForCoords(locX, locZ);
                        boolean hasDust = WeatherUtils.biomeHasDust(biome);
                        if (hasDust || BiomeRegistry.hasPrecipitation(biome)) {
                            int k1 = getPrecipitationHeight(world, locX, locZ);
                            int l1 = playerY - radius;
                            int i2 = playerY + radius;
                            if (l1 < k1) {
                                l1 = k1;
                            }

                            if (i2 < k1) {
                                i2 = k1;
                            }

                            float f8 = 1.0F;
                            int j2 = k1;
                            if (k1 < locY) {
                                j2 = locY;
                            }

                            if (l1 != i2) {
                                random.setSeed(
                                    locX * locX * 3121 + locX * 45238971 ^ locZ * locZ * 418711 + locZ * 13761);
                                float heightTemp = world.getWorldChunkManager()
                                    .getTemperatureAtHeight(biome.getFloatTemperature(locX, l1, locZ), k1);
                                if (!hasDust && heightTemp >= 0.15F) {
                                    if (b1 != 0) {
                                        if (b1 >= 0) {
                                            tessellator.draw();
                                        }

                                        b1 = 0;
                                        mc.getTextureManager()
                                            .bindTexture(locationRainPng);
                                        tessellator.startDrawingQuads();
                                    }

                                    float f10 = ((rendererUpdateCount + locX * locX * 3121
                                        + locX * 45238971
                                        + locZ * locZ * 418711
                                        + locZ * 13761 & 31) + partialTicks) / 32.0F * (3.0F + random.nextFloat());
                                    double deltaX = locX + 0.5F - entity.posX;
                                    double deltaZ = locZ + 0.5F - entity.posZ;
                                    float dist = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ) / radius;
                                    tessellator.setBrightness(world.getLightBrightnessForSkyBlocks(locX, j2, locZ, 0));
                                    tessellator.setColorRGBA_F(
                                        1.0F,
                                        1.0F,
                                        1.0F,
                                        ((1.0F - dist * dist) * 0.5F + 0.5F) * alphaRatio);
                                    tessellator.setTranslation(-spawnX * 1.0, -spawnY * 1.0, -spawnZ * 1.0);
                                    tessellator.addVertexWithUV(
                                        locX - f6 + 0.5,
                                        l1,
                                        locZ - f7 + 0.5,
                                        0.0,
                                        l1 * 1.0F / 4.0F + f10 * 1.0F);
                                    tessellator.addVertexWithUV(
                                        locX + f6 + 0.5,
                                        l1,
                                        locZ + f7 + 0.5,
                                        1.0,
                                        l1 * 1.0F / 4.0F + f10 * 1.0F);
                                    tessellator.addVertexWithUV(
                                        locX + f6 + 0.5,
                                        i2,
                                        locZ + f7 + 0.5,
                                        1.0,
                                        i2 * 1.0F / 4.0F + f10 * 1.0F);
                                    tessellator.addVertexWithUV(
                                        locX - f6 + 0.5,
                                        i2,
                                        locZ - f7 + 0.5,
                                        0.0,
                                        i2 * 1.0F / 4.0F + f10 * 1.0F);
                                    tessellator.setTranslation(0.0, 0.0, 0.0);
                                } else {
                                    if (b1 != 1) {
                                        if (b1 >= 0) {
                                            tessellator.draw();
                                        }

                                        ResourceLocation texture = locationSnowPng;
                                        if (hasDust && heightTemp >= 0.15F) {
                                            texture = locationDustPng;
                                        }

                                        b1 = 1;
                                        mc.getTextureManager()
                                            .bindTexture(texture);
                                        tessellator.startDrawingQuads();
                                    }

                                    float f10 = ((rendererUpdateCount & 511) + partialTicks) / 512.0F;
                                    float factor = hasDust ? 0.2F : 0.01F;
                                    float f16 = random.nextFloat() + f5 * factor * (float) random.nextGaussian();
                                    float f11 = random.nextFloat() + f5 * (float) random.nextGaussian() * 0.001F;
                                    double deltaX = locX + 0.5F - entity.posX;
                                    double deltaZ = locZ + 0.5F - entity.posZ;
                                    float dist = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ) / radius;
                                    tessellator.setBrightness(
                                        (world.getLightBrightnessForSkyBlocks(locX, j2, locZ, 0) * 3 + 15728880) / 4);
                                    Color color = new Color(1.0F, 1.0F, 1.0F);
                                    if (world.provider.dimensionId == -1) {
                                        Color c = BiomeRegistry.getDustColor(biome);
                                        if (c != null) {
                                            color.mix(c);
                                        }
                                    }

                                    tessellator.setColorRGBA_F(
                                        color.red,
                                        color.green,
                                        color.blue,
                                        ((1.0F - dist * dist) * 0.3F + 0.5F) * alphaRatio);
                                    tessellator.setTranslation(-spawnX * 1.0, -spawnY * 1.0, -spawnZ * 1.0);
                                    tessellator.addVertexWithUV(
                                        locX - f6 + 0.5,
                                        l1,
                                        locZ - f7 + 0.5,
                                        0.0F + f16,
                                        l1 * 1.0F / 4.0F + f10 * 1.0F + f11);
                                    tessellator.addVertexWithUV(
                                        locX + f6 + 0.5,
                                        l1,
                                        locZ + f7 + 0.5,
                                        1.0F + f16,
                                        l1 * 1.0F / 4.0F + f10 * 1.0F + f11);
                                    tessellator.addVertexWithUV(
                                        locX + f6 + 0.5,
                                        i2,
                                        locZ + f7 + 0.5,
                                        1.0F + f16,
                                        i2 * 1.0F / 4.0F + f10 * 1.0F + f11);
                                    tessellator.addVertexWithUV(
                                        locX - f6 + 0.5,
                                        i2,
                                        locZ - f7 + 0.5,
                                        0.0F + f16,
                                        i2 * 1.0F / 4.0F + f10 * 1.0F + f11);
                                    tessellator.setTranslation(0.0, 0.0, 0.0);
                                }
                            }
                        }
                    }
                }

                if (b1 >= 0) {
                    tessellator.draw();
                }

                GL11.glEnable(2884);
                GL11.glDisable(3042);
                GL11.glAlphaFunc(516, 0.1F);
                renderer.disableLightmap(partialTicks);
            }
        }
    }

    static float rainXAt(int i, int j) {
        return RAIN_X_COORDS[i << 5 | j];
    }

    static float rainYAt(int i, int j) {
        return RAIN_Y_COORDS[i << 5 | j];
    }

    static {
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                float f2 = j - 16;
                float f3 = i - 16;
                float f4 = Math.max(1.0E-4F, MathHelper.sqrt_float(f2 * f2 + f3 * f3));
                RAIN_X_COORDS[i << 5 | j] = -f3 / f4;
                RAIN_Y_COORDS[i << 5 | j] = f2 / f4;
            }
        }
    }
}
