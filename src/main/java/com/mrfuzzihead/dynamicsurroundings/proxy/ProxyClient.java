package com.mrfuzzihead.dynamicsurroundings.proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.common.MinecraftForge;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.ClientEffectHandler;
import com.mrfuzzihead.dynamicsurroundings.client.aurora.AuroraRenderHandler;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system.ForgeDictionary;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.user.GenerateBlockReport;
import com.mrfuzzihead.dynamicsurroundings.client.fx.particle.TextPopOffRenderer;
import com.mrfuzzihead.dynamicsurroundings.client.hud.GuiHUDHandler;
import com.mrfuzzihead.dynamicsurroundings.client.sound.SoundManager;
import com.mrfuzzihead.dynamicsurroundings.client.sound.cache.SoundCacheReloadListener;
import com.mrfuzzihead.dynamicsurroundings.client.waila.WailaHandler;
import com.mrfuzzihead.dynamicsurroundings.compat.angelica.AngelicaDebugEntries;
import com.mrfuzzihead.dynamicsurroundings.data.BlockRegistry;
import com.mrfuzzihead.dynamicsurroundings.data.SoundRegistry;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors.SoundHandlerAccessor;

import cpw.mods.fml.common.Loader;
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
        if (Loader.isModLoaded("Waila")) {
            WailaHandler.register();
        }

        BlockRegistry.initialize();
        ClientEffectHandler.initialize();
        MinecraftForge.EVENT_BUS.register(new AuroraRenderHandler());
        MinecraftForge.EVENT_BUS.register(new TextPopOffRenderer());
        AngelicaDebugEntries.register();
        GuiHUDHandler.initialize();
        SoundRegistry.initialize();
        SoundManager.configureSound();
        ((IReloadableResourceManager) Minecraft.getMinecraft()
            .getResourceManager()).registerReloadListener(new SoundCacheReloadListener());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        if (ModOptions.enableDebugLogging) {
            SoundHandler handler = Minecraft.getMinecraft()
                .getSoundHandler();
            List<String> sounds = new ArrayList<>();

            for (Object resource : ((SoundHandlerAccessor) handler).dynamicsurroundings$getSndRegistry()
                .getKeys()) {
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
