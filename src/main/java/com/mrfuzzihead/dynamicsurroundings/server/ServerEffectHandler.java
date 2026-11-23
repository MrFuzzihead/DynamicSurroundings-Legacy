package com.mrfuzzihead.dynamicsurroundings.server;

import java.util.Set;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent.Unload;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.compat.ExternalWeatherPolicy;
import com.mrfuzzihead.dynamicsurroundings.data.AuroraData;
import com.mrfuzzihead.dynamicsurroundings.data.AuroraPreset;
import com.mrfuzzihead.dynamicsurroundings.data.BiomeRegistry;
import com.mrfuzzihead.dynamicsurroundings.data.ColorPair;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionEffectData;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionEffectDataFile;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;
import com.mrfuzzihead.dynamicsurroundings.network.Network;
import com.mrfuzzihead.dynamicsurroundings.util.DiurnalUtils;
import com.mrfuzzihead.dynamicsurroundings.util.PlayerUtils;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntIntHashMap;

public final class ServerEffectHandler {

    private static final float RESET = -10.0F;
    private static final long MIN_AURORA_DISTANCE_SQ = 160000L;
    private static final int CHECK_INTERVAL = 100;
    private static final int MAX_AURORAS = 32;
    private static final int RAIN_KEEPALIVE_TICKS = 40;
    private static TIntIntHashMap tickCounters = new TIntIntHashMap();
    private static TIntIntHashMap rainTickCounters = new TIntIntHashMap();
    private static TIntFloatHashMap lastRainIntensity = new TIntFloatHashMap();

    public static void initialize() {
        FMLCommonHandler.instance()
            .bus()
            .register(new ServerEffectHandler());
    }

    @SubscribeEvent
    public void onWorldUnload(Unload event) {
        DimensionEffectDataFile.unload(event.world);
    }

    @SubscribeEvent
    public void tickEvent(WorldTickEvent event) {
        if (event.phase == Phase.END) {
            if (ModOptions.auroraEnable) {
                this.processAuroras(event);
            }
        } else {
            if (!ModOptions.disableWeatherEffects && !ExternalWeatherPolicy.isDeferred()) {
                World world = event.world;
                int dimensionId = world.provider.dimensionId;
                float sendIntensity = DimensionRegistry.hasWeather(world) ? DimensionEffectData.get(world)
                    .getRainIntensity() : RESET;
                int tick = rainTickCounters.get(dimensionId) + 1;
                rainTickCounters.put(dimensionId, tick);
                if (shouldSendRainIntensity(
                    sendIntensity,
                    lastRainIntensity.containsKey(dimensionId) ? lastRainIntensity.get(dimensionId) : Float.NaN,
                    tick)) {
                    Network.sendRainIntensity(sendIntensity, dimensionId);
                    lastRainIntensity.put(dimensionId, sendIntensity);
                }
            }
        }
    }

    static boolean canAddAurora(int size) {
        return size < MAX_AURORAS;
    }

    public static boolean shouldSendRainIntensity(float current, float previous, int tick) {
        return Float.isNaN(previous) || Float.compare(current, previous) != 0 || tick % RAIN_KEEPALIVE_TICKS == 0;
    }

    static boolean isAuroraInRange(int playerX, int playerZ, int spawnOffset, Set<AuroraData> data) {
        for (AuroraData aurora : data) {
            long deltaX = aurora.posX - playerX;
            long deltaZ = aurora.posZ - (playerZ - spawnOffset);
            long distSq = deltaX * deltaX + deltaZ * deltaZ;
            if (distSq <= MIN_AURORA_DISTANCE_SQ) {
                return true;
            }
        }

        return false;
    }

    private static boolean isAuroraInRange(EntityPlayerMP player, Set<AuroraData> data) {
        return isAuroraInRange((int) player.posX, (int) player.posZ, ModOptions.auroraSpawnOffset, data);
    }

    private static boolean okToSpawnAurora(World world) {
        return DiurnalUtils.isNighttime(world);
    }

    protected void processAuroras(WorldTickEvent event) {
        World world = event.world;
        if (world != null && DimensionRegistry.hasAuroras(world)) {
            DimensionEffectData effectData = DimensionEffectData.get(world);
            Set<AuroraData> data = effectData.getAuroraList();
            if (DiurnalUtils.isDaytime(world)) {
                if (!data.isEmpty()) {
                    data.clear();
                    effectData.markDirty();
                }
            } else {
                int tickCount = tickCounters.get(world.provider.dimensionId) + 1;
                tickCounters.put(world.provider.dimensionId, tickCount);
                if (tickCount % CHECK_INTERVAL == 0) {
                    if (okToSpawnAurora(world)) {
                        for (EntityPlayerMP player : MinecraftServer.getServer()
                            .getConfigurationManager().playerEntityList) {
                            if (BiomeRegistry.hasAurora(PlayerUtils.getPlayerBiome(player, false))
                                && !isAuroraInRange(player, data)
                                && canAddAurora(data.size())) {
                                int colorSet = ColorPair.randomId();
                                int preset = AuroraPreset.randomId();
                                AuroraData aurora = new AuroraData(
                                    player,
                                    -ModOptions.auroraSpawnOffset,
                                    colorSet,
                                    preset);
                                if (data.add(aurora)) {
                                    effectData.markDirty();
                                    ModLog.debug("Spawned new aurora: " + aurora.toString());
                                }
                            }
                        }
                    }

                    for (AuroraData a : data) {
                        Network.sendAurora(a, world.provider.dimensionId);
                    }
                }
            }
        }
    }
}
