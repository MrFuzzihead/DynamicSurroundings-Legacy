package com.mrfuzzihead.dynamicsurroundings.gametest;

import java.lang.reflect.Method;

import net.minecraft.command.ICommandManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.storage.WorldInfo;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.Module;
import com.mrfuzzihead.dynamicsurroundings.commands.CommandRain;
import com.mrfuzzihead.dynamicsurroundings.data.AuroraData;
import com.mrfuzzihead.dynamicsurroundings.data.BiomeRegistry;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionEffectData;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;
import com.mrfuzzihead.dynamicsurroundings.network.Network;
import com.mrfuzzihead.dynamicsurroundings.util.DiurnalUtils;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;

/**
 * The in-game assertions. Each {@code check*} method returns the number of failures it produced and logs every
 * failure with the {@code [DS-GAMETEST] FAIL} prefix so the exit code and the log agree.
 *
 * <p>
 * Everything here needs the real game: the loaded config, the dimension/biome registries built in post-init, a live
 * {@link World} with its saved data, and the coremod transformer that the {@code -PgameTest} launch runs with.
 * </p>
 */
public final class ServerGameTests {

    private static final String FAIL = "[DS-GAMETEST] FAIL ";
    private static final String PASS = "[DS-GAMETEST] ok   ";

    private static int failures;
    private static int checks;

    private ServerGameTests() {}

    /**
     * Runs every assertion.
     *
     * @return the number of failed assertions
     */
    public static int runAll() {
        failures = 0;
        checks = 0;

        checkModIsLoaded();
        checkConfigIsLoaded();
        checkDimensionRegistry();
        checkBiomeRegistry();
        checkDiurnalCycle();
        checkWeatherTransformerIsLive();
        checkDimensionDataIsPerWorldSavedData();
        checkRainCommand();
        checkNetworkChannel();
        checkAuroraNbtRoundTrip();

        return failures;
    }

    /**
     * @return how many assertions {@link #runAll()} executed
     */
    public static int checkCount() {
        return checks;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Mod lifecycle
    // ---------------------------------------------------------------------------------------------------------

    private static void checkModIsLoaded() {
        check("the mod instance was created", Module.instance() != null);
        check("the mod id is stable", "dynamicsurroundings".equals(Module.MOD_ID));
        check("a proxy was injected", Module.proxy() != null);
        check(
            "the server proxy is the common one",
            Module.proxy()
                .getClass()
                .getName()
                .equals("com.mrfuzzihead.dynamicsurroundings.proxy.Proxy"));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Config
    // ---------------------------------------------------------------------------------------------------------

    private static void checkConfigIsLoaded() {
        check("the config object exists", Module.config() != null);
        if (Module.config() == null) return;

        check(
            "the rain category exists",
            Module.config()
                .getCategory("rain") != null);
        check(
            "the aurora category exists",
            Module.config()
                .getCategory("aurora") != null);
        check(
            "the fog category exists",
            Module.config()
                .getCategory("fog") != null);
        check(
            "the dimension category exists",
            Module.config()
                .getCategory("dimensions") != null);
        check(
            "the sound level was read from the config",
            ModOptions.soundLevel >= 0.0F && ModOptions.soundLevel <= 1.0F);
        check("the particle count base is non-negative", ModOptions.particleCountBase >= 0);
        check(
            "the data directory was created",
            Module.dataDirectory() != null && Module.dataDirectory()
                .isDirectory());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Dimension registry (built in post-init from assets/dynamicsurroundings/data/dimensions.json)
    // ---------------------------------------------------------------------------------------------------------

    private static void checkDimensionRegistry() {
        WorldServer overworld = overworld();
        if (overworld == null) {
            check("the overworld exists", false);
            return;
        }

        check("overworld sky height", DimensionRegistry.getSkyHeight(overworld) == 256);
        check("overworld cloud height is half the sky", DimensionRegistry.getCloudHeight(overworld) == 128);
        check("overworld space height sits above the sky", DimensionRegistry.getSpaceHeight(overworld) == 288);
        // The registry derives the sea level from the provider, so assert the derivation rather than a constant.
        int providerSeaLevel = overworld.provider.getAverageGroundLevel();
        check(
            "overworld sea level comes from the provider (" + providerSeaLevel + ")",
            DimensionRegistry.getSeaLevel(overworld) == providerSeaLevel);
        check(
            "overworld sea level is plausible",
            DimensionRegistry.getSeaLevel(overworld) > 0 && DimensionRegistry.getSeaLevel(overworld) <= 128);
        check("overworld has haze", DimensionRegistry.hasHaze(overworld));
        check("overworld has auroras", DimensionRegistry.hasAuroras(overworld));
        check("overworld has weather", DimensionRegistry.hasWeather(overworld));

        // dimensions.json declares the nether as weathery but without haze or auroras, and the end as weatherless.
        WorldServer nether = worldServer(-1);
        if (nether != null) {
            check(
                "the nether came from the config",
                "Nether".equals(
                    DimensionRegistry.getData(nether)
                        .getName()));
            check("the nether has no haze", !DimensionRegistry.hasHaze(nether));
            check("the nether has no auroras", !DimensionRegistry.hasAuroras(nether));
            check("the nether has weather", DimensionRegistry.hasWeather(nether));
            check("the nether sea level comes from the config", DimensionRegistry.getSeaLevel(nether) == 0);
        }

        WorldServer end = worldServer(1);
        if (end != null) {
            check("the end has no weather", !DimensionRegistry.hasWeather(end));
            check("the end cloud height comes from the config", DimensionRegistry.getCloudHeight(end) == 128);
        }

        String conditions = DimensionRegistry.getConditions(overworld);
        check("conditions are tokenised with '#'", conditions.startsWith("#") && conditions.endsWith("#"));
        check("conditions include the day/night token", conditions.contains("#day#") || conditions.contains("#night#"));
        check("conditions include the dimension name", conditions.contains("#Overworld#"));
        check("conditions include the season slot", conditions.contains("#noseason#"));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Biome registry
    // ---------------------------------------------------------------------------------------------------------

    private static void checkBiomeRegistry() {
        check("vanilla oceans are named", "Ocean".equals(BiomeRegistry.resolveName(BiomeGenBase.ocean)));
        check("plains reports precipitation", BiomeRegistry.hasPrecipitation(BiomeGenBase.plains));
        check("an unknown biome resolves to the placeholder", "(Bad Biome)".equals(BiomeRegistry.resolveName(null)));
        check(
            "the pseudo biomes keep their negative ids",
            BiomeRegistry.UNDERGROUND.biomeID < 0 && BiomeRegistry.OUTERSPACE.biomeID < 0);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Day cycle, driven by the real world time
    // ---------------------------------------------------------------------------------------------------------

    private static void checkDiurnalCycle() {
        WorldServer world = overworld();
        if (world == null) return;

        long originalTime = world.getWorldTime();
        try {
            world.setWorldTime(6000L);
            check("noon is daytime", DiurnalUtils.isDaytime(world));
            check("noon hides the aurora", !DiurnalUtils.isAuroraVisible(world));

            world.setWorldTime(18000L);
            check("midnight is nighttime", DiurnalUtils.isNighttime(world));
            check("midnight shows the aurora", DiurnalUtils.isAuroraVisible(world));

            world.setWorldTime(24000L * 4 + 1000L);
            check("the clock time is the day offset", DiurnalUtils.getClockTime(world) == 1000L);
        } finally {
            world.setWorldTime(originalTime);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Coremod: the weather transformer replaced World.updateWeatherBody
    // ---------------------------------------------------------------------------------------------------------

    private static void checkWeatherTransformerIsLive() {
        WorldServer world = overworld();
        if (world == null) return;

        if (ModOptions.disableWeatherEffects) {
            // The patch is intentionally not applied in that configuration; nothing to assert.
            check("weather effects are enabled for the game test", false);
            return;
        }

        WorldInfo info = world.getWorldInfo();
        boolean wasRaining = info.isRaining();
        boolean wasThundering = info.isThundering();
        int wasRainTime = info.getRainTime();
        int wasThunderTime = info.getThunderTime();

        DimensionEffectData data = DimensionEffectData.get(world);
        float wasIntensity = data.getRainIntensity();

        try {
            info.setRaining(true);
            data.setRainIntensity(0.0F);
            invokeUpdateWeatherBody(world);

            // Vanilla's own updateWeatherBody never touches DimensionEffectData, so a non-zero intensity proves
            // that WorldHandler.updateWeatherBody is the method that ran.
            check("the weather transformer replaced World.updateWeatherBody", data.getRainIntensity() > 0.0F);
            check(
                "the randomised intensity is in range",
                data.getRainIntensity() >= 0.01F && data.getRainIntensity() <= 1.0F);
        } finally {
            info.setRaining(wasRaining);
            info.setThundering(wasThundering);
            info.setRainTime(wasRainTime);
            info.setThunderTime(wasThunderTime);
            data.setRainIntensity(wasIntensity);
        }
    }

    /**
     * Calls the (protected, possibly transformed) {@code World.updateWeatherBody} on the live world.
     */
    private static void invokeUpdateWeatherBody(World world) {
        try {
            Method method = World.class.getDeclaredMethod("updateWeatherBody");
            method.setAccessible(true);
            method.invoke(world);
        } catch (Exception e) {
            check("World.updateWeatherBody is callable: " + e, false);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Saved data
    // ---------------------------------------------------------------------------------------------------------

    private static void checkDimensionDataIsPerWorldSavedData() {
        WorldServer overworld = overworld();
        if (overworld == null) return;

        DimensionEffectData first = DimensionEffectData.get(overworld);
        DimensionEffectData second = DimensionEffectData.get(overworld);
        check("the dimension data is cached per world", first == second);
        check("the dimension id matches", first.getDimensionId() == 0);
        check("the intensity window is ordered", first.getMinRainIntensity() <= first.getMaxRainIntensity());

        WorldServer nether = worldServer(-1);
        if (nether != null) {
            DimensionEffectData netherData = DimensionEffectData.get(nether);
            check("the nether has its own data", netherData != first);
            check("the nether data has the nether id", netherData.getDimensionId() == -1);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // /rain status
    // ---------------------------------------------------------------------------------------------------------

    private static void checkRainCommand() {
        WorldServer world = overworld();
        if (world == null) return;

        ICommandManager manager = MinecraftServer.getServer()
            .getCommandManager();
        check(
            "the /rain command is registered",
            manager.getCommands()
                .containsKey("rain"));

        DimensionEffectData data = DimensionEffectData.get(world);
        String status = CommandRain.statusOutput(world, data);
        check("the status output names the dimension", status.startsWith("dim 0:"));
        check("the status output reports the surface flag", status.contains("isSurface: true"));
        check("the status output reports a rain timer", status.contains("timer: "));
        check("the status output reports the rain strength", status.contains("strength: "));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Network channel
    // ---------------------------------------------------------------------------------------------------------

    private static void checkNetworkChannel() {
        check("the network channel was registered", Network.network != null);
        check(
            "the channel is registered on the client side",
            NetworkRegistry.INSTANCE.hasChannel("dynamicsurroundings", Side.CLIENT));
        check(
            "the channel is registered on the server side",
            NetworkRegistry.INSTANCE.hasChannel("dynamicsurroundings", Side.SERVER));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Aurora serialization against the real NBT classes
    // ---------------------------------------------------------------------------------------------------------

    private static void checkAuroraNbtRoundTrip() {
        WorldServer world = overworld();
        if (world == null) return;

        AuroraData aurora = new AuroraData(0, 100, -64, 12345L, 3, 1);
        check("an aurora records its dimension", aurora.dimensionId == 0);
        check("an aurora records its seed", aurora.seed == 12345L);

        NBTTagCompound nbt = new NBTTagCompound();
        aurora.writeToNBT(nbt);

        AuroraData restored = new AuroraData();
        restored.readFromNBT(nbt);
        check("the aurora survives the NBT round trip", aurora.equals(restored));
        check("the aurora colour set survives the NBT round trip", restored.colorSet == aurora.colorSet);
        check("the aurora preset survives the NBT round trip", restored.preset == aurora.preset);
        check("the aurora seed survives the NBT round trip", restored.seed == aurora.seed);

        check("auroras are equal by dimension and column", aurora.equals(restored));
        check("a different column is a different aurora", !aurora.equals(new AuroraData(0, 101, -64, 12345L, 3, 1)));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------

    private static WorldServer overworld() {
        return worldServer(0);
    }

    private static WorldServer worldServer(int dimensionId) {
        try {
            return MinecraftServer.getServer()
                .worldServerForDimension(dimensionId);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void check(String description, boolean condition) {
        checks++;
        if (condition) {
            System.out.println(PASS + description);
        } else {
            failures++;
            System.out.println(FAIL + description);
        }
    }
}
