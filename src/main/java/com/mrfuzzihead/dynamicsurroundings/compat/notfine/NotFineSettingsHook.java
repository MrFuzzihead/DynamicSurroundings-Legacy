package com.mrfuzzihead.dynamicsurroundings.compat.notfine;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import jss.notfine.core.Settings;
import jss.notfine.core.SettingsManager;

/**
 * Direct references to NotFine's settings. Loaded only after NotFine's mod id is observed; {@link NotFineBridge} is
 * the guarded entry point.
 */
@SideOnly(Side.CLIENT)
final class NotFineSettingsHook {

    private NotFineSettingsHook() {}

    static int downfallDistance() {
        return SettingsManager.downfallDistance;
    }

    static boolean isVoidFogEnabled() {
        Object value = Settings.VOID_FOG.option.getStore();
        return !(value instanceof Boolean) || (Boolean) value;
    }

    static int cloudHeight() {
        Object value = Settings.CLOUD_HEIGHT.option.getStore();
        return value instanceof Number ? ((Number) value).intValue() : 128;
    }
}
