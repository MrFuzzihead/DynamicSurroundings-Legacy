package org.blockartistry.mod.DynSurround.client.fx.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.particle.EntityBlockDustFX;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

@SideOnly(Side.CLIENT)
public class EntityDustJetFX extends EntityJetFX {
   protected final Block block;

   public EntityDustJetFX(int strength, World world, double x, double y, double z, Block block) {
      super(strength, world, x, y, z, 2);
      this.block = block;
   }

   @Override
   protected EntityFX getJetParticle() {
      return new EntityDustJetFX.EntityDustFX(this.worldObj, this.posX, this.posY, this.posZ, this.block);
   }

   protected static final class EntityDustFX extends EntityBlockDustFX {
      public EntityDustFX(World world, double x, double y, double z, Block block) {
         super(world, x + EntityJetFX.RANDOM.nextGaussian() * 0.2, y, z + EntityJetFX.RANDOM.nextGaussian() * 0.2, 0.0, 0.0, 0.0, block, 0);
         this.multipleParticleScaleBy((float)(0.3F + EntityJetFX.RANDOM.nextGaussian() / 30.0));
         this.setPosition(this.posX, this.posY, this.posZ);
      }
   }
}
