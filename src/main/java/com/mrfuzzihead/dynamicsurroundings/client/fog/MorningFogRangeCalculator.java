package com.mrfuzzihead.dynamicsurroundings.client.fog;

import javax.annotation.Nonnull;

import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.EnvironStateHandler;
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
        if (this.doFog && this.cache.getStart() > 10.0F
            && EnvironStateHandler.EnvironState.getDimensionId() != 1
            && EnvironStateHandler.EnvironState.getDimensionId() != -1) {
            float ca = EnvironStateHandler.EnvironState.getWorld()
                .getCelestialAngle((float) event.renderPartialTicks);
            if (ca >= 0.63F && ca <= 0.83F) {
                float factor = 1.0F - MathStuff.abs(ca - 0.73F) / 0.100000024F;
                float shift = this.cache.getStart() * factor;
                float newEnd = this.cache.getEnd() - shift;
                float newStart = MathStuff.clamp(this.cache.getStart() - shift * 2.0F, 11.0F, newEnd);
                this.cache.set(newStart, newEnd);
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
