package com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.implem;

import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.IOptions;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.ISoundPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PendingSound {

    private final Object location;
    private final String soundName;
    private final float volume;
    private final float pitch;
    private final IOptions options;
    private final long timeToPlay;
    private final long maximum;

    public PendingSound(Object location, String soundName, float volume, float pitch, IOptions options, long timeToPlay,
        long maximum) {
        this.location = location;
        this.soundName = soundName;
        this.volume = volume;
        this.pitch = pitch;
        this.options = options;
        this.timeToPlay = timeToPlay;
        this.maximum = maximum;
    }

    public void playSound(ISoundPlayer player) {
        player.playSound(this.location, this.soundName, this.volume, this.pitch, this.options);
    }

    public long getTimeToPlay() {
        return this.timeToPlay;
    }

    public long getMaximumBase() {
        return this.maximum;
    }
}
