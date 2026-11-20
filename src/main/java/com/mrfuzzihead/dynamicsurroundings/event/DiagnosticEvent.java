package com.mrfuzzihead.dynamicsurroundings.event;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import cpw.mods.fml.common.eventhandler.Event;

public class DiagnosticEvent extends Event {

    public static class Gather extends DiagnosticEvent {

        public final World world;
        public final EntityPlayer player;
        public final List<String> output = new ArrayList<>();

        public Gather(World world, EntityPlayer player) {
            this.world = world;
            this.player = player;
        }
    }
}
