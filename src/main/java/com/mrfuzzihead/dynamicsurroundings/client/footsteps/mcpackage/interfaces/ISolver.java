package com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces;

import net.minecraft.entity.player.EntityPlayer;

import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.EventType;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system.Association;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface ISolver {

    void playAssociation(EntityPlayer var1, Association var2, EventType var3);

    Association findAssociationForPlayer(EntityPlayer var1, double var2, boolean var4);

    Association findAssociationForPlayer(EntityPlayer var1, double var2);

    Association findAssociationForLocation(EntityPlayer var1, int var2, int var3, int var4);

    Association findAssociationForBlock(int var1, int var2, int var3);

    Association findAssociationForBlock(int var1, int var2, int var3, String var4);

    boolean playSpecialStoppingConditions(EntityPlayer var1);

    boolean hasSpecialStoppingConditions(EntityPlayer var1);
}
