package com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.implem;

import java.util.EnumMap;
import java.util.Map;

import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.IOptions;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ConfigOptions implements IOptions {

    private final Map<IOptions.Option, Object> map = new EnumMap<>(IOptions.Option.class);

    public Map<IOptions.Option, Object> getMap() {
        return this.map;
    }

    @Override
    public boolean hasOption(IOptions.Option option) {
        return this.map.containsKey(option);
    }

    @Override
    public Object getOption(IOptions.Option option) {
        return this.map.get(option);
    }
}
