package com.mrfuzzihead.dynamicsurroundings.client.sound;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundPoolEntry;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCdevice;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.EnvironStateHandler;
import com.mrfuzzihead.dynamicsurroundings.compat.BlockPos;
import com.mrfuzzihead.dynamicsurroundings.data.SoundRegistry;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors.SoundHandlerAccessor;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors.SoundManagerAccessor;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import paulscode.sound.SoundSystemConfig;

@SideOnly(Side.CLIENT)
public class SoundManager {

    private static final int AGE_THRESHOLD_TICKS = 5;
    private static final int SOUND_QUEUE_SLACK = 6;
    private static final Map<SoundEffect, Emitter> emitters = new HashMap<>();
    private static final List<SpotSound> pending = new ArrayList<>();
    private static int normalChannelCount = 0;
    private static int streamChannelCount = 0;

    public static void clearSounds() {
        for (Emitter emit : emitters.values()) {
            emit.fade();
        }

        emitters.clear();
        pending.clear();
    }

    public static void queueAmbientSounds(List<SoundEffect> sounds) {
        Map<String, SoundEffect> working = new LinkedHashMap<>();
        for (SoundEffect sound : sounds) {
            working.put(sound.sound, sound);
        }

        Iterator<SoundEffect> iterator = emitters.keySet()
            .iterator();
        while (iterator.hasNext()) {
            SoundEffect effect = iterator.next();
            SoundEffect incoming = working.remove(effect.sound);
            if (incoming == null) {
                emitters.get(effect)
                    .fade();
                iterator.remove();
            } else {
                emitters.get(effect)
                    .setVolume(incoming.getVolume());
            }
        }

        for (SoundEffect sound : working.values()) {
            emitters.put(sound, new Emitter(sound));
        }
    }

    public static void update() {
        for (Emitter emitter : emitters.values()) {
            emitter.update();
        }

        Iterator<SpotSound> pitr = pending.iterator();

        while (pitr.hasNext()) {
            SpotSound sound = pitr.next();
            if (sound.getTickAge() >= 5) {
                ModLog.debug("AGING: " + sound.toString());
                pitr.remove();
            } else if (sound.getTickAge() >= 0 && canFitSound()) {
                playSound(sound);
                pitr.remove();
            }
        }
    }

    public static int currentSoundCount() {
        return ((SoundManagerAccessor) liveSoundManager()).dynamicsurroundings$getPlayingSounds()
            .size();
    }

    public static int maxSoundCount() {
        return SoundSystemConfig.getNumberNormalChannels() + SoundSystemConfig.getNumberStreamingChannels();
    }

    static boolean canFitSound(int current, int normalChannels, int streamingChannels) {
        return current < normalChannels + streamingChannels - SOUND_QUEUE_SLACK;
    }

    private static boolean canFitSound() {
        return canFitSound(
            currentSoundCount(),
            SoundSystemConfig.getNumberNormalChannels(),
            SoundSystemConfig.getNumberStreamingChannels());
    }

    static void playSound(ISound sound) {
        if (sound != null) {
            if (ModOptions.enableDebugLogging) {
                ModLog.debug("PLAYING: " + sound.toString());
            }

            SoundHandler h = Minecraft.getMinecraft()
                .getSoundHandler();
            h.playSound(sound);
        }
    }

    public static void playSoundAtPlayer(SoundEffect sound) {
        SpotSound s = new SpotSound(sound);
        playSound(s);
    }

    public static void playSoundAtPlayer(EntityPlayer player, SoundEffect sound) {
        if (player == null) {
            player = EnvironStateHandler.EnvironState.getSnapshot()
                .getPlayer();
        }

        SpotSound s = new SpotSound(player, sound);
        if (!canFitSound()) {
            pending.add(s);
        } else {
            playSound(s);
        }
    }

    public static void playSoundAt(BlockPos pos, SoundEffect sound, int tickDelay) {
        if (tickDelay <= 0 || canFitSound()) {
            SpotSound s = new SpotSound(pos, sound, tickDelay);
            if (tickDelay <= 0 && canFitSound()) {
                playSound(s);
            } else {
                pending.add(s);
            }
        }
    }

    public static boolean isSoundPlaying(@Nonnull ISound sound) {
        net.minecraft.client.audio.SoundManager manager = liveSoundManager();
        SoundManagerAccessor accessor = (SoundManagerAccessor) manager;
        return manager.isSoundPlaying(sound) || accessor.dynamicsurroundings$getDelayedSounds()
            .containsKey(sound);
    }

    private static net.minecraft.client.audio.SoundManager liveSoundManager() {
        return ((SoundHandlerAccessor) Minecraft.getMinecraft()
            .getSoundHandler()).dynamicsurroundings$getSndManager();
    }

    public static List<String> getSounds() {
        List<String> result = new ArrayList<>();

        for (SoundEffect effect : emitters.keySet()) {
            result.add(
                "EMITTER: " + effect.toString()
                    + "[vol:"
                    + emitters.get(effect)
                        .getVolume()
                    + "]");
        }

        for (SpotSound effect : pending) {
            result.add(
                (effect.getTickAge() < 0 ? "DELAYED: " : "PENDING: ") + effect.getSoundEffect()
                    .toString());
        }

        return result;
    }

    private static float getVolume(@Nonnull SoundCategory category) {
        GameSettings settings = Minecraft.getMinecraft().gameSettings;
        return settings != null && category != null && category != SoundCategory.MASTER
            ? settings.getSoundLevel(category)
            : 1.0F;
    }

    public static float getNormalizedVolume(ISound sound, SoundPoolEntry poolEntry, SoundCategory category) {
        float result = 0.0F;
        if (sound == null) {
            ModLog.warn("getNormalizedVolume(): Null sound parameter");
            return result;
        } else {
            String soundName = sound.getPositionedSoundLocation()
                .toString();
            if (poolEntry == null) {
                ModLog.warn("getNormalizedVolume(%s): Null poolEntry parameter", soundName);
            } else if (category == null) {
                ModLog.warn("getNormalizedVolume(%s): Null category parameter", soundName);
            } else {
                try {
                    float volumeScale = SoundRegistry.getVolumeScale(soundName);
                    result = (float) MathHelper.clamp_double(
                        sound.getVolume() * poolEntry.getVolume() * getVolume(category) * volumeScale,
                        0.0,
                        1.0);
                } catch (Throwable var6) {
                    ModLog.error("getNormalizedVolume(): Unable to calculate " + soundName, var6);
                }
            }

            return result;
        }
    }

    public static float getNormalizedPitch(ISound sound, SoundPoolEntry poolEntry) {
        float result = 0.0F;
        if (sound == null) {
            ModLog.warn("getNormalizedPitch(): Null sound parameter");
            return result;
        } else {
            String soundName = sound.getPositionedSoundLocation()
                .toString();
            if (poolEntry == null) {
                ModLog.warn("getNormalizedPitch(%s): Null poolEntry parameter", soundName);
            } else {
                try {
                    result = (float) MathHelper.clamp_double(sound.getPitch() * poolEntry.getPitch(), 0.5, 2.0);
                } catch (Throwable var5) {
                    ModLog.error("getNormalizedPitch(): Unable to calculate " + soundName, var5);
                }
            }

            return result;
        }
    }

    static int[] resolveChannels(int totalChannels, boolean autoConfigure, int normal, int streaming) {
        int resolvedNormal = normal;
        int resolvedStreaming = streaming;
        if (autoConfigure && totalChannels > 64) {
            int scaledTotal = (totalChannels + 1) * 3 / 4;
            resolvedStreaming = scaledTotal / 5;
            resolvedNormal = scaledTotal - resolvedStreaming;
        }

        return new int[] { resolvedNormal, resolvedStreaming, totalChannels };
    }

    private static int queryDeviceChannels() {
        if (AL.isCreated()) {
            IntBuffer ib = BufferUtils.createIntBuffer(1);
            ALC10.alcGetInteger(AL.getDevice(), 4112, ib);
            return ib.get(0);
        }

        ALCdevice device = ALC10.alcOpenDevice(null);
        if (device == null) {
            return -1;
        }

        try {
            IntBuffer ib = BufferUtils.createIntBuffer(1);
            ALC10.alcGetInteger(device, 4112, ib);
            return ib.get(0);
        } finally {
            ALC10.alcCloseDevice(device);
        }
    }

    public static void configureSound() {
        int totalChannels = -1;
        try {
            totalChannels = queryDeviceChannels();
        } catch (Throwable var3) {
            ModLog.warn("Unable to query OpenAL channel count; using configured channels");
            ModLog.catching(var3);
        }

        int[] resolved = resolveChannels(
            totalChannels,
            ModOptions.autoConfigureChannels,
            ModOptions.normalSoundChannelCount,
            ModOptions.streamingSoundChannelCount);
        normalChannelCount = resolved[0];
        streamChannelCount = resolved[1];

        ModLog.info(
            "Sound channels: %d normal, %d streaming (total avail: %s)",
            normalChannelCount,
            streamChannelCount,
            totalChannels == -1 ? "UNKNOWN" : Integer.toString(totalChannels));
        SoundSystemConfig.setNumberNormalChannels(normalChannelCount);
        SoundSystemConfig.setNumberStreamingChannels(streamChannelCount);
    }
}
