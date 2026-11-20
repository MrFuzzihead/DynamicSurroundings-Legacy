package org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces;

import net.minecraft.entity.EntityLivingBase;

import org.blockartistry.mod.DynSurround.client.footsteps.game.system.Association;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IDefaultStepPlayer {

    void playStep(EntityLivingBase var1, Association var2);
}
