package com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.implem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.EventType;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.IAcoustic;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.IOptions;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.ISoundPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SimultaneousAcoustic implements IAcoustic {

    protected final List<IAcoustic> acoustics;

    public SimultaneousAcoustic(Collection<IAcoustic> acoustics) {
        this.acoustics = new ArrayList<>(acoustics);
    }

    @Override
    public void playSound(ISoundPlayer player, Object location, EventType event, IOptions inputOptions) {
        for (IAcoustic acoustic : this.acoustics) {
            acoustic.playSound(player, location, event, inputOptions);
        }
    }
}
