package com.mrfuzzihead.dynamicsurroundings.core;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import com.mrfuzzihead.dynamicsurroundings.ModOptions;

/**
 * The mixin catalogue. Every patch that can be expressed as a mixin is registered here, grouped by the feature that
 * owns it so a single config switch can turn a whole module off at load time. (The sound-manager synchronization
 * transform has no mixin equivalent and stays a class transformer, see {@code SoundManagerSyncTransformer}.)
 *
 * <p>
 * The enum is consumed by {@link EarlyMixinsLoader} through {@link IMixins#getEarlyMixins}, which is what the
 * GTNH mixin framework uses to build the list of mixin class names for this mod's early config.
 * </p>
 */
public enum Mixins implements IMixins {

    /**
     * Field accessors that replace the old {@code dynamicsurroundings_at.cfg} access transformer. They are always
     * applied: the classes that use them are plain code paths, not optional patches, and a missing accessor would
     * fail with a {@link ClassCastException} instead of a readable compile error.
     */
    ACCESSORS(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("accessors.EntityAccessor", "accessors.EntityLivingBaseAccessor")
        .addClientMixins(
            "accessors.EntityRendererAccessor",
            "accessors.EntityFlameFXAccessor",
            "accessors.SoundHandlerAccessor",
            "accessors.SoundManagerAccessor")),

    /** Weather replacement: the old {@code PatchWorld}, {@code PatchWorldServer} and {@code PatchEntityRenderer}. */
    WEATHER(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("WorldMixin", "WorldServerMixin")
        .addClientMixins("EntityRendererMixin")
        .setApplyIf(() -> !ModOptions.disableWeatherEffects)),

    /**
     * Sound engine hooks: the old {@code PatchSoundManager*}, {@code SoundPlayFlush} and
     * {@code PatchSoundManagerPlayTime}. They are part of the sound subsystem and have no separate config switch.
     */
    SOUND(new MixinBuilder().setPhase(Phase.EARLY)
        .addClientMixins("SoundManagerMixin")),

    /**
     * paulscode SoundSystem crash fixes: the old {@code SoundCrashFixSource}, {@code SoundCrashFixLibrary} and
     * {@code SoundCrashFixStreamThread}. Only the client ever loads the sound library.
     */
    SOUND_CRASH_FIX(new MixinBuilder().setPhase(Phase.EARLY)
        .addClientMixins("paulscode.SourceMixin", "paulscode.LibraryMixin", "paulscode.StreamThreadMixin")),

    /**
     * Optional-mod integration. Biomes O' Plenty keeps its fog colour/density on the biome class; the accessor
     * replaces the reflection that used to read those fields. Late phase because BOP's classes must already exist.
     */
    BOP_FOG(new MixinBuilder().setPhase(Phase.LATE)
        .addCommonMixins("bop.BOPBiomeMixin")
        .addRequiredMod(TargetMods.BIOMESOPLENTY));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
