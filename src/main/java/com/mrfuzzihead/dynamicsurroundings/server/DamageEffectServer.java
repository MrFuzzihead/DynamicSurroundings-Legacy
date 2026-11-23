package com.mrfuzzihead.dynamicsurroundings.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.potion.Potion;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.network.HealthData;
import com.mrfuzzihead.dynamicsurroundings.network.Network;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Server-side damage/heal detector for the client pop-off packets. */
public final class DamageEffectServer {

    private DamageEffectServer() {}

    public static void initialize() {
        if (ModOptions.enableDamagePopoffs) {
            MinecraftForge.EVENT_BUS.register(new DamageEffectServer());
        }
    }

    private static boolean isCritical(EntityPlayer player, Entity target) {
        return player.fallDistance > 0.0F && !player.onGround
            && !player.isOnLadder()
            && !player.isInWater()
            && !player.isPotionActive(Potion.blindness)
            && player.ridingEntity == null
            && target instanceof EntityLivingBase;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event != null && event.entity != null && event.entity.worldObj != null && !event.entity.worldObj.isRemote) {
            if (!(event.ammount <= 0.0F) && event.entityLiving != null) {
                boolean isCrit = false;
                if (event.source instanceof EntityDamageSourceIndirect) {
                    EntityDamageSourceIndirect dmgSource = (EntityDamageSourceIndirect) event.source;
                    if (dmgSource.getSourceOfDamage() instanceof EntityArrow) {
                        EntityArrow arrow = (EntityArrow) dmgSource.getSourceOfDamage();
                        isCrit = arrow.getIsCritical();
                    }
                } else if (event.source instanceof EntityDamageSource) {
                    EntityDamageSource dmgSource = (EntityDamageSource) event.source;
                    if (dmgSource.getSourceOfDamage() instanceof EntityPlayer) {
                        EntityPlayer player = (EntityPlayer) dmgSource.getSourceOfDamage();
                        isCrit = isCritical(player, event.entityLiving);
                    }
                }

                HealthData data = new HealthData(event.entityLiving, isCrit, (int) event.ammount);
                Network.sendHealthUpdate(data, event.entity.worldObj.provider.dimensionId);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingHeal(LivingHealEvent event) {
        if (event != null && event.entity != null && event.entity.worldObj != null && !event.entity.worldObj.isRemote) {
            if (!(event.amount <= 0.0F) && event.entityLiving != null
                && event.entityLiving.getHealth() != event.entityLiving.getMaxHealth()) {
                HealthData data = new HealthData(event.entityLiving, false, -((int) event.amount));
                Network.sendHealthUpdate(data, event.entity.worldObj.provider.dimensionId);
            }
        }
    }
}
