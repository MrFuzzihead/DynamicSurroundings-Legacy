package com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.implem;

import java.util.EnumMap;
import java.util.Map;

import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.EventType;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.IAcoustic;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.INamedAcoustic;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.IOptions;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.ISoundPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EventSelectorAcoustics implements INamedAcoustic {

    private final String name;
    private final Map<EventType, IAcoustic> pairs = new EnumMap<>(EventType.class);

    public EventSelectorAcoustics(String acousticName) {
        this.name = acousticName;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void playSound(ISoundPlayer player, Object location, EventType event, IOptions inputOptions) {
        IAcoustic acoustic = this.pairs.get(event);
        if (acoustic != null) {
            acoustic.playSound(player, location, event, inputOptions);
        } else if (event.canTransition()) {
            this.playSound(player, location, event.getTransitionDestination(), inputOptions);
        }
    }

    public void setAcousticPair(EventType type, IAcoustic acoustic) {
        this.pairs.put(type, acoustic);
    }
}
