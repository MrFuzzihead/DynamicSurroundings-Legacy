package com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system;

import com.mrfuzzihead.dynamicsurroundings.client.EnvironStateHandler;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.ILibrary;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.ISoundPlayer;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces.IBlockMap;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces.IDefaultStepPlayer;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces.IGenerator;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces.IGeneratorSettable;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces.IIsolator;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces.IPrimitiveMap;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces.ISolver;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces.IVariator;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces.IVariatorSettable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PFIsolator implements IIsolator, IVariatorSettable, IGeneratorSettable {

    private ILibrary acoustics;
    private ISolver solver;
    private IBlockMap blockMap;
    private IPrimitiveMap primitiveMap;
    private ISoundPlayer soundPlayer;
    private IDefaultStepPlayer defaultStepPlayer;
    private IVariator VAR;
    private IGenerator generator;

    @Override
    public void onFrame() {
        if (this.generator != null) {
            this.generator.generateFootsteps(EnvironStateHandler.EnvironState.getPlayer());
            this.acoustics.think();
        }
    }

    @Override
    public ILibrary getAcoustics() {
        return this.acoustics;
    }

    @Override
    public ISolver getSolver() {
        return this.solver;
    }

    @Override
    public IBlockMap getBlockMap() {
        return this.blockMap;
    }

    @Override
    public IPrimitiveMap getPrimitiveMap() {
        return this.primitiveMap;
    }

    @Override
    public ISoundPlayer getSoundPlayer() {
        return this.soundPlayer;
    }

    @Override
    public IDefaultStepPlayer getDefaultStepPlayer() {
        return this.defaultStepPlayer;
    }

    @Override
    public void setAcoustics(ILibrary acoustics) {
        this.acoustics = acoustics;
    }

    @Override
    public void setSolver(ISolver solver) {
        this.solver = solver;
    }

    @Override
    public void setBlockMap(IBlockMap blockMap) {
        this.blockMap = blockMap;
    }

    @Override
    public void setPrimitiveMap(IPrimitiveMap primitiveMap) {
        this.primitiveMap = primitiveMap;
    }

    @Override
    public void setSoundPlayer(ISoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
    }

    @Override
    public void setDefaultStepPlayer(IDefaultStepPlayer defaultStepPlayer) {
        this.defaultStepPlayer = defaultStepPlayer;
    }

    @Override
    public void setVariator(IVariator var) {
        this.VAR = var;
        this.fixVariator(this.generator);
    }

    @Override
    public void setGenerator(IGenerator generator) {
        this.generator = generator;
        this.fixVariator(this.generator);
    }

    private void fixVariator(Object possiblyAVariator) {
        if (possiblyAVariator != null) {
            if (possiblyAVariator instanceof IVariatorSettable) {
                ((IVariatorSettable) possiblyAVariator).setVariator(this.VAR);
            }
        }
    }
}
