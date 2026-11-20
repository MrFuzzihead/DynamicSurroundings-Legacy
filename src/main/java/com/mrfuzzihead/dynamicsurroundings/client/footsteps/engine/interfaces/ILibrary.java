package com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces;

import com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system.Association;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface ILibrary {

    void addAcoustic(INamedAcoustic var1);

    void playAcoustic(Object var1, Association var2, EventType var3);

    void playAcoustic(Object var1, String var2, EventType var3, IOptions var4);

    void think();
}
