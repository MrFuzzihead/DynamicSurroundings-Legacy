package com.mrfuzzihead.dynamicsurroundings.testutil;

import java.lang.reflect.Field;

import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;

/** Test-only helpers for vanilla fields that have no setter in 1.7.10. */
public final class EntityTestSupport {

    private EntityTestSupport() {}

    public static void setBoundingBox(Entity entity, AxisAlignedBB box) {
        try {
            Field field = Entity.class.getDeclaredField("boundingBox");
            field.setAccessible(true);
            field.set(entity, box);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to install the entity bounding box", e);
        }
    }

    public static float getFloat(Object target, String name) {
        try {
            Field field = target.getClass()
                .getSuperclass()
                .getDeclaredField(name);
            field.setAccessible(true);
            return field.getFloat(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to read field " + name, e);
        }
    }
}
