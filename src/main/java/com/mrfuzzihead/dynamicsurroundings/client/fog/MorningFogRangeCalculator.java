package com.mrfuzzihead.dynamicsurroundings.client.fog;

import javax.annotation.Nonnull;

import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.EnvironStateHandler;
import com.mrfuzzihead.dynamicsurroundings.client.EnvironmentSnapshot;
import com.mrfuzzihead.dynamicsurroundings.util.MathStuff;
import com.mrfuzzihead.dynamicsurroundings.util.random.XorShiftRandom;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MorningFogRangeCalculator extends VanillaFogRangeCalculator {

    protected static final float START = 0.63F;
    protected static final float MID = 0.73F;
    protected static final float END = 0.83F;
    protected static final float RESERVE = 10.0F;
    protected int fogDay = -1;
    protected boolean doFog = false;
    protected final FogResult cache = new FogResult();

    @Nonnull
    @Override
    public FogResult calculate(@Nonnull RenderFogEvent event) {
        this.cache.set(event);
        EnvironmentSnapshot snapshot = EnvironStateHandler.EnvironState.getSnapshot();
        if (this.doFog && event.farPlaneDistance * FogResult.DEFAULT_PLANE_SCALE > 10.0F
            && snapshot.getDimensionId() != 1
            && snapshot.getDimensionId() != -1) {
            float ca = EnvironStateHandler.EnvironState.getWorld()
                .getCelestialAngle((float) event.renderPartialTicks);
            if (ca >= START && ca <= END) {
                float factor = 1.0F - MathStuff.abs(ca - MID) / 0.100000024F;
                float endScale = 1.0F - FogResult.DEFAULT_PLANE_SCALE * factor;
                float startScale = 1.0F - 2.0F * factor;
                if (event.farPlaneDistance > 0.0F) {
                    startScale = Math.max(startScale, 11.0F / (event.farPlaneDistance * FogResult.DEFAULT_PLANE_SCALE));
                }
                this.cache.set(event.fogMode, startScale, endScale);
            }
        }

        return this.cache;
    }

    private int getDay() {
        long time = EnvironStateHandler.EnvironState.getWorld()
            .getWorldTime();
        return (int) (time / 24000L);
    }

    @Override
    public void tick() {
        int day = this.getDay();
        if (this.fogDay != day) {
            this.fogDay = day;
            this.doFog = ModOptions.morningFogChance < 2 || XorShiftRandom.current()
                .nextInt(ModOptions.morningFogChance) == 0;
        }
    }
}
