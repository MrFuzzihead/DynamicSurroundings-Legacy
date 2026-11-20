package org.blockartistry.mod.DynSurround.client.fog;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;

@SideOnly(Side.CLIENT)
public class HolisticFogRangeCalculator implements IFogRangeCalculator {
   protected final List<IFogRangeCalculator> calculators = new ArrayList<>();
   protected final FogResult cached = new FogResult();

   public void add(@Nonnull IFogRangeCalculator calc) {
      this.calculators.add(calc);
   }

   @Nonnull
   @Override
   public FogResult calculate(@Nonnull RenderFogEvent event) {
      float start = event.farPlaneDistance;
      float end = event.farPlaneDistance;

      for (IFogRangeCalculator calc : this.calculators) {
         FogResult result = calc.calculate(event);
         start = Math.min(start, result.getStart());
         end = Math.min(end, result.getEnd());
      }

      this.cached.set(start, end);
      return this.cached;
   }

   @Override
   public void tick() {
      for (IFogRangeCalculator calc : this.calculators) {
         calc.tick();
      }
   }

   @Nonnull
   @Override
   public String toString() {
      return this.cached.toString();
   }
}
