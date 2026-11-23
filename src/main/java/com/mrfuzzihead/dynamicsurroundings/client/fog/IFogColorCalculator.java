package com.mrfuzzihead.dynamicsurroundings.client.fog;

import javax.annotation.Nonnull;

import net.minecraftforge.client.event.EntityViewRenderEvent.FogColors;

import com.mrfuzzihead.dynamicsurroundings.util.RgbColor;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IFogColorCalculator {

    @Nonnull
    RgbColor calculate(@Nonnull FogColors var1);

    void tick();
}
