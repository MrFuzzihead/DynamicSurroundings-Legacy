package org.blockartistry.mod.DynSurround.client.gui;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.client.IModGuiFactory.RuntimeOptionCategoryElement;
import cpw.mods.fml.client.IModGuiFactory.RuntimeOptionGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

@SideOnly(Side.CLIENT)
public class ConfigGuiFactory implements IModGuiFactory {
   public void initialize(Minecraft minecraftInstance) {
   }

   public Class<? extends GuiScreen> mainConfigGuiClass() {
      return DynSurroundConfigGui.class;
   }

   public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
      return null;
   }

   public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
      return null;
   }
}
