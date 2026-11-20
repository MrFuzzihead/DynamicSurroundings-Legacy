package org.blockartistry.mod.DynSurround.client.footsteps.game.system;

import org.blockartistry.mod.DynSurround.client.EnvironStateHandler;
import org.blockartistry.mod.DynSurround.client.footsteps.engine.interfaces.ILibrary;
import org.blockartistry.mod.DynSurround.client.footsteps.engine.interfaces.ISoundPlayer;
import org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces.IBlockMap;
import org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces.IDefaultStepPlayer;
import org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces.IGenerator;
import org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces.IGeneratorSettable;
import org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces.IIsolator;
import org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces.IPrimitiveMap;
import org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces.ISolver;
import org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces.IVariator;
import org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces.IVariatorSettable;

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
