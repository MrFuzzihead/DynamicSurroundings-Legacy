package com.mrfuzzihead.dynamicsurroundings.asm;

import java.io.File;
import java.util.Map;

import net.minecraftforge.common.config.Configuration;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.Name;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.SortingIndex;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

@MCVersion("1.7.10")
@TransformerExclusions({ "com.mrfuzzihead.dynamicsurroundings.asm." })
@SortingIndex(10001)
@Name("dynamicsurroundingscore")
public class TransformLoader implements IFMLLoadingPlugin {

    public String[] getASMTransformerClass() {
        return new String[] { Transformer.class.getName() };
    }

    public String getAccessTransformerClass() {
        return null;
    }

    public String getSetupClass() {
        return null;
    }

    public void injectData(Map<String, Object> map) {
        File configFile = new File((File) map.get("mcLocation"), "/config/dynamicsurroundings/dynamicsurroundings.cfg");
        Configuration config = new Configuration(configFile);
        ModOptions.load(config);
    }

    public String getModContainerClass() {
        return null;
    }
}
