package com.mrfuzzihead.dynamicsurroundings.compat.notfine;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.compat.Compat;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Guarded entry point for NotFine's settings. The actual references live in {@link NotFineSettingsHook}, which is
 * only loaded when NotFine is present.
 */
@SideOnly(Side.CLIENT)
public final class NotFineBridge {

    private static final int MIN_DOWNFALL_DISTANCE = 0;
    private static final int MAX_DOWNFALL_DISTANCE = 15;

    private NotFineBridge() {}

    public static boolean isAvailable() {
        return Compat.isNotFineLoaded();
    }

    /**
     * Returns NotFine's configured weather radius (0 disables downfall rendering) or {@code fallback} when NotFine is
     * absent. The value is clamped to NotFine's own range.
     */
    public static int downfallDistance(int fallback) {
        if (!isAvailable()) {
            return fallback;
        }

        try {
            int value = NotFineSettingsHook.downfallDistance();
            return Math.max(MIN_DOWNFALL_DISTANCE, Math.min(MAX_DOWNFALL_DISTANCE, value));
        } catch (LinkageError | RuntimeException e) {
            ModLog.debug("NotFine bridge unavailable: %s", e.toString());
            return fallback;
        }
    }

    /**
     * NotFine's {@code VOID_FOG} toggle. DS applies its own void-fog term inside the biome colour blend; when NotFine
     * has the option off, that term must be skipped as well.
     */
    public static boolean isVoidFogEnabled() {
        if (!isAvailable()) {
            return true;
        }

        try {
            return NotFineSettingsHook.isVoidFogEnabled();
        } catch (LinkageError | RuntimeException e) {
            ModLog.debug("NotFine void fog bridge unavailable: %s", e.toString());
            return true;
        }
    }

    /** NotFine's configured cloud height, or {@code fallback} when NotFine is absent. */
    public static int cloudHeight(int fallback) {
        if (!isAvailable()) {
            return fallback;
        }

        try {
            return NotFineSettingsHook.cloudHeight();
        } catch (LinkageError | RuntimeException e) {
            ModLog.debug("NotFine cloud height bridge unavailable: %s", e.toString());
            return fallback;
        }
    }
}
