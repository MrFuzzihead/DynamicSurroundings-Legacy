package org.blockartistry.mod.DynSurround.server;

import java.util.Set;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import org.blockartistry.mod.DynSurround.ModLog;
import org.blockartistry.mod.DynSurround.ModOptions;
import org.blockartistry.mod.DynSurround.data.AuroraData;
import org.blockartistry.mod.DynSurround.data.AuroraPreset;
import org.blockartistry.mod.DynSurround.data.BiomeRegistry;
import org.blockartistry.mod.DynSurround.data.ColorPair;
import org.blockartistry.mod.DynSurround.data.DimensionEffectData;
import org.blockartistry.mod.DynSurround.data.DimensionRegistry;
import org.blockartistry.mod.DynSurround.network.Network;
import org.blockartistry.mod.DynSurround.util.DiurnalUtils;
import org.blockartistry.mod.DynSurround.util.PlayerUtils;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent;
import gnu.trove.map.hash.TIntIntHashMap;

public final class ServerEffectHandler {

    private static final float RESET = -10.0F;
    private static final long MIN_AURORA_DISTANCE_SQ = 160000L;
    private static final int CHECK_INTERVAL = 100;
    private static TIntIntHashMap tickCounters = new TIntIntHashMap();

    public static void initialize() {
        FMLCommonHandler.instance()
            .bus()
            .register(new ServerEffectHandler());
    }

    @SubscribeEvent
    public void tickEvent(WorldTickEvent event) {
        if (event.phase == Phase.END) {
            if (ModOptions.auroraEnable) {
                this.processAuroras(event);
            }
        } else {
            if (!ModOptions.disableWeatherEffects) {
                World world = event.world;
                int dimensionId = world.provider.dimensionId;
                float sendIntensity = DimensionRegistry.hasWeather(world) ? DimensionEffectData.get(world)
                    .getRainIntensity() : -10.0F;
                Network.sendRainIntensity(sendIntensity, dimensionId);
            }
        }
    }

    private static boolean isAuroraInRange(EntityPlayerMP player, Set<AuroraData> data) {
        for (AuroraData aurora : data) {
            long deltaX = aurora.posX - (int) player.posX;
            long deltaZ = aurora.posZ - (int) player.posZ + -ModOptions.auroraSpawnOffset;
            long distSq = deltaX * deltaX + deltaZ * deltaZ;
            if (distSq <= 160000L) {
                return true;
            }
        }

        return false;
    }

    private static boolean okToSpawnAurora(World world) {
        return DiurnalUtils.isNighttime(world);
    }

    protected void processAuroras(WorldTickEvent event) {
        World world = event.world;
        if (world != null && DimensionRegistry.hasAuroras(world)) {
            Set<AuroraData> data = DimensionEffectData.get(world)
                .getAuroraList();
            if (DiurnalUtils.isDaytime(world)) {
                data.clear();
            } else {
                int tickCount = tickCounters.get(world.provider.dimensionId) + 1;
                tickCounters.put(world.provider.dimensionId, tickCount);
                if (tickCount % 100 == 0) {
                    if (okToSpawnAurora(world)) {
                        for (EntityPlayerMP player : MinecraftServer.getServer()
                            .getConfigurationManager().playerEntityList) {
                            if (BiomeRegistry.hasAurora(PlayerUtils.getPlayerBiome(player, false))
                                && !isAuroraInRange(player, data)) {
                                int colorSet = ColorPair.randomId();
                                int preset = AuroraPreset.randomId();
                                AuroraData aurora = new AuroraData(
                                    player,
                                    -ModOptions.auroraSpawnOffset,
                                    colorSet,
                                    preset);
                                if (data.add(aurora)) {
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
