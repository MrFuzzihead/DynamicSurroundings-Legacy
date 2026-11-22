package com.mrfuzzihead.dynamicsurroundings.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.fx.particle.EntityCriticalPopOffFX;
import com.mrfuzzihead.dynamicsurroundings.client.fx.particle.EntityDamagePopOffFX;
import com.mrfuzzihead.dynamicsurroundings.client.fx.particle.EntityHealPopOffFX;
import com.mrfuzzihead.dynamicsurroundings.network.HealthData;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class DamageEffectHandler {

    @SideOnly(Side.CLIENT)
    public static void handleEvent(HealthData data) {
        if (ModOptions.enableDamagePopoffs) {
            if (!EnvironStateHandler.EnvironState.isPlayer(data.entityId)) {
                double distance = EnvironStateHandler.EnvironState.distanceToPlayer(data.posX, data.posY, data.posZ);
                if (!(distance >= 1024.0)) {
                    World world = EnvironStateHandler.EnvironState.getWorld();
                    if (world == null) {
                        return;
                    }

                    EffectRenderer renderer = Minecraft.getMinecraft().effectRenderer;
                    if (data.isCritical) {
                        EntityFX fx = new EntityCriticalPopOffFX(world, data.posX, data.posY, data.posZ);
                        renderer.addEffect(fx);
                    }

                    EntityFX fx;
                    if (data.amount > 0) {
                        fx = new EntityDamagePopOffFX(world, data.posX, data.posY, data.posZ, data.amount);
                    } else {
                        fx = new EntityHealPopOffFX(
                            world,
                            data.posX,
                            data.posY,
                            data.posZ,
                            MathHelper.abs_int(data.amount));
                    }

                    renderer.addEffect(fx);
                }
            }
        }
    }
}
