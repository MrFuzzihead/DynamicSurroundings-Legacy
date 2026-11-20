package org.blockartistry.mod.DynSurround.client.gui;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Property;

import org.apache.commons.lang3.StringUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MyConfigElement<T> extends ConfigElement<T> {

    protected String label;

    public MyConfigElement(ConfigCategory ctgy) {
        this(ctgy, null);
    }

    public MyConfigElement(ConfigCategory ctgy, String label) {
        super(ctgy);
        this.label = label;
    }

    public MyConfigElement(Property prop) {
        this(prop, null);
    }

    public MyConfigElement(Property prop, String label) {
        super(prop);
        this.label = label;
    }

    public String getName() {
        return StringUtils.isEmpty(this.label) ? super.getName() : this.label;
    }
}
