package com.mrfuzzihead.dynamicsurroundings.client.sound;

import net.minecraft.client.audio.PositionedSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.EnvironStateHandler;
import com.mrfuzzihead.dynamicsurroundings.compat.BlockPos;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SpotSound extends PositionedSound {

    private static final int SPOT_SOUND_RANGE = 6;
    private final SoundEffect sound;
    private final int timeMark;

    public SpotSound(SoundEffect sound) {
        super(new ResourceLocation(sound.sound));
        this.sound = sound;
        this.volume = sound.volume;
        this.field_147663_c = sound.getPitch(EnvironStateHandler.EnvironState.RANDOM);
        this.repeat = false;
        this.field_147665_h = 0;
        this.xPosF = (float) EnvironStateHandler.EnvironState.getPlayer().posX + 0.5F;
        this.yPosF = (float) EnvironStateHandler.EnvironState.getPlayer().posY + 32.0F;
        this.zPosF = (float) EnvironStateHandler.EnvironState.getPlayer().posZ + 0.5F;
        this.field_147666_i = AttenuationType.NONE;
        this.timeMark = EnvironStateHandler.EnvironState.getTickCounter();
    }

    public SpotSound(BlockPos pos, SoundEffect sound, int delay) {
        super(new ResourceLocation(sound.sound));
        this.sound = sound;
        this.volume = sound.volume;
        this.field_147663_c = sound.getPitch(EnvironStateHandler.EnvironState.RANDOM);
        this.repeat = false;
        this.field_147665_h = 0;
        this.xPosF = pos.getX() + 0.5F;
        this.yPosF = pos.getY() + 0.5F;
        this.zPosF = pos.getZ() + 0.5F;
        this.timeMark = EnvironStateHandler.EnvironState.getTickCounter() + delay;
    }

    public SpotSound(EntityPlayer player, SoundEffect sound) {
        super(new ResourceLocation(sound.sound));
        this.sound = sound;
        this.volume = sound.volume;
        this.field_147663_c = sound.getPitch(EnvironStateHandler.EnvironState.RANDOM);
        this.repeat = false;
        this.field_147665_h = 0;
        this.xPosF = MathHelper.floor_double(
            player.posX + EnvironStateHandler.EnvironState.RANDOM.nextInt(6)
                - EnvironStateHandler.EnvironState.RANDOM.nextInt(6));
        this.yPosF = MathHelper.floor_double(
            player.posY + 1.0
                + EnvironStateHandler.EnvironState.RANDOM.nextInt(6)
                - EnvironStateHandler.EnvironState.RANDOM.nextInt(6));
        this.zPosF = MathHelper.floor_double(
            player.posZ + EnvironStateHandler.EnvironState.RANDOM.nextInt(6)
                - EnvironStateHandler.EnvironState.RANDOM.nextInt(6));
        this.timeMark = EnvironStateHandler.EnvironState.getTickCounter();
    }

    public float getVolume() {
        return super.getVolume() * ModOptions.masterSoundScaleFactor;
    }

    public int getTickAge() {
        return EnvironStateHandler.EnvironState.getTickCounter() - this.timeMark;
    }

    public SoundEffect getSoundEffect() {
        return this.sound;
    }

    public String toString() {
        return this.sound.toString();
    }
}
