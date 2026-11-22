package com.mrfuzzihead.dynamicsurroundings.proxy;

import net.minecraft.command.ICommandManager;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.server.MinecraftServer;

import com.mrfuzzihead.dynamicsurroundings.VersionCheck;
import com.mrfuzzihead.dynamicsurroundings.commands.CommandRain;
import com.mrfuzzihead.dynamicsurroundings.data.BiomeRegistry;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;
import com.mrfuzzihead.dynamicsurroundings.network.Network;
import com.mrfuzzihead.dynamicsurroundings.server.DamageEffectServer;
import com.mrfuzzihead.dynamicsurroundings.server.ServerEffectHandler;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class Proxy {

    public void preInit(FMLPreInitializationEvent event) {
        VersionCheck.register();
    }

    public void init(FMLInitializationEvent event) {
        Network.initialize();
        ServerEffectHandler.initialize();
        DamageEffectServer.initialize();
    }

    public void postInit(FMLPostInitializationEvent event) {
        BiomeRegistry.initialize();
        DimensionRegistry.initialize();
    }

    public void serverStarting(FMLServerStartingEvent event) {
        MinecraftServer server = MinecraftServer.getServer();
        ICommandManager command = server.getCommandManager();
        ServerCommandManager serverCommand = (ServerCommandManager) command;
        serverCommand.registerCommand(new CommandRain());
    }
}
