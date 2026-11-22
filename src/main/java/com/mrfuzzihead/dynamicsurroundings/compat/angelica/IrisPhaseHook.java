package com.mrfuzzihead.dynamicsurroundings.compat.angelica;

import net.coderbot.iris.Iris;
import net.coderbot.iris.pipeline.WorldRenderingPhase;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Direct Iris phase access. Loaded only after {@link IrisPhaseBridge} confirms the Iris class is present.
 */
@SideOnly(Side.CLIENT)
final class IrisPhaseHook {

    private IrisPhaseHook() {}

    static Object push(String phaseName) {
        if (!Iris.enabled) {
            return null;
        }

        WorldRenderingPipeline pipeline = Iris.getPipelineManager()
            .getPipelineNullable();
        if (pipeline == null) {
            return null;
        }

        WorldRenderingPhase previous = pipeline.getPhase();
        pipeline.setPhase(WorldRenderingPhase.valueOf(phaseName));
        return new Scope(pipeline, previous);
    }

    static void pop(Object scope) {
        if (!(scope instanceof Scope)) {
            return;
        }

        Scope state = (Scope) scope;
        state.pipeline.setPhase(state.previous);
    }

    private static final class Scope {

        private final WorldRenderingPipeline pipeline;
        private final WorldRenderingPhase previous;

        private Scope(WorldRenderingPipeline pipeline, WorldRenderingPhase previous) {
            this.pipeline = pipeline;
            this.previous = previous;
        }
    }
}
