package com.mrfuzzihead.dynamicsurroundings.compat.angelica;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.compat.Compat;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Guarded entry point for Iris world-render phases. The direct Iris references live in {@link IrisPhaseHook}; this
 * class only probes for the Iris class, so it remains loadable when Iris is absent.
 */
@SideOnly(Side.CLIENT)
public final class IrisPhaseBridge {

    private static final String IRIS_CLASS = "net.coderbot.iris.Iris";
    private static volatile boolean resolved;
    private static volatile boolean available;

    private IrisPhaseBridge() {}

    public static boolean isAvailable() {
        resolve();
        return available;
    }

    public static Object push(String phaseName) {
        if (!isAvailable()) {
            return null;
        }

        try {
            return IrisPhaseHook.push(phaseName);
        } catch (LinkageError | RuntimeException e) {
            ModLog.debug("Iris phase bridge unavailable: %s", e.toString());
            return null;
        }
    }

    public static boolean isShaderPipelineActive() {
        if (!isAvailable()) {
            return false;
        }

        try {
            return IrisPhaseHook.isShaderPipelineActive();
        } catch (LinkageError | RuntimeException e) {
            ModLog.debug("Iris pipeline probe unavailable: %s", e.toString());
            return false;
        }
    }

    public static void pop(Object scope) {
        if (scope == null || !isAvailable()) {
            return;
        }

        try {
            IrisPhaseHook.pop(scope);
        } catch (LinkageError | RuntimeException e) {
            ModLog.debug("Iris phase restore failed: %s", e.toString());
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }

        synchronized (IrisPhaseBridge.class) {
            if (resolved) {
                return;
            }

            if (!Compat.isAngelicaLoaded() && !Compat.isIrisLoaded()) {
                available = false;
                resolved = true;
                return;
            }

            try {
                Class.forName(IRIS_CLASS, false, IrisPhaseBridge.class.getClassLoader());
                available = true;
            } catch (ClassNotFoundException | LinkageError | RuntimeException e) {
                available = false;
            }

            resolved = true;
        }
    }
}
