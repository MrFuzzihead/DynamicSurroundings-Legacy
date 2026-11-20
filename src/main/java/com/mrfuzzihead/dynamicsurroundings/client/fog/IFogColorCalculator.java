package com.mrfuzzihead.dynamicsurroundings.client.fog;

import javax.annotation.Nonnull;

import net.minecraftforge.client.event.EntityViewRenderEvent.FogColors;

import com.mrfuzzihead.dynamicsurroundings.util.Color;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IFogColorCalculator {

    @Nonnull
    Color calculate(@Nonnull FogColors var1);

    void tick();
}
