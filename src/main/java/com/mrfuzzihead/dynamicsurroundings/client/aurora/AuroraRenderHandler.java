package com.mrfuzzihead.dynamicsurroundings.client.aurora;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.AuroraEffectHandler;
import com.mrfuzzihead.dynamicsurroundings.compat.angelica.IrisPhaseBridge;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws the aurora after the world instead of from inside {@code renderRainSnow}.
 *
 * <p>
 * The old render path was registered in the same list as {@code StormRenderer}, so it ran in the RAIN_SNOW phase and
 * disappeared whenever another mod (Angelica) took ownership of that phase, or when
 * {@code disableWeatherEffects} removed the weather mixin. {@link RenderWorldLastEvent} is independent of both and
 * gives the immediate-mode draw a stable point after terrain and weather.
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class AuroraRenderHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!ModOptions.auroraEnable) {
            return;
        }

        World world = Minecraft.getMinecraft().theWorld;
        if (world == null || !DimensionRegistry.hasAuroras(world)) {
            return;
        }

        if (!ModOptions.auroraRenderWithShaders && IrisPhaseBridge.isShaderPipelineActive()) {
            return;
        }

        if (ModOptions.auroraOnlyWhenLookingNorth && Minecraft.getMinecraft().thePlayer.getLookVec().zCoord > -0.5D) {
            return;
        }

        Aurora aurora = AuroraEffectHandler.currentAurora;
        if (aurora != null) {
            Object phase = IrisPhaseBridge.push("CLOUDS");
            try {
                AuroraRenderer.renderAurora(event.partialTicks, aurora);
            } finally {
                IrisPhaseBridge.pop(phase);
            }
        }
    }
}
