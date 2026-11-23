package com.mrfuzzihead.dynamicsurroundings.client.fog;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraftforge.client.event.EntityViewRenderEvent.FogColors;

import com.mrfuzzihead.dynamicsurroundings.util.RgbColor;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class HolisticFogColorCalculator implements IFogColorCalculator {

    protected List<IFogColorCalculator> calculators = new ArrayList<>();
    protected RgbColor cached;

    public void add(@Nonnull IFogColorCalculator calc) {
        this.calculators.add(calc);
    }

    @Override
    public RgbColor calculate(@Nonnull FogColors event) {
        RgbColor result = null;

        for (IFogColorCalculator calc : this.calculators) {
            RgbColor color = calc.calculate(event);
            if (result == null) {
                result = color;
            } else if (color != null) {
                result = result.mix(color);
            }
        }

        return this.cached = result;
    }

    @Override
    public void tick() {
        for (IFogColorCalculator calc : this.calculators) {
            calc.tick();
        }
    }

    @Override
    public String toString() {
        return this.cached != null ? this.cached.toString() : "<NOT SET>";
    }
}
