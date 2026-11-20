package org.blockartistry.mod.DynSurround.client.footsteps.mcpackage.implem;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public enum Substrate {

    CARPET("carpet"),
    FOLIAGE("foliage"),
    MESSY("messy"),
    FENCE("bigger");

    private static final Map<String, Substrate> lookup = new HashMap<>();
    private final String name;

    private Substrate(@Nonnull String name) {
        this.name = name;
    }

    @Nullable
    public static Substrate get(@Nonnull String name) {
        return lookup.get(name);
    }

    static {
        for (Substrate s : values()) {
            lookup.put(s.name, s);
        }
    }
}
