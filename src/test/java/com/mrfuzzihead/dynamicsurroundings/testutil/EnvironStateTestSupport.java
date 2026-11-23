package com.mrfuzzihead.dynamicsurroundings.testutil;

import java.lang.reflect.Field;

import net.minecraft.entity.player.EntityPlayer;

import com.mrfuzzihead.dynamicsurroundings.client.EnvironmentSnapshot;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;

/** Installs the client environment statics without exposing test-only setters in production code. */
public final class EnvironStateTestSupport {

    private EnvironStateTestSupport() {}

    public static void setPlayer(EntityPlayer player) {
        setSnapshot(
            current().toBuilder()
                .player(player)
                .build());
    }

    public static void setTickCounter(int tick) {
        setSnapshot(
            current().toBuilder()
                .tickCounter(tick)
                .build());
    }

    public static void setDimensionInfo(DimensionRegistry dimensionInfo) {
        setSnapshot(
            current().toBuilder()
                .dimensionInfo(dimensionInfo)
                .build());
    }

    public static void setDimensionId(int dimensionId) {
        setSnapshot(
            current().toBuilder()
                .dimensionId(dimensionId)
                .build());
    }

    public static void reset() {
        setSnapshot(EnvironmentSnapshot.EMPTY);
    }

    private static EnvironmentSnapshot current() {
        try {
            Class<?> state = Class
                .forName("com.mrfuzzihead.dynamicsurroundings.client.EnvironStateHandler$EnvironState");
            Field field = state.getDeclaredField("snapshot");
            field.setAccessible(true);
            return (EnvironmentSnapshot) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to read EnvironState.snapshot", e);
        }
    }

    private static void setSnapshot(EnvironmentSnapshot snapshot) {
        try {
            Class<?> state = Class
                .forName("com.mrfuzzihead.dynamicsurroundings.client.EnvironStateHandler$EnvironState");
            Field field = state.getDeclaredField("snapshot");
            field.setAccessible(true);
            field.set(null, snapshot);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to set EnvironState.snapshot", e);
        }
    }
}
