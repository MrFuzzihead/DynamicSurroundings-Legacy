package com.mrfuzzihead.dynamicsurroundings.testutil;

import java.lang.reflect.Field;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;

/**
 * {@code World.provider} is a {@code public final} field in vanilla. The production code reads it directly (there is
 * no getter in 1.7.10), so tests that mock a {@link World} have to install the provider on the mock. The old access
 * transformer made the field non-final just so tests could assign it; now the test does it explicitly.
 */
public final class WorldTestSupport {

    private WorldTestSupport() {}

    public static void setProvider(World world, WorldProvider provider) {
        try {
            Field field = World.class.getDeclaredField("provider");
            field.setAccessible(true);
            field.set(world, provider);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to install the world provider on the test mock", e);
        }
    }
}
