package org.blockartistry.mod.DynSurround.client.fog;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import javax.annotation.Nonnull;
import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;

@SideOnly(Side.CLIENT)
public class VanillaFogRangeCalculator implements IFogRangeCalculator {
   @Nonnull
   @Override
   public FogResult calculate(@Nonnull RenderFogEvent event) {
      return new FogResult(event);
   }

   @Override
   public void tick() {
   }
}
