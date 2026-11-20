package org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces;

import org.blockartistry.mod.DynSurround.client.footsteps.util.property.simple.ConfigProperty;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IVariator {

    void loadConfig(ConfigProperty var1);
}
