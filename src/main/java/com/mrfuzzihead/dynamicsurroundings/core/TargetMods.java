package com.mrfuzzihead.dynamicsurroundings.core;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

/**
 * Mods the late mixins depend on. Declaring them through the GTNH mixin builders (rather than checking
 * {@code Loader.isModLoaded} inside a class body) keeps the mixin from loading before the target mod exists.
 */
public enum TargetMods implements ITargetMod {

    BIOMESOPLENTY("BiomesOPlenty");

    private final TargetModBuilder builder;

    TargetMods(String modId) {
        this.builder = new TargetModBuilder().setModId(modId);
    }

    @Nonnull
    @Override
    public TargetModBuilder getBuilder() {
        return builder;
    }
}
