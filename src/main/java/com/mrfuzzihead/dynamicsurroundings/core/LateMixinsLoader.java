package com.mrfuzzihead.dynamicsurroundings.core;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.mrfuzzihead.dynamicsurroundings.ModOptions;

/**
 * Registers the late mixin config. Late mixins are the ones that need another mod's classes, which must not be
 * resolved while the coremod is loading.
 *
 * <p>
 * The annotation is what tells GTNHMixins to instantiate this class and call {@link #getMixins}; the config file
 * name returned here must match {@code src/main/resources/mixins.dynamicsurroundings.late.json}.
 * </p>
 */
@LateMixin
public class LateMixinsLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.dynamicsurroundings.late.json";
    }

    @Nonnull
    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        // Keep the config in sync if a late mixin ever becomes config-gated. The call is idempotent.
        ModOptions.synchronizeConfiguration(null);
        return IMixins.getLateMixins(Mixins.class, loadedMods);
    }
}
