package com.mrfuzzihead.dynamicsurroundings.mixins.early;

import net.minecraft.world.WorldServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrfuzzihead.dynamicsurroundings.server.PlayerSleepHandler;

/**
 * Port of the {@code PatchWorldServer} coremod patch. Sleep is supposed to clear rain and thunder, but the mod
 * routes the reset through {@link PlayerSleepHandler} so the "reset on sleep" config option is honoured.
 */
@Mixin(WorldServer.class)
public abstract class WorldServerMixin {

    @Inject(method = "resetRainAndThunder", at = @At("HEAD"), cancellable = true, require = 1)
    private void dynamicsurroundings$resetRainAndThunder(CallbackInfo ci) {
        PlayerSleepHandler.resetRainAndThunder((WorldServer) (Object) this);
        ci.cancel();
    }
}
