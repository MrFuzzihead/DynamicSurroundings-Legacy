package org.blockartistry.mod.DynSurround.client.fx.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntityFlameFX;
import net.minecraft.client.particle.EntityLavaFX;
import net.minecraft.world.World;
import org.blockartistry.mod.DynSurround.client.sound.SoundEffect;
import org.blockartistry.mod.DynSurround.client.sound.SoundManager;
import org.blockartistry.mod.DynSurround.compat.BlockPos;

@SideOnly(Side.CLIENT)
public class EntityFireJetFX extends EntityJetFX {
   private static final SoundEffect FIRE = new SoundEffect("minecraft:fire.fire");
   protected final boolean isLava = RANDOM.nextInt(3) == 0;

   public EntityFireJetFX(int strength, World world, double x, double y, double z) {
      super(strength, world, x, y, z);
   }

   @Override
   public void playSound() {
      SoundManager.playSoundAt(new BlockPos(this), FIRE, 0);
   }

   @Override
   protected EntityFX getJetParticle() {
      if (this.isLava) {
         return new EntityLavaFX(this.worldObj, this.posX, this.posY, this.posZ);
      } else {
         EntityFlameFX flame = new EntityFlameFX(
            this.worldObj, this.posX, this.posY, this.posZ, 0.0, this.jetStrength / 10.0, 0.0
         );
         flame.flameScale = flame.flameScale * this.jetStrength;
         return flame;
      }
   }
}
