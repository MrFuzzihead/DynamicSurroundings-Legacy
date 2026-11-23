package com.mrfuzzihead.dynamicsurroundings.mixins.early;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrfuzzihead.dynamicsurroundings.compat.ExternalWeatherPolicy;
import com.mrfuzzihead.dynamicsurroundings.server.WorldHandler;

/**
 * Port of the {@code PatchWorld} coremod patch. Vanilla's {@code updateWeatherBody} is replaced by the per-dimension
 * rain/thunder state machine in {@link WorldHandler}. The old transformer inserted the handler call and a
 * {@code RETURN} at the top of the method, which made the vanilla body dead code; cancelling at {@code HEAD} is the
 * mixin equivalent.
 */
@Mixin(World.class)
public abstract class WorldMixin {

    @Inject(method = "updateWeatherBody", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void dynamicsurroundings$updateWeatherBody(CallbackInfo ci) {
        if (ExternalWeatherPolicy.isDeferred()) {
            return;
        }
        WorldHandler.updateWeatherBody((World) (Object) this);
        ci.cancel();
    }
}
