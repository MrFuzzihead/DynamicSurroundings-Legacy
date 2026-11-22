package com.mrfuzzihead.dynamicsurroundings.client.hud;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class GuiHUDHandler {

    private static final List<GuiHUDHandler.IGuiOverlay> overlays = new ArrayList<>();

    private GuiHUDHandler() {}

    public static void register(GuiHUDHandler.IGuiOverlay overlay) {
        overlays.add(overlay);
    }

    public static void initialize() {
        if (ModOptions.potionHudEnabled) {
            register(new PotionHUD());
        }

        MinecraftForge.EVENT_BUS.register(new GuiHUDHandler());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderExperienceBar(RenderGameOverlayEvent event) {
        // HUDCaching (Angelica) rebuilds the overlay framebuffer on its own schedule. Only the non-cancelable
        // EXPERIENCE pass is the actual cached frame; Pre/Post/Text and the synthetic passes would redraw the
        // potion list several times per frame.
        if (event.type != ElementType.EXPERIENCE || event.isCancelable()) {
            return;
        }

        for (GuiHUDHandler.IGuiOverlay overlay : overlays) {
            overlay.doRender(event);
        }
    }

    public interface IGuiOverlay {

        void doRender(RenderGameOverlayEvent var1);
    }
}
