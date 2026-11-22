package com.mrfuzzihead.dynamicsurroundings.network;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Runs a packet body on the Minecraft client thread.
 *
 * <p>
 * In 1.7.10 {@code SimpleNetworkWrapper} invokes handlers on the Netty event loop. Touch render state, the effect
 * renderer or the client world from there and the result is a data race at best and a GL crash at worst. The vanilla
 * schedule queue is the supported hand-off point.
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    public static void schedule(Runnable task) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }

        if (mc.func_152345_ab()) {
            task.run();
        } else {
            mc.func_152344_a(task);
        }
    }
}
