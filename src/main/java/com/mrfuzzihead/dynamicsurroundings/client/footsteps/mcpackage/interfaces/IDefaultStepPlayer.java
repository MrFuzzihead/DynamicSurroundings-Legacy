package com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces;

import net.minecraft.entity.EntityLivingBase;

import com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system.Association;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IDefaultStepPlayer {

    void playStep(EntityLivingBase var1, Association var2);
}
