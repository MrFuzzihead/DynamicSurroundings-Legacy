package com.mrfuzzihead.dynamicsurroundings.client.weather;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.NoiseGeneratorSimplex;

import org.apache.commons.lang3.StringUtils;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.EnvironStateHandler;
import com.mrfuzzihead.dynamicsurroundings.client.WeatherUtils;
import com.mrfuzzihead.dynamicsurroundings.client.fx.particle.ParticleFactory;
import com.mrfuzzihead.dynamicsurroundings.compat.IParticleFactory;
import com.mrfuzzihead.dynamicsurroundings.data.BiomeRegistry;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;
import com.mrfuzzihead.dynamicsurroundings.util.DiurnalUtils;
import com.mrfuzzihead.dynamicsurroundings.util.random.XorShiftRandom;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gnu.trove.map.hash.TIntObjectHashMap;

@SideOnly(Side.CLIENT)
public class StormSplashRenderer {

    private static final TIntObjectHashMap<StormSplashRenderer> splashRenderers = new TIntObjectHashMap();
    private static final StormSplashRenderer DEFAULT = new StormSplashRenderer();
    protected static final int PARTICLE_SOUND_CHANCE = 20;
    protected static final int RANGE = 10;
    protected static final XorShiftRandom RANDOM = new XorShiftRandom();
    protected static final NoiseGeneratorSimplex GENERATOR = new NoiseGeneratorSimplex(RANDOM);

    public static void renderStormSplashes(int dimensionId, EntityRenderer renderer) {
        StormSplashRenderer splash = (StormSplashRenderer) splashRenderers.get(dimensionId);
        if (splash == null) {
            splash = DEFAULT;
        }

        splash.addRainParticles(renderer);
    }

    protected StormSplashRenderer() {}

    protected static float calculateRainSoundVolume(World world) {
        return MathHelper.clamp_float(
            (float) (Weather.getCurrentVolume()
                + GENERATOR.func_151605_a(DiurnalUtils.getClockTime(world) / 100L, 1.0) / 5.0),
            0.0F,
            1.0F);
    }

    protected EntityFX getBlockParticleFX(Block block, boolean dust, World world, double x, double y, double z) {
        IParticleFactory factory = null;
        if (dust) {
            factory = null;
        } else if (block == Blocks.soul_sand) {
            factory = null;
        } else if (block == Blocks.netherrack && RANDOM.nextInt(20) == 0) {
            factory = ParticleFactory.lavaSpark;
        } else if (block.getMaterial() == Material.lava) {
            factory = ParticleFactory.smoke;
        } else if (block.getMaterial() != Material.air) {
            factory = ParticleFactory.rain;
        }

        return factory != null ? factory.getEntityFX(0, world, x, y, z, 0.0, 0.0, 0.0) : null;
    }

    protected String getBlockSoundFX(Block block, boolean hasDust, World world) {
        if (hasDust) {
            return Weather.getIntensity()
                .getDustSound();
        } else {
            return block == Blocks.netherrack ? "minecraft:liquid.lavapop"
                : Weather.getIntensity()
                    .getStormSound();
        }
    }

    protected int getPrecipitationHeight(World world, int range, int x, int z) {
        return world.getPrecipitationHeight(x, z);
    }

    protected void playSplashSound(EntityRenderer renderer, WorldClient world, EntityLivingBase player, double x,
        double y, double z) {
        int theX = MathHelper.floor_double(x);
        int theY = MathHelper.floor_double(y);
        int theZ = MathHelper.floor_double(z);
        boolean hasDust = WeatherUtils.biomeHasDust(world.getBiomeGenForCoords(theX, theZ));
        Block block = world.getBlock(theX, theY - 1, theZ);
        String sound = this.getBlockSoundFX(block, hasDust, world);
        if (!StringUtils.isEmpty(sound)) {
            float volume = calculateRainSoundVolume(world);
            float pitch = 1.0F;
            int playerX = MathHelper.floor_double(player.posX);
            int playerY = MathHelper.floor_double(player.posY);
            int playerZ = MathHelper.floor_double(player.posZ);
            if (y > player.posY + 1.0 && world.getPrecipitationHeight(playerX, playerZ) > playerY) {
                pitch = 0.5F;
            }

            renderer.mc.theWorld.playSound(x, y, z, sound, volume, pitch, false);
        }
    }

    public void addRainParticles(EntityRenderer theThis) {
        if (theThis.mc.gameSettings.particleSetting != 2) {
            if (DimensionRegistry.hasWeather(EnvironStateHandler.EnvironState.getWorld())) {
                float rainStrengthFactor = theThis.mc.theWorld.getRainStrength(1.0F);
                if (!theThis.mc.gameSettings.fancyGraphics) {
                    rainStrengthFactor /= 2.0F;
                }

                if (!(rainStrengthFactor <= 0.0F)) {
                    RANDOM.setSeed(theThis.rendererUpdateCount * 312987231L);
                    EntityLivingBase entity = theThis.mc.renderViewEntity;
                    WorldClient worldclient = theThis.mc.theWorld;
                    int playerX = MathHelper.floor_double(entity.posX);
                    int playerY = MathHelper.floor_double(entity.posY);
                    int playerZ = MathHelper.floor_double(entity.posZ);
                    double spawnX = 0.0;
                    double spawnY = 0.0;
                    double spawnZ = 0.0;
                    int particlesSpawned = 0;
                    int particleCount = (int) (ModOptions.particleCountBase * rainStrengthFactor * rainStrengthFactor);
                    if (theThis.mc.gameSettings.particleSetting == 1) {
                        particleCount >>= 1;
                    }

                    for (int j1 = 0; j1 < particleCount; j1++) {
                        int locX = playerX + RANDOM.nextInt(10) - RANDOM.nextInt(10);
                        int locZ = playerZ + RANDOM.nextInt(10) - RANDOM.nextInt(10);
                        int locY = this.getPrecipitationHeight(worldclient, 5, locX, locZ);
                        BiomeGenBase biome = worldclient.getBiomeGenForCoords(locX, locZ);
                        boolean hasDust = WeatherUtils.biomeHasDust(biome);
                        if (locY <= playerY + 10 && locY >= playerY - 10
                            && (hasDust || BiomeRegistry.hasPrecipitation(biome)
                                && biome.getFloatTemperature(locX, locY, locZ) >= 0.15F)) {
                            Block block = worldclient.getBlock(locX, locY - 1, locZ);
                            double posX = locX + RANDOM.nextFloat();
                            double posY = locY + 0.1F - block.getBlockBoundsMinY();
                            double posZ = locZ + RANDOM.nextFloat();
                            EntityFX particle = this.getBlockParticleFX(block, hasDust, worldclient, posX, posY, posZ);
                            if (particle != null) {
                                theThis.mc.effectRenderer.addEffect(particle);
                            }

                            if (RANDOM.nextInt(++particlesSpawned) == 0) {
                                spawnX = posX;
                                spawnY = posY;
                                spawnZ = posZ;
                            }
                        }
                    }

                    if (particlesSpawned > 0 && RANDOM.nextInt(20) < theThis.rainSoundCounter++) {
                        theThis.rainSoundCounter = 0;
                        this.playSplashSound(theThis, worldclient, entity, spawnX, spawnY, spawnZ);
                    }
                }
            }
        }
    }

    static {
        splashRenderers.put(0, DEFAULT);
        splashRenderers.put(-1, new NetherSplashRenderer());
        splashRenderers.put(1, new NullSplashRenderer());
    }
}
