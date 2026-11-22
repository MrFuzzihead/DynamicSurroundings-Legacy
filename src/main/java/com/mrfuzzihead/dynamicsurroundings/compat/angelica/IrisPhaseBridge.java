package com.mrfuzzihead.dynamicsurroundings.compat.angelica;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.mrfuzzihead.dynamicsurroundings.ModLog;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Optional bridge for Iris/Angelica world render phases.
 *
 * <p>
 * When a shader pack is active, immediate-mode world draws need an explicit {@code WorldRenderingPhase}; otherwise the
 * aurora is shaded by whatever gbuffer pass happened to be current. Iris exposes the phase through
 * {@code Iris.getPipelineManager().getPipelineNullable().setPhase(...)}. The bridge is reflection-only and no-ops
 * without Iris.
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class IrisPhaseBridge {

    private static final String IRIS_CLASS = "net.coderbot.iris.Iris";
    private static final String PHASE_CLASS = "net.coderbot.iris.pipeline.WorldRenderingPhase";
    private static volatile boolean resolved;
    private static volatile boolean available;
    private static volatile Method getPipelineManager;
    private static volatile Method getPipelineNullable;
    private static volatile Method getPhase;
    private static volatile Method setPhase;
    private static volatile Field irisEnabled;
    private static volatile Class<?> phaseClass;

    private IrisPhaseBridge() {}

    public static boolean isAvailable() {
        resolve();
        return available;
    }

    /**
     * Enters {@code phaseName} if Iris is active and returns an opaque scope that must be passed to {@link #pop}.
     * Returns {@code null} when there is nothing to restore.
     */
    public static Object push(String phaseName) {
        resolve();
        if (!available || !isIrisEnabled()) {
            return null;
        }

        try {
            Object manager = getPipelineManager.invoke(null);
            Object pipeline = getPipelineNullable.invoke(manager);
            if (pipeline == null) {
                return null;
            }

            Object previous = getPhase.invoke(pipeline);
            Object phase = enumValue(phaseName);
            if (phase == null) {
                return null;
            }

            setPhase.invoke(pipeline, phase);
            return new Scope(pipeline, previous);
        } catch (ReflectiveOperationException | RuntimeException e) {
            ModLog.debug("Iris phase bridge unavailable: %s", e.toString());
            return null;
        }
    }

    public static void pop(Object scope) {
        if (!(scope instanceof Scope)) {
            return;
        }

        Scope state = (Scope) scope;
        try {
            setPhase.invoke(state.pipeline, state.previous);
        } catch (ReflectiveOperationException | RuntimeException e) {
            ModLog.debug("Iris phase restore failed: %s", e.toString());
        }
    }

    private static boolean isIrisEnabled() {
        if (irisEnabled == null) {
            return false;
        }

        try {
            return irisEnabled.getBoolean(null);
        } catch (IllegalAccessException | RuntimeException e) {
            return false;
        }
    }

    private static Object enumValue(String name) {
        Object[] constants = phaseClass.getEnumConstants();
        if (constants == null) {
            return null;
        }

        for (Object constant : constants) {
            if (((Enum<?>) constant).name()
                .equals(name)) {
                return constant;
            }
        }

        return null;
    }

    private static void resolve() {
        if (resolved) {
            return;
        }

        synchronized (IrisPhaseBridge.class) {
            if (resolved) {
                return;
            }

            try {
                Class<?> iris = Class.forName(IRIS_CLASS, true, IrisPhaseBridge.class.getClassLoader());
                phaseClass = Class.forName(PHASE_CLASS, true, IrisPhaseBridge.class.getClassLoader());
                getPipelineManager = iris.getMethod("getPipelineManager");
                Class<?> manager = getPipelineManager.getReturnType();
                getPipelineNullable = manager.getMethod("getPipelineNullable");
                Class<?> pipeline = getPipelineNullable.getReturnType();
                getPhase = pipeline.getMethod("getPhase");
                setPhase = pipeline.getMethod("setPhase", phaseClass);
                irisEnabled = iris.getField("enabled");
                available = true;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                available = false;
                ModLog.debug("Iris phase bridge unavailable: %s", e.toString());
            }

            resolved = true;
        }
    }

    private static final class Scope {

        private final Object pipeline;
        private final Object previous;

        private Scope(Object pipeline, Object previous) {
            this.pipeline = pipeline;
            this.previous = previous;
        }
    }
}
