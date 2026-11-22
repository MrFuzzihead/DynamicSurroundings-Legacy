package com.mrfuzzihead.dynamicsurroundings.compat;

import cpw.mods.fml.common.Loader;

/**
 * Small, dependency-free mod detection facade. Optional compatibility classes must never name Angelica, NotFine or
 * Iris types from a class that can be loaded without those mods; callers use this facade to decide whether the
 * guarded bridge may be touched.
 */
public final class Compat {

    public static final String ANGELICA = "angelica";
    public static final String NOTFINE = "notfine";
    public static final String IRIS = "iris";
    public static final String OCULUS = "oculus";
    public static final String BIOMES_O_PLENTY = "BiomesOPlenty";

    private static volatile Boolean angelicaBopFogActive;

    private Compat() {}

    public static boolean isAngelicaLoaded() {
        return isLoaded(ANGELICA);
    }

    public static boolean isNotFineLoaded() {
        return isLoaded(NOTFINE);
    }

    public static boolean isIrisLoaded() {
        return isLoaded(IRIS) || isLoaded(OCULUS);
    }

    public static boolean isBiomesOPlentyLoaded() {
        return isLoaded(BIOMES_O_PLENTY);
    }

    /**
     * True when Angelica's late BOP fog mixin is present. The mixin applies whenever both mods are installed, so DS
     * must not also import BOP's {@code IBiomeFog} into its own biome entries; that would blend the same colour and
     * density twice.
     */
    public static boolean isAngelicaBopFogActive() {
        Boolean cached = angelicaBopFogActive;
        if (cached != null) {
            return cached;
        }

        boolean active = isAngelicaLoaded() && isBiomesOPlentyLoaded();
        if (active) {
            try {
                Class
                    .forName("com.gtnewhorizons.angelica.compat.bop.BopFogBlend", false, Compat.class.getClassLoader());
            } catch (ClassNotFoundException | LinkageError | RuntimeException e) {
                active = false;
            }
        }

        angelicaBopFogActive = active;
        return active;
    }

    /**
     * {@link Loader#isModLoaded} assumes FML's {@code LaunchClassLoader} is the context loader. Unit tests (and any
     * early bootstrap call) run on a plain application loader, where the lookup throws instead of returning false.
     * Optional-mod detection must never be able to break the render path, so treat that case as "not loaded".
     */
    private static boolean isLoaded(String modId) {
        try {
            return Loader.isModLoaded(modId);
        } catch (Exception | LinkageError e) {
            return false;
        }
    }
}
