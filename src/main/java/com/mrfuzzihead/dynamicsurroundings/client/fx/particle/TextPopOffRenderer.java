package com.mrfuzzihead.dynamicsurroundings.client.fx.particle;

import java.util.Iterator;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import com.mrfuzzihead.dynamicsurroundings.compat.angelica.IrisPhaseBridge;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws all active {@link EntityTextPopOffFX} markers after the world, instead of inside the particle/instancing pass.
 */
@SideOnly(Side.CLIENT)
public final class TextPopOffRenderer {

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (EntityTextPopOffFX.active()
            .isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityLivingBase view = mc.renderViewEntity;
        if (view == null) {
            return;
        }

        double cameraX = view.lastTickPosX + (view.posX - view.lastTickPosX) * event.partialTicks;
        double cameraY = view.lastTickPosY + (view.posY - view.lastTickPosY) * event.partialTicks;
        double cameraZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * event.partialTicks;

        Object phase = IrisPhaseBridge.push("PARTICLES");
        try {
            Iterator<EntityTextPopOffFX> iterator = EntityTextPopOffFX.active()
                .iterator();
            while (iterator.hasNext()) {
                EntityTextPopOffFX popOff = iterator.next();
                if (popOff.isDead || popOff.worldObj != mc.theWorld) {
                    EntityTextPopOffFX.active()
                        .remove(popOff);
                    continue;
                }

                popOff.renderText(event.partialTicks, cameraX, cameraY, cameraZ);
            }
        } finally {
            IrisPhaseBridge.pop(phase);
        }
    }
}
