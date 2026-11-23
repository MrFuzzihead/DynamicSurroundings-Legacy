package com.mrfuzzihead.dynamicsurroundings.testutil;

import java.lang.reflect.Field;

import net.minecraft.entity.player.EntityPlayer;

import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;

/** Installs the client environment statics without exposing test-only setters in production code. */
public final class EnvironStateTestSupport {

    private EnvironStateTestSupport() {}

    public static void setPlayer(EntityPlayer player) {
        set("player", player);
    }

    public static void setTickCounter(int tick) {
        set("tickCounter", tick);
    }

    public static void setDimensionInfo(DimensionRegistry dimensionInfo) {
        set("dimensionInfo", dimensionInfo);
    }

    public static void setDimensionId(int dimensionId) {
        set("dimensionId", dimensionId);
    }

    public static void reset() {
        setPlayer(null);
        setDimensionInfo(null);
        setDimensionId(0);
        setTickCounter(0);
    }

    private static void set(String name, Object value) {
        try {
            Class<?> state = Class
                .forName("com.mrfuzzihead.dynamicsurroundings.client.EnvironStateHandler$EnvironState");
            Field field = state.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to set EnvironState." + name, e);
        }
    }
}
