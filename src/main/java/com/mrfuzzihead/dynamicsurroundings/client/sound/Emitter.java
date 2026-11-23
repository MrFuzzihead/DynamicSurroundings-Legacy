package com.mrfuzzihead.dynamicsurroundings.client.sound;

import java.util.Random;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.util.random.XorShiftRandom;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import paulscode.sound.SoundSystemConfig;

@SideOnly(Side.CLIENT)
class Emitter {

    protected static final Random RANDOM = new XorShiftRandom();
    protected final SoundEffect effect;
    protected PlayerSound activeSound;
    protected int repeatDelay = 0;

    public Emitter(SoundEffect sound) {
        this.effect = sound;
    }

    public void update() {
        if (!(SoundSystemConfig.getMasterGain() <= 0.0F)) {
            if (this.activeSound == null) {
                this.activeSound = new PlayerSound(this.effect);
            } else if (DynamicSoundManager.isSoundPlaying(this.activeSound)) {
                return;
            }

            try {
                DynamicSoundManager.playSound(this.activeSound);
            } catch (Throwable var2) {
                ModLog.error("Unable to play sound", var2);
            }
        }
    }

    public void setVolume(float volume) {
        if (this.activeSound != null) {
            this.activeSound.setVolume(volume);
        }
    }

    public float getVolume() {
        return this.activeSound != null ? this.activeSound.getVolume() : 0.0F;
    }

    public void fade() {
        if (this.activeSound != null) {
            this.activeSound.fadeAway();
        }
    }
}
