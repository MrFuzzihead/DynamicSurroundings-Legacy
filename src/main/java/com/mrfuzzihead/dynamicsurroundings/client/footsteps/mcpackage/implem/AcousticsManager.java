package org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.implem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.Block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;

import org.blockartistry.mod.DynSurround.ModLog;
import org.blockartistry.mod.DynSurround.client.EnvironStateHandler;
import org.blockartistry.mod.DynSurround.client.footsteps.engine.implem.AcousticsLibrary;
import org.blockartistry.mod.DynSurround.client.footsteps.engine.interfaces.IOptions;
import org.blockartistry.mod.DynSurround.client.footsteps.engine.interfaces.ISoundPlayer;
import org.blockartistry.mod.DynSurround.client.footsteps.game.system.Association;
import org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces.IDefaultStepPlayer;
import org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces.IIsolator;
import org.blockartistry.mod.DynSurround.util.MyUtils;
import org.blockartistry.mod.DynSurround.util.random.XorShiftRandom;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class AcousticsManager extends AcousticsLibrary implements ISoundPlayer, IDefaultStepPlayer {

    private static final Random RANDOM = new XorShiftRandom();
    private static final boolean USING_LATENESS = true;
    private static final boolean USING_EARLYNESS = true;
    private static final float LATENESS_THRESHOLD_DIVIDER = 1.2F;
    private static final double EARLYNESS_THRESHOLD_POW = 0.75;
    private final List<PendingSound> pending = new ArrayList<>();
    private final IIsolator isolator;
    private long minimum;

    public AcousticsManager(IIsolator isolator) {
        this.isolator = isolator;
    }

    @Override
    public void playStep(EntityLivingBase entity, Association assos) {
        try {
            Block block = assos.getBlock();
            if (!block.getMaterial()
                .isLiquid() && block.stepSound != null && block.stepSound.soundName != null) {
                SoundType soundType = block.stepSound;
                if (EnvironStateHandler.EnvironState.getWorld()
                    .getBlock(assos.x, assos.y + 1, assos.z) == Blocks.snow_layer) {
                    soundType = Blocks.snow_layer.stepSound;
                }

                entity.playSound(soundType.getStepResourcePath(), soundType.getVolume() * 0.15F, soundType.getPitch());
            }
        } catch (Throwable var5) {
            ModLog.error("Unable to play step", var5);
        }
    }

    @Override
    public void playSound(Object location, String soundName, float volume, float pitch, IOptions options) {
        if (location instanceof Entity) {
            if (options != null) {
                if (options.hasOption(IOptions.Option.DELAY_MIN) && options.hasOption(IOptions.Option.DELAY_MAX)) {
                    long delay = this.randAB(
                        RANDOM,
                        (Long) options.getOption(IOptions.Option.DELAY_MIN),
                        (Long) options.getOption(IOptions.Option.DELAY_MAX));
                    if (delay < this.minimum) {
                        this.minimum = delay;
                    }

                    this.pending.add(
                        new PendingSound(
                            location,
                            soundName,
                            volume,
                            pitch,
                            null,
                            MyUtils.currentTimeMillis() + delay,
                            options.hasOption(IOptions.Option.SKIPPABLE) ? -1L
                                : (Long) options.getOption(IOptions.Option.DELAY_MAX)));
                } else {
                    this.actuallyPlaySound((Entity) location, soundName, volume, pitch);
                }
            } else {
                this.actuallyPlaySound((Entity) location, soundName, volume, pitch);
            }
        }
    }

    protected void actuallyPlaySound(Entity location, String soundName, float volume, float pitch) {
        if (ModLog.DEBUGGING) {
            ModLog.debug(
                "    Playing sound " + soundName
                    + " ("
                    + String.format(Locale.ENGLISH, "v%.2f, p%.2f", volume, pitch)
                    + ")");
        }

        try {
            location.playSound(soundName, volume, pitch);
        } catch (Throwable var6) {
            ModLog.error("Unable to play sound", var6);
        }
    }

    private long randAB(Random rng, long a, long b) {
        return a >= b ? a : a + rng.nextInt((int) b + 1);
    }

    @Override
    public Random getRNG() {
        return RANDOM;
    }

    @Override
    public void think() {
        if (!this.pending.isEmpty() && MyUtils.currentTimeMillis() >= this.minimum) {
            long newMinimum = Long.MAX_VALUE;
            long time = MyUtils.currentTimeMillis();
            Iterator<PendingSound> iter = this.pending.iterator();

            while (iter.hasNext()) {
                PendingSound sound = iter.next();
                if (time < sound.getTimeToPlay()
                    && !(time >= sound.getTimeToPlay() - Math.pow(sound.getMaximumBase(), 0.75))) {
                    newMinimum = sound.getTimeToPlay();
                } else {
                    if (ModLog.DEBUGGING && time < sound.getTimeToPlay()) {
                        ModLog.debug(
                            "    Playing early sound (early by " + (sound.getTimeToPlay() - time)
                                + "ms, tolerence is "
                                + Math.pow(sound.getMaximumBase(), 0.75));
                    }

                    long lateness = time - sound.getTimeToPlay();
                    if (sound.getMaximumBase() < 0L || (float) lateness <= (float) sound.getMaximumBase() / 1.2F) {
                        sound.playSound(this);
                    } else if (ModLog.DEBUGGING) {
                        ModLog.debug(
                            "    Skipped late sound (late by " + lateness
                                + "ms, tolerence is "
                                + (float) sound.getMaximumBase() / 1.2F
                                + "ms)");
                    }

                    iter.remove();
                }
            }

            this.minimum = newMinimum;
        }
    }

    @Override
    protected ISoundPlayer mySoundPlayer() {
        return this.isolator.getSoundPlayer();
    }
}
