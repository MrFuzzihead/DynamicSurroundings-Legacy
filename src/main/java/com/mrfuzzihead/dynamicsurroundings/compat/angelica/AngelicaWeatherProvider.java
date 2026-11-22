package com.mrfuzzihead.dynamicsurroundings.compat.angelica;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraftforge.client.IRenderHandler;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.RenderWeather;
import com.mrfuzzihead.dynamicsurroundings.client.WeatherUtils;
import com.mrfuzzihead.dynamicsurroundings.client.weather.StormRenderer;
import com.mrfuzzihead.dynamicsurroundings.client.weather.Weather;
import com.mrfuzzihead.dynamicsurroundings.client.weather.WeatherBackend;
import com.mrfuzzihead.dynamicsurroundings.compat.Compat;
import com.mrfuzzihead.dynamicsurroundings.data.BiomeRegistry;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors.EntityRendererAccessor;
import com.mrfuzzihead.dynamicsurroundings.util.Color;
import com.mrfuzzihead.dynamicsurroundings.util.random.XorShiftRandom;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Feeds DS's per-biome rain/snow/dust columns into Angelica's instanced weather pipeline.
 *
 * <p>
 * Angelica only recognises a provider that implements both Forge's {@link IRenderHandler} and its own
 * {@code com.gtnewhorizons.angelica.api.weather.WeatherColumnProvider}. This class has no compile-time Angelica
 * dependency: the provider interface is loaded reflectively and wrapped in a {@link Proxy}. Without Angelica the
 * bridge is a no-op, and {@link WeatherBackend} keeps the legacy DS renderer on the normal path.
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class AngelicaWeatherProvider {

    private static final String PROVIDER_INTERFACE = "com.gtnewhorizons.angelica.api.weather.WeatherColumnProvider";
    private static final int RAIN_PERIOD = 32;
    private static final int SNOW_PERIOD = 512;
    private static final float RAIN_FADE = 0.5F;
    private static final float SNOW_FADE = 0.3F;
    private static final float DUST_U_DRIFT = 0.2F;
    private static final float DUST_V_DRIFT = 0.001F;
    private static final XorShiftRandom RANDOM = new XorShiftRandom();
    private static final Map<World, Handler> handlers = new IdentityHashMap<>();
    private static final Map<World, IRenderHandler> previous = new IdentityHashMap<>();
    private static volatile Method beginLayerMethod;
    private static volatile Method columnMethod;
    private static volatile boolean sinkMethodsResolved;

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
            Class<?> providerInterface = Class
                .forName(PROVIDER_INTERFACE, false, AngelicaWeatherProvider.class.getClassLoader());
            Handler handler = new Handler((WorldClient) world);
            Object proxy = Proxy.newProxyInstance(
                AngelicaWeatherProvider.class.getClassLoader(),
                new Class<?>[] { IRenderHandler.class, providerInterface },
                handler);
            handler.proxy = proxy;
            previous.put(world, world.provider.getWeatherRenderer());
            handlers.put(world, handler);
            world.provider.setWeatherRenderer((IRenderHandler) proxy);
        } catch (ClassNotFoundException | LinkageError | RuntimeException e) {
            ModLog.debug("Angelica weather provider unavailable: %s", e.toString());
        }
    }

    public static boolean isDsProvider(IRenderHandler provider) {
        if (provider == null) {
            return false;
        }

        for (Handler handler : handlers.values()) {
            if (handler.proxy == provider) {
                return true;
            }
        }

        return false;
    }

    public static void uninstall(World world) {
        Handler handler = handlers.remove(world);
        if (handler == null) {
            return;
        }

        IRenderHandler old = previous.remove(world);
        if (world.provider.getWeatherRenderer() == handler.proxy) {
            world.provider.setWeatherRenderer(old);
        }
    }

    private static void resolveSinkMethods(Class<?> sinkClass) {
        if (sinkMethodsResolved) {
            return;
        }

        synchronized (AngelicaWeatherProvider.class) {
            if (sinkMethodsResolved) {
                return;
            }

            try {
                beginLayerMethod = sinkClass
                    .getMethod("beginLayer", ResourceLocation.class, int.class, float.class, float.class, float.class);
                columnMethod = sinkClass.getMethod(
                    "column",
                    int.class,
                    int.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class,
                    int.class,
                    int.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class);
                beginLayerMethod.setAccessible(true);
                columnMethod.setAccessible(true);
                sinkMethodsResolved = true;
            } catch (NoSuchMethodException | RuntimeException e) {
                ModLog.debug("Angelica weather sink unavailable: %s", e.toString());
            }
        }
    }

    private static int precipitationHeight(World world, int x, int z) {
        return world.provider.dimensionId == -1 ? 0 : world.getPrecipitationHeight(x, z);
    }

    private static float alphaRatio(float rainStrength) {
        if (Weather.isRaining() && Weather.getMaxIntensityLevel() > 0.0F) {
            return Weather.getIntensityLevel() / Weather.getMaxIntensityLevel();
        }
        return rainStrength;
    }

    private static final class Handler implements InvocationHandler {

        private WorldClient world;
        private Object proxy;
        private int revision;
        private boolean collecting;

        private Handler(WorldClient world) {
            this.world = world;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            this.proxy = proxy;
            String name = method.getName();
            switch (name) {
                case "render":
                    if (!ModOptions.disableWeatherEffects) {
                        RenderWeather.renderRainSnow(Minecraft.getMinecraft().entityRenderer, (Float) args[0]);
                    }
                    return null;
                case "rendersVanillaWeather":
                    return !DimensionRegistry.hasWeather(this.world);
                case "beginFrame":
                    this.world = (WorldClient) args[0];
                    Weather.setTextures();
                    this.revision = (int) (this.world.getTotalWorldTime() / 20L) * 31
                        + (int) (this.world.getRainStrength(1.0F) * 100.0F);
                    return ((EntityRendererAccessor) Minecraft.getMinecraft().entityRenderer)
                        .dynamicsurroundings$getRendererUpdateCount();
                case "columnRevision":
                    return this.revision;
                case "fadeRadius":
                    return Math.max(1.0F, WeatherBackend.downfallDistance());
                case "collectColumns":
                    if (!this.collecting) {
                        this.collecting = true;
                        try {
                            this.collect(args[0]);
                        } finally {
                            this.collecting = false;
                        }
                    }
                    return null;
                case "toString":
                    return "DynamicSurroundings Angelica weather provider";
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    return null;
            }
        }

        private void collect(Object sink) {
            resolveSinkMethods(sink.getClass());
            if (!sinkMethodsResolved || beginLayerMethod == null || columnMethod == null) {
                return;
            }

            Minecraft mc = Minecraft.getMinecraft();
            EntityLivingBase view = mc.renderViewEntity;
            if (view == null || this.world == null || !DimensionRegistry.hasWeather(this.world)) {
                return;
            }

            int configuredRadius = WeatherBackend.downfallDistance();
            if (configuredRadius <= 0) {
                return;
            }

            int radius = configuredRadius;
            int playerX = MathHelper.floor_double(view.posX);
            int playerY = MathHelper.floor_double(view.posY);
            int playerZ = MathHelper.floor_double(view.posZ);
            int locY = MathHelper.floor_double(view.posY);
            float rainStrength = this.world.getRainStrength(1.0F);
            if (rainStrength <= 0.0F) {
                return;
            }

            float alphaRatio = alphaRatio(rainStrength);
            WorldChunkManager chunkManager = this.world.getWorldChunkManager();
            ResourceLocation currentTexture = null;

            for (int z = playerZ - radius; z <= playerZ + radius; z++) {
                for (int x = playerX - radius; x <= playerX + radius; x++) {
                    BiomeGenBase biome = this.world.getBiomeGenForCoords(x, z);
                    boolean hasDust = WeatherUtils.biomeHasDust(biome);
                    if (!hasDust && !BiomeRegistry.hasPrecipitation(biome)) {
                        continue;
                    }

                    int height = precipitationHeight(this.world, x, z);
                    int bottom = Math.max(playerY - radius, height);
                    int top = Math.max(playerY + radius, height);
                    if (bottom == top) {
                        continue;
                    }

                    int lightY = Math.max(height, locY);
                    int light = this.world.getLightBrightnessForSkyBlocks(x, lightY, z, 0);
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
                        try {
                            beginLayerMethod.invoke(sink, texture, period, uDriftScale, vDriftScale, fadeScale);
                            currentTexture = texture;
                        } catch (IllegalAccessException | InvocationTargetException | RuntimeException e) {
                            ModLog.debug("Angelica weather layer failed: %s", e.toString());
                            return;
                        }
                    }

                    // Angelica's weather shader applies the fade and radius itself; the column colour carries only
                    // the weather strength. Passing the already-faded alpha here would square the fade.
                    float alpha = alphaRatio;
                    float uBase = rain ? 0.0F : RANDOM.nextFloat();
                    float uDrift = rain ? 0.0F : RANDOM.nextFloat();
                    float vBase = rain ? 0.0F : RANDOM.nextFloat();
                    float vDrift = rain ? 0.0F : RANDOM.nextFloat();
                    int scrollOffset = rain ? (x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761) : 0;
                    float scrollScale = rain ? 3.0F + RANDOM.nextFloat() : 1.0F;
                    Color color = new Color(1.0F, 1.0F, 1.0F);
                    if (this.world.provider.dimensionId == -1) {
                        Color dustColor = BiomeRegistry.getDustColor(biome);
                        if (dustColor != null) {
                            color.mix(dustColor);
                        }
                    }
                    int brightness = rain ? light : (light * 3 + 15728880) / 4;

                    try {
                        columnMethod.invoke(
                            sink,
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
                            alpha);
                    } catch (IllegalAccessException | InvocationTargetException | RuntimeException e) {
                        ModLog.debug("Angelica weather column failed: %s", e.toString());
                        return;
                    }
                }
            }
        }
    }
}
