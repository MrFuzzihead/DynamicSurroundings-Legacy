package com.mrfuzzihead.dynamicsurroundings.client;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.aurora.Aurora;
import com.mrfuzzihead.dynamicsurroundings.data.AuroraData;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;
import com.mrfuzzihead.dynamicsurroundings.util.DiurnalUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class AuroraEffectHandler implements IClientEffectHandler {

    private static int auroraDimension = 0;
    private static final Set<AuroraData> auroras = new HashSet<>();
    public static volatile Aurora currentAurora;

    public static void addAurora(AuroraData data) {
        if (ModOptions.auroraEnable) {
            synchronized (auroras) {
                if (auroraDimension != data.dimensionId
                    || EnvironStateHandler.EnvironState.getDimensionId() != data.dimensionId) {
                    auroras.clear();
                    currentAurora = null;
                    auroraDimension = data.dimensionId;
                }

                auroras.add(data);
            }
        }
    }

    private Aurora getClosestAurora(World world, EntityPlayer player) {
        if (!ModOptions.auroraEnable || !DimensionRegistry.hasAuroras(world) || DiurnalUtils.isDaytime(world)) {
            synchronized (auroras) {
                auroras.clear();
                currentAurora = null;
            }
            return null;
        }

        if (player == null) {
            return null;
        }

        AuroraData ad = null;
        synchronized (auroras) {
            if (auroraDimension != world.provider.dimensionId) {
                auroras.clear();
            }

            if (auroras.isEmpty()) {
                currentAurora = null;
                return null;
            }

            int playerX = (int) player.posX;
            int playerZ = (int) player.posZ;
            boolean started = false;
            int distanceSq = 0;

            for (AuroraData data : auroras) {
                int deltaX = data.posX - playerX;
                int deltaZ = data.posZ - playerZ;
                int d = deltaX * deltaX + deltaZ * deltaZ;
                if (!started || distanceSq > d) {
                    started = true;
                    distanceSq = d;
                    ad = data;
                }
            }
        }

        if (ad == null) {
            currentAurora = null;
        } else if (currentAurora == null || currentAurora.posX != ad.posX || currentAurora.posZ != ad.posZ) {
            ModLog.debug("New aurora: " + ad.toString());
            currentAurora = new Aurora(ad);
        }

        return currentAurora;
    }

    @Override
    public boolean hasEvents() {
        return false;
    }

    @Override
    public void process(World world, EntityPlayer player) {
        Aurora aurora = this.getClosestAurora(world, player);
        if (aurora != null) {
            aurora.update();
            if (aurora.isAlive() && DiurnalUtils.isSunrise(world)) {
                ModLog.debug("Aurora fade...");
                aurora.die();
            }
        }
    }
}
