package com.mrfuzzihead.dynamicsurroundings.compat.notfine;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.compat.Compat;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Guarded, reflection-only bridge to NotFine's video settings. NotFine is bundled with modern Angelica builds but is
 * also shipped as a standalone mod, so this is gated by mod id instead of by Angelica.
 */
@SideOnly(Side.CLIENT)
public final class NotFineBridge {

    private static final String SETTINGS_MANAGER_CLASS = "jss.notfine.core.SettingsManager";
    private static final String SETTINGS_CLASS = "jss.notfine.core.Settings";
    private static final String DOWNFALL_DISTANCE_FIELD = "downfallDistance";
    private static final int MIN_DOWNFALL_DISTANCE = 0;
    private static final int MAX_DOWNFALL_DISTANCE = 15;

    private static volatile boolean resolved;
    private static volatile boolean available;
    private static volatile Field downfallDistanceField;
    private static volatile boolean settingsResolved;
    private static volatile boolean settingsAvailable;
    private static volatile Class<?> settingsClass;
    private static volatile Field optionField;
    private static volatile Method getStoreMethod;

    private NotFineBridge() {}

    public static boolean isAvailable() {
        resolve();
        return available;
    }

    /**
     * Returns NotFine's configured weather radius (0 disables downfall rendering) or {@code fallback} when NotFine is
     * absent. The value is clamped to the range NotFine itself uses so a stale/corrupt config cannot make the
     * renderer allocate an oversized scan.
     */
    public static int downfallDistance(int fallback) {
        resolve();
        if (!available || downfallDistanceField == null) {
            return fallback;
        }

        try {
            int value = downfallDistanceField.getByte(null);
            return Math.max(MIN_DOWNFALL_DISTANCE, Math.min(MAX_DOWNFALL_DISTANCE, value));
        } catch (ReflectiveOperationException | RuntimeException e) {
            ModLog.debug("NotFine bridge unavailable: %s", e.toString());
            return fallback;
        }
    }

    /**
     * NotFine's {@code VOID_FOG} toggle. DS applies its own void-fog term inside the biome colour blend; when NotFine
     * has the option off, that term must be skipped as well. The fallback preserves DS's pre-existing behaviour when
     * NotFine is absent.
     */
    public static boolean isVoidFogEnabled() {
        return booleanSetting("VOID_FOG", true);
    }

    private static boolean booleanSetting(String name, boolean fallback) {
        resolveSettings();
        if (!settingsAvailable || settingsClass == null || optionField == null || getStoreMethod == null) {
            return fallback;
        }

        try {
            for (Object constant : settingsClass.getEnumConstants()) {
                if (!((Enum<?>) constant).name()
                    .equals(name)) {
                    continue;
                }

                Object option = optionField.get(constant);
                Object value = getStoreMethod.invoke(option);
                return value instanceof Boolean ? (Boolean) value : fallback;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            ModLog.debug("NotFine setting %s unavailable: %s", name, e.toString());
        }

        return fallback;
    }

    private static void resolve() {
        if (resolved) {
            return;
        }

        synchronized (NotFineBridge.class) {
            if (resolved) {
                return;
            }

            available = Compat.isNotFineLoaded();
            if (available) {
                try {
                    Class<?> settings = Class
                        .forName(SETTINGS_MANAGER_CLASS, true, NotFineBridge.class.getClassLoader());
                    Field field = settings.getDeclaredField(DOWNFALL_DISTANCE_FIELD);
                    field.setAccessible(true);
                    downfallDistanceField = field;
                } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                    available = false;
                    ModLog.debug("NotFine bridge unavailable: %s", e.toString());
                }
            }

            resolved = true;
        }
    }

    private static void resolveSettings() {
        if (settingsResolved) {
            return;
        }

        synchronized (NotFineBridge.class) {
            if (settingsResolved) {
                return;
            }

            settingsAvailable = Compat.isNotFineLoaded();
            if (settingsAvailable) {
                try {
                    Class<?> settings = Class.forName(SETTINGS_CLASS, true, NotFineBridge.class.getClassLoader());
                    Field field = settings.getField("option");
                    settingsClass = settings;
                    optionField = field;
                    getStoreMethod = field.getType()
                        .getMethod("getStore");
                } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                    settingsAvailable = false;
                    ModLog.debug("NotFine settings bridge unavailable: %s", e.toString());
                }
            }

            settingsResolved = true;
        }
    }
}
