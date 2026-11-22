package com.mrfuzzihead.dynamicsurroundings.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.event.world.WorldEvent.Unload;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.Footsteps;
import com.mrfuzzihead.dynamicsurroundings.client.fx.BlockEffectHandler;
import com.mrfuzzihead.dynamicsurroundings.client.sound.SoundManager;
import com.mrfuzzihead.dynamicsurroundings.compat.angelica.AngelicaWeatherProvider;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientEffectHandler {

    private static final List<IClientEffectHandler> effectHandlers = new ArrayList<>();

    public static void register(IClientEffectHandler handler) {
        effectHandlers.add(handler);
        if (handler.hasEvents()) {
            MinecraftForge.EVENT_BUS.register(handler);
            FMLCommonHandler.instance()
                .bus()
                .register(handler);
        }
    }

    private ClientEffectHandler() {}

    public static void initialize() {
        ClientEffectHandler handler = new ClientEffectHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance()
            .bus()
            .register(handler);
        register(new EnvironStateHandler());
        register(new BiomeSurveyHandler());
        register(new FogEffectHandler());
        register(new BlockEffectHandler());
        if (ModOptions.blockedSounds.length > 0 || ModOptions.culledSounds.length > 0) {
            register(new SoundBlockHandler());
        }

        if (ModOptions.enableFootstepSounds) {
            register(new Footsteps());
        }

        if (ModOptions.auroraEnable) {
            register(new AuroraEffectHandler());
        }

        if (ModOptions.enableBiomeSounds) {
            register(new PlayerSoundEffectHandler());
        }

    }

    static boolean canProcess(World world, EntityPlayer player) {
        return world != null && player != null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void clientTick(ClientTickEvent event) {
        if (!Minecraft.getMinecraft()
            .isGamePaused()) {
            World world = FMLClientHandler.instance()
                .getClient().theWorld;
            EntityPlayer player = FMLClientHandler.instance()
                .getClient().thePlayer;
            if (canProcess(world, player)) {
                if (event.phase == Phase.START) {
                    for (IClientEffectHandler handler : effectHandlers) {
                        handler.process(world, player);
                    }

                    // The ambience handler is conditional, but the sound queue also receives block/step sounds from
                    // always-registered handlers. Drive it unconditionally so those sounds play with biome sounds off.
                    SoundManager.update();
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onWorldLoad(Load e) {
        if (e.world.isRemote) {
            DimensionRegistry.loading(e.world);
            AngelicaWeatherProvider.install(e.world);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onWorldUnload(Unload e) {
        if (e.world.isRemote) {
            AngelicaWeatherProvider.uninstall(e.world);
        }
    }
}
