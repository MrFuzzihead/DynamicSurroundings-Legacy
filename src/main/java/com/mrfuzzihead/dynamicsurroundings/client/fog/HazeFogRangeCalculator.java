package com.mrfuzzihead.dynamicsurroundings.client.fog;

import javax.annotation.Nonnull;

import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;

import com.mrfuzzihead.dynamicsurroundings.client.EnvironStateHandler;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class HazeFogRangeCalculator extends VanillaFogRangeCalculator {

    protected static final int BAND_OFFSETS = 15;
    protected static final int BAND_CORE_SIZE = 10;
    protected static final float IMPACT_FAR = 0.6F;
    protected static final float IMPACT_NEAR = 0.95F;
    protected final FogResult cached = new FogResult();

    @Nonnull
    @Override
    public FogResult calculate(@Nonnull RenderFogEvent event) {
        DimensionRegistry di = EnvironStateHandler.EnvironState.getDimensionInfo();
        if (di == null) {
            this.cached.set(event);
            return this.cached;
        } else {
            if (di.getHasHaze()) {
                float lowY = di.getCloudHeight() - 15;
                float highY = di.getCloudHeight() + 15 + 10;
                double eyeHeight = EnvironStateHandler.EnvironState.getPlayer().posY
                    + EnvironStateHandler.EnvironState.getPlayer().eyeHeight;
                if (eyeHeight >= lowY && eyeHeight <= highY) {
                    float coreLowY = lowY + 15.0F;
                    float coreHighY = coreLowY + 10.0F;
                    float scaleFar = 0.6F;
                    float scaleNear = 0.95F;
                    if (eyeHeight < coreLowY) {
                        float factor = (float) ((eyeHeight - lowY) / 15.0);
                        scaleFar *= factor;
                        scaleNear *= factor;
                    } else if (eyeHeight > coreHighY) {
                        float factor = (float) ((highY - eyeHeight) / 15.0);
                        scaleFar *= factor;
                        scaleNear *= factor;
                    }

                    float end = event.farPlaneDistance * (1.0F - scaleFar);
                    float start = event.farPlaneDistance * (1.0F - scaleNear);
                    this.cached.set(start, end);
                    return this.cached;
                }
            }

            this.cached.set(event);
            return this.cached;
        }
    }
}
