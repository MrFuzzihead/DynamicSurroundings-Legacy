package com.mrfuzzihead.dynamicsurroundings.client.fog;

import javax.annotation.Nonnull;

import net.minecraftforge.client.event.EntityViewRenderEvent.FogColors;

import com.mrfuzzihead.dynamicsurroundings.util.RgbColor;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class VanillaFogColorCalculator implements IFogColorCalculator {

    @Nonnull
    @Override
    public RgbColor calculate(@Nonnull FogColors event) {
        return new RgbColor(event.red, event.green, event.blue);
    }

    @Override
    public void tick() {}
}
