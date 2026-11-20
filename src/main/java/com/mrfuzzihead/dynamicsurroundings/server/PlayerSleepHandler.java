package com.mrfuzzihead.dynamicsurroundings.server;

import net.minecraft.world.WorldServer;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;

public class PlayerSleepHandler {

    public static void resetRainAndThunder(WorldServer world) {
        if (ModOptions.resetRainOnSleep) {
            world.provider.resetRainAndThunder();
        }
    }
}
