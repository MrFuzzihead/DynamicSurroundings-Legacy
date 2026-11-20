package org.blockartistry.mod.DynSurround.client.footsteps.parsers;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Map;
import java.util.Map.Entry;
import org.blockartistry.mod.DynSurround.ModLog;
import org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces.IRegistration;
import org.blockartistry.mod.DynSurround.client.footsteps.util.property.simple.ConfigProperty;

@SideOnly(Side.CLIENT)
public final class Register {
   private Register() {
   }

   public static void setup(ConfigProperty props, IRegistration registration) {
      Map<String, String> properties = props.getAllProperties();

      for (Entry<String, String> entry : properties.entrySet()) {
         try {
            registration.register(entry.getKey(), entry.getValue());
         } catch (Exception var6) {
            ModLog.info("Error making registration " + entry.getKey() + ": " + var6.getMessage());
         }
      }
   }
}
