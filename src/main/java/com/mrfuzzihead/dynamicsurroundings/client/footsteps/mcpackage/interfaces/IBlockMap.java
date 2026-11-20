package org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces;

import java.util.List;

import net.minecraft.block.Block;

import org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.implem.Substrate;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IBlockMap extends IRegistration {

    String getBlockMap(Block var1, int var2);

    String getBlockMapSubstrate(Block var1, int var2, Substrate var3);

    void collectData(Block var1, int var2, List<String> var3);
}
