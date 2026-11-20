package com.mrfuzzihead.dynamicsurroundings.core;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.mrfuzzihead.dynamicsurroundings.ModOptions;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.Name;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.SortingIndex;

/**
 * The mod's coremod entry point. It registers the early mixin config and the one remaining bytecode transformer
 * that cannot be expressed as a mixin (see {@link SoundManagerSyncTransformer}).
 *
 * <p>
 * The config is loaded here rather than only in pre-init because the {@link Mixins} catalogue gates some entries on
 * values from it ({@code disableWeatherEffects}) and {@link #getMixins} runs before FML pre-init. Like
 * FuzziTweaks, the file is resolved from {@code Launch.minecraftHome} and loaded once; pre-init then re-reads the
 * same file through the normal Forge path.
 * </p>
 */
@MCVersion("1.7.10")
@SortingIndex(10001)
@Name("dynamicsurroundingscore")
public class EarlyMixinsLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { SoundManagerSyncTransformer.class.getName() };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        File mcLocation = (File) data.get("mcLocation");
        if (mcLocation != null) {
            ModOptions
                .synchronizeConfiguration(new File(mcLocation, "config/dynamicsurroundings/dynamicsurroundings.cfg"));
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String getMixinConfig() {
        return "mixins.dynamicsurroundings.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        // getMixins() can run before injectData(), so make the config available here as well. The call is
        // idempotent: a second invocation for the same file is a no-op.
        ModOptions.synchronizeConfiguration(null);
        return IMixins.getEarlyMixins(Mixins.class, loadedCoreMods);
    }
}
