package com.mrfuzzihead.dynamicsurroundings.client;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IClientEffectHandler {

    void process(World var1, EntityPlayer var2);

    boolean hasEvents();
}
