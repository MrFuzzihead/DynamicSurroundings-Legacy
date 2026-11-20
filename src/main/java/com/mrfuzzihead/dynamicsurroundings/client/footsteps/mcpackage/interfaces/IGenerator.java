package org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IGenerator {

    void generateFootsteps(EntityPlayer var1);
}
