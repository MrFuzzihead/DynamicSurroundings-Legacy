package org.blockartistry.mod.DynSurround.client.fx.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.particle.EntityBubbleFX;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

@SideOnly(Side.CLIENT)
public class EntityBubbleJetFX extends EntityJetFX {
   public EntityBubbleJetFX(int strength, World world, double x, double y, double z) {
      super(strength, world, x, y, z);
   }

   @Override
   protected EntityFX getJetParticle() {
      return new EntityBubbleFX(this.worldObj, this.posX, this.posY, this.posZ, 0.0, 0.5 + this.jetStrength / 10.0, 0.0);
   }
}
