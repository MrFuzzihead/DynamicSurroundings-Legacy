package org.blockartistry.mod.DynSurround.client.waila;

import cpw.mods.fml.common.Optional.Interface;
import cpw.mods.fml.common.Optional.Method;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameData;
import java.util.List;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.apache.commons.lang3.StringUtils;
import org.blockartistry.mod.DynSurround.ModLog;
import org.blockartistry.mod.DynSurround.ModOptions;
import org.blockartistry.mod.DynSurround.client.footsteps.Footsteps;
import org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.interfaces.IBlockMap;

@Interface(
   iface = "mcp.mobius.waila.api.IWailaDataProvider",
   modid = "Waila"
)
public final class WailaHandler implements IWailaDataProvider {
   private List<String> gatherText(ItemStack stack, List<String> text, IWailaDataAccessor accessor, IWailaConfigHandler config) {
      if (stack != null && stack.getItem() != null) {
         text.add(EnumChatFormatting.GOLD + "dsurround");
         Item item = stack.getItem();
         String blockName = GameData.getItemRegistry().getNameForObject(item);
         if (!StringUtils.isEmpty(blockName)) {
            StringBuilder builder = new StringBuilder();
            builder.append(blockName);
            if (stack.getHasSubtypes()) {
               builder.append(':').append(stack.getItemDamage());
            }

            if (accessor != null) {
               builder.append('[').append(accessor.getMetadata()).append(']');
            }

            text.add(builder.toString());
         }

         if (accessor != null && Footsteps.INSTANCE != null) {
            IBlockMap bm = Footsteps.INSTANCE.getBlockMap();
            if (bm != null) {
               bm.collectData(accessor.getBlock(), accessor.getMetadata(), text);
            }
         }

         return text;
      } else {
         return text;
      }
   }

   @Method(
      modid = "Waila"
   )
   public static void callbackRegister(IWailaRegistrar register) {
      ModLog.info("Registering Waila handler...");
      WailaHandler instance = new WailaHandler();
      register.registerTailProvider(instance, Block.class);
   }

   public WailaHandler() {
      MinecraftForge.EVENT_BUS.register(this);
   }

   @Method(
      modid = "Waila"
   )
   public List<String> getWailaHead(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
      return this.gatherText(itemStack, currenttip, accessor, config);
   }

   @Method(
      modid = "Waila"
   )
   public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
      return this.gatherText(itemStack, currenttip, accessor, config);
   }

   @Method(
      modid = "Waila"
   )
   public List<String> getWailaTail(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
      return this.gatherText(itemStack, currenttip, accessor, config);
   }

   public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
      return null;
   }

   public NBTTagCompound getNBTData(EntityPlayerMP arg0, TileEntity arg1, NBTTagCompound arg2, World arg3, int arg4, int arg5, int arg6) {
      return null;
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST,
      receiveCanceled = false
   )
   public void onToolTipEvent(ItemTooltipEvent event) {
      if (event != null && event.itemStack != null && event.toolTip != null) {
         this.gatherText(event.itemStack, event.toolTip, null, null);
      }
   }

   public static void register() {
      if (ModOptions.enableDebugLogging) {
         FMLInterModComms.sendMessage("Waila", "register", WailaHandler.class.getName() + ".callbackRegister");
      }
   }
}
