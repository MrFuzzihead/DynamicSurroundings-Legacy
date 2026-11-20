package com.mrfuzzihead.dynamicsurroundings.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.EntityRenderer;

import com.mrfuzzihead.dynamicsurroundings.client.aurora.AuroraRenderer;
import com.mrfuzzihead.dynamicsurroundings.client.weather.StormRenderer;
import com.mrfuzzihead.dynamicsurroundings.client.weather.StormSplashRenderer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class RenderWeather {

    private static final List<IAtmosRenderer> renderList = new ArrayList<>();

    public static void register(IAtmosRenderer renderer) {
        renderList.add(renderer);
    }

    public static void addRainParticles(EntityRenderer theThis) {
        StormSplashRenderer.renderStormSplashes(EnvironStateHandler.EnvironState.getDimensionId(), theThis);
    }

    public static void renderRainSnow(EntityRenderer theThis, float partialTicks) {
        for (IAtmosRenderer renderer : renderList) {
            renderer.render(theThis, partialTicks);
        }
    }

    static {
        register(new StormRenderer());
        register(new AuroraRenderer());
    }
}
