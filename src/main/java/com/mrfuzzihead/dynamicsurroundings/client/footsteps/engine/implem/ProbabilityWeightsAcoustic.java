package com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.implem;

import java.util.ArrayList;
import java.util.List;

import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.EventType;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.IAcoustic;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.IOptions;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.ISoundPlayer;
import com.mrfuzzihead.dynamicsurroundings.util.WeightedRandom;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ProbabilityWeightsAcoustic implements IAcoustic {

    protected final List<IAcoustic> acoustics;
    protected final int[] weights;

    public ProbabilityWeightsAcoustic(List<IAcoustic> acoustics, List<Integer> weights) {
        this.acoustics = new ArrayList<>(acoustics);
        this.weights = new int[weights.size()];

        for (int i = 0; i < weights.size(); i++) {
            this.weights[i] = weights.get(i);
        }
    }

    @Override
    public void playSound(ISoundPlayer player, Object location, EventType event, IOptions inputOptions) {
        int index = WeightedRandom.pickIndex(this.weights, player.getRNG());
        if (index >= 0 && index < this.acoustics.size()) {
            this.acoustics.get(index)
                .playSound(player, location, event, inputOptions);
        }
    }
}
