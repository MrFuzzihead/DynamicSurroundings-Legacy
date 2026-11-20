package com.mrfuzzihead.dynamicsurroundings.data.compat;

/**
 * Guarded entry point for the optional BOP fog bridge. This class itself has no BOP references in its signature, so
 * callers that only touch it when BOP is loaded never trigger a {@link NoClassDefFoundError} for the BOP API.
 */
public final class BOPFogIntegration {

    private static final IBOPFog INSTANCE = create();

    private BOPFogIntegration() {}

    public static IBOPFog get() {
        return INSTANCE;
    }

    private static IBOPFog create() {
        try {
            return new BOPFogCompat();
        } catch (Throwable t) {
            // BOP is present but its fog API does not match this version; degrade to no BOP fog.
            return IBOPFog.NONE;
        }
    }
}
