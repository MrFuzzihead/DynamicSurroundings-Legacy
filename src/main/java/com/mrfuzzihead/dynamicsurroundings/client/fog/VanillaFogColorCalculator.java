package org.blockartistry.mod.DynSurround.client.fog;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import javax.annotation.Nonnull;
import net.minecraftforge.client.event.EntityViewRenderEvent.FogColors;
import org.blockartistry.mod.DynSurround.util.Color;

@SideOnly(Side.CLIENT)
public class VanillaFogColorCalculator implements IFogColorCalculator {
   @Nonnull
   @Override
   public Color calculate(@Nonnull FogColors event) {
      return new Color(event.red, event.green, event.blue);
   }

   @Override
   public void tick() {
   }
}
