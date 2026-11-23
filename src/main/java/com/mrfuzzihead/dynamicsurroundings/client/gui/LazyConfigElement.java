package com.mrfuzzihead.dynamicsurroundings.client.gui;

import java.util.List;
import java.util.function.Supplier;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;

import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A category whose children are generated only when the user opens it. */
@SideOnly(Side.CLIENT)
public class LazyConfigElement extends MyConfigElement<Object> {

    private final Supplier<ConfigCategory> supplier;
    private ConfigCategory built;

    public LazyConfigElement(String name, String label, Supplier<ConfigCategory> supplier) {
        super(new ConfigCategory(name), label);
        this.supplier = supplier;
    }

    @Override
    public List<IConfigElement> getChildElements() {
        if (this.built == null) {
            this.built = this.supplier.get();
        }
        return new ConfigElement<Object>(this.built).getChildElements();
    }

    public boolean isBuilt() {
        return this.built != null;
    }
}
