package com.mrfuzzihead.dynamicsurroundings.client.fog;

import javax.annotation.Nonnull;

import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IFogRangeCalculator {

    @Nonnull
    FogResult calculate(@Nonnull RenderFogEvent var1);

    void tick();
}
