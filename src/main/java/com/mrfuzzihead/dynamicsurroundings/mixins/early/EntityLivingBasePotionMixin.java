package com.mrfuzzihead.dynamicsurroundings.mixins.early;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;

/** Suppresses the local player's potion swirl without mutating the shared datawatcher colour. */
@Mixin(EntityLivingBase.class)
public abstract class EntityLivingBasePotionMixin {

    @Inject(
        method = "updatePotionEffects",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnParticle(Ljava/lang/String;DDDDDD)V"),
        cancellable = true,
        require = 1)
    private void dynamicsurroundings$suppressPotionParticles(CallbackInfo ci) {
        if (ModOptions.suppressPotionParticles && (Object) this instanceof EntityPlayer) {
            ci.cancel();
        }
    }
}
