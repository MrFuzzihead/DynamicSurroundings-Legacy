package com.mrfuzzihead.dynamicsurroundings.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mrfuzzihead.dynamicsurroundings.ModLog;

/**
 * Single reload entry point for the JSON-driven registries.
 *
 * <p>
 * The registries still expose their static {@code initialize()} methods for compatibility, but the callers no longer
 * need to know the order or the set of registries. A failed registry is logged and the remaining registries still
 * reload, which keeps a malformed optional-mod file from taking down the whole client reload.
 * </p>
 */
public final class RegistryManager {

    public interface Reloadable {

        void reload();
    }

    private static final Map<String, Reloadable> REGISTRIES = new LinkedHashMap<>();

    static {
        register("biomes", BiomeRegistry::initialize);
        register("dimensions", DimensionRegistry::initialize);
        register("sounds", SoundRegistry::initialize);
    }

    private RegistryManager() {}

    public static synchronized void register(String id, Reloadable reloadable) {
        REGISTRIES.put(id, reloadable);
    }

    public static synchronized List<String> ids() {
        return new ArrayList<>(REGISTRIES.keySet());
    }

    public static void reloadAll() {
        reload(ids().toArray(new String[0]));
    }

    public static void reload(String... ids) {
        Map<String, Reloadable> snapshot;
        synchronized (RegistryManager.class) {
            snapshot = new LinkedHashMap<>(REGISTRIES);
        }

        for (String id : ids) {
            Reloadable reloadable = snapshot.get(id);
            if (reloadable == null) {
                ModLog.warn("Unknown registry reload request: %s", id);
                continue;
            }

            try {
                reloadable.reload();
            } catch (Throwable t) {
                ModLog.error("Unable to reload registry " + id, t);
            }
        }
    }
}
