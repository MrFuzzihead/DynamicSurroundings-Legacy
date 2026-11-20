package com.mrfuzzihead.dynamicsurroundings.proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.ClientEffectHandler;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system.ForgeDictionary;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.user.GenerateBlockReport;
import com.mrfuzzihead.dynamicsurroundings.client.hud.GuiHUDHandler;
import com.mrfuzzihead.dynamicsurroundings.client.sound.SoundManager;
import com.mrfuzzihead.dynamicsurroundings.data.BlockRegistry;
import com.mrfuzzihead.dynamicsurroundings.data.SoundRegistry;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ProxyClient extends Proxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        BlockRegistry.initialize();
        ClientEffectHandler.initialize();
        GuiHUDHandler.initialize();
        SoundRegistry.initialize();
        SoundManager.configureSound();
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        if (ModOptions.enableDebugLogging) {
            SoundHandler handler = Minecraft.getMinecraft()
                .getSoundHandler();
            List<String> sounds = new ArrayList<>();

            for (Object resource : handler.sndRegistry.getKeys()) {
                sounds.add(resource.toString());
            }

            Collections.sort(sounds);
            ModLog.info("*** SOUND REGISTRY ***");

            for (String sound : sounds) {
                ModLog.info(sound);
            }

            ModLog.info("*** REGISTERED BLOCK NAMES ***");
            GenerateBlockReport report = new GenerateBlockReport();

            for (String entry : report.getBlockNames()) {
                ModLog.info(entry);
            }

            ForgeDictionary.dumpOreNames();
        }
    }
}
