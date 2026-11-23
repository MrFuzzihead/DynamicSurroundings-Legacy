package com.mrfuzzihead.dynamicsurroundings.client.fog;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class HolisticFogRangeCalculator implements IFogRangeCalculator {

    protected final List<IFogRangeCalculator> calculators = new ArrayList<>();
    protected final FogResult cached = new FogResult();

    public void add(@Nonnull IFogRangeCalculator calc) {
        this.calculators.add(calc);
    }

    @Nonnull
    @Override
    public FogResult calculate(@Nonnull RenderFogEvent event) {
        float startScale = 1.0F;
        float endScale = 1.0F;

        for (IFogRangeCalculator calc : this.calculators) {
            FogResult result = calc.calculate(event);
            startScale = Math.min(startScale, result.getStartScale());
            endScale = Math.min(endScale, result.getEndScale());
        }

        this.cached.set(event.fogMode, startScale, endScale);
        return this.cached;
    }

    @Override
    public void tick() {
        for (IFogRangeCalculator calc : this.calculators) {
            calc.tick();
        }
    }

    @Nonnull
    @Override
    public String toString() {
        return this.cached.toString();
    }
}
