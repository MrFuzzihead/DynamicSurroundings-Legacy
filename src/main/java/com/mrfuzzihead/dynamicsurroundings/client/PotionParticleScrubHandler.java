package com.mrfuzzihead.dynamicsurroundings.client;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class PotionParticleScrubHandler implements IClientEffectHandler {

    static boolean needsScrub(net.minecraft.entity.DataWatcher watcher) {
        return watcher.getWatchableObjectInt(7) != 0;
    }

    @Override
    public void process(World world, EntityPlayer player) {
        net.minecraft.entity.DataWatcher watcher = player.getDataWatcher();
        if (needsScrub(watcher)) {
            watcher.updateObject(7, 0);
        }
    }

    @Override
    public boolean hasEvents() {
        return false;
    }
}
