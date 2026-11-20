package com.mrfuzzihead.dynamicsurroundings.client.fx.particle;

import net.minecraft.block.Block;
import net.minecraft.client.particle.EntityBlockDustFX;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EntityFountainJetFX extends EntityJetFX {

    protected final Block block;

    public EntityFountainJetFX(int strength, World world, double x, double y, double z, Block block) {
        super(strength, world, x, y, z, 1);
        this.block = block;
    }

    @Override
    protected EntityFX getJetParticle() {
        double motionX = RANDOM.nextGaussian() * 0.03;
        double motionZ = RANDOM.nextGaussian() * 0.03;
        return new EntityFountainJetFX.EntityFoundtainParitcleFX(
            this.worldObj,
            this.posX,
            this.posY,
            this.posZ,
            motionX,
            0.5,
            motionZ,
            this.block);
    }

    protected static final class EntityFoundtainParitcleFX extends EntityBlockDustFX {

        public EntityFoundtainParitcleFX(World world, double x, double y, double z, double dX, double dY, double dZ,
            Block block) {
            super(
                world,
                x + EntityJetFX.RANDOM.nextGaussian() * 0.2,
                y,
                z + EntityJetFX.RANDOM.nextGaussian() * 0.2,
                dX,
                dY,
                dZ,
                block,
                0);
            this.multipleParticleScaleBy((float) (0.3F + EntityJetFX.RANDOM.nextGaussian() / 10.0));
            this.setPosition(this.posX, this.posY, this.posZ);
        }
    }
}
