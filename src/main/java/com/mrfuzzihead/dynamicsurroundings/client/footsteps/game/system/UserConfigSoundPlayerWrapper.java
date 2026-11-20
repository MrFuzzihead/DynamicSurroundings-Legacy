package org.blockartistry.mod.DynSurround.client.footsteps.game.system;

import java.util.Random;

import org.blockartistry.mod.DynSurround.ModOptions;
import org.blockartistry.mod.DynSurround.client.footsteps.engine.interfaces.IOptions;
import org.blockartistry.mod.DynSurround.client.footsteps.engine.interfaces.ISoundPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class UserConfigSoundPlayerWrapper implements ISoundPlayer {

    private final ISoundPlayer wrapped;

    public UserConfigSoundPlayerWrapper(ISoundPlayer wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void playSound(Object location, String soundName, float volume, float pitch, IOptions options) {
        this.wrapped.playSound(location, soundName, volume * ModOptions.footstepsSoundFactor, pitch, options);
    }

    @Override
    public Random getRNG() {
        return this.wrapped.getRNG();
    }
}
