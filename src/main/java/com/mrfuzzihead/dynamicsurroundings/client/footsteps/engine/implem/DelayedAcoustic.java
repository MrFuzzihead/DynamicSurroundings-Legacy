package com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.implem;

import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.IOptions;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class DelayedAcoustic extends BasicAcoustic implements IOptions {

    protected long delayMin = 0L;
    protected long delayMax = 0L;

    public DelayedAcoustic() {
        this.outputOptions = this;
    }

    @Override
    public boolean hasOption(IOptions.Option option) {
        return option == IOptions.Option.DELAY_MIN || option == IOptions.Option.DELAY_MAX;
    }

    @Override
    public Object getOption(IOptions.Option option) {
        return option == IOptions.Option.DELAY_MIN ? this.delayMin
            : option == IOptions.Option.DELAY_MAX ? this.delayMax : null;
    }

    public void setDelayMin(long delay) {
        this.delayMin = delay;
    }

    public void setDelayMax(long delay) {
        this.delayMax = delay;
    }
}
