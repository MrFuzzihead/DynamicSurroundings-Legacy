package com.mrfuzzihead.dynamicsurroundings.util;

import static org.junit.Assert.assertEquals;

import net.minecraftforge.common.config.Configuration;

import org.junit.Test;

public class ConfigProcessorTest {

    @Test
    public void enumOptionsAreSupported() {
        Configuration config = new Configuration();
        config.get("types", "mode", "ALPHA")
            .set("BETA");

        ConfigProcessor.process(config, EnumOptions.class);

        assertEquals(EnumOptions.Mode.BETA, EnumOptions.mode);
    }

    @Test
    public void doubleAndLongOptionsAreSupported() {
        Configuration config = new Configuration();
        config.get("types", "double", "1.5")
            .set(2.5);
        config.get("types", "long", "1")
            .set("42");

        ConfigProcessor.process(config, TypeOptions.class);

        assertEquals(2.5D, TypeOptions.doubleValue, 0.0D);
        assertEquals(42L, TypeOptions.longValue);
    }

    @Test
    public void unannotatedFloatOptionsAreNotClampedToTheSmallestPositiveFloat() {
        Configuration config = new Configuration();
        config.get("test", "value", 0.0F)
            .set(-1.5F);

        ConfigProcessor.process(config, TestOptions.class);

        assertEquals(-1.5F, TestOptions.value, 0.0F);
    }

    public static final class EnumOptions {

        public enum Mode {
            ALPHA,
            BETA
        }

        @ConfigProcessor.Parameter(category = "types", property = "mode", defaultValue = "ALPHA")
        public static Mode mode = Mode.ALPHA;
    }

    public static final class TypeOptions {

        @ConfigProcessor.Parameter(category = "types", property = "double", defaultValue = "1.5")
        public static double doubleValue = 1.5D;

        @ConfigProcessor.Parameter(category = "types", property = "long", defaultValue = "1")
        public static long longValue = 1L;
    }

    public static final class TestOptions {

        @ConfigProcessor.Parameter(category = "test", property = "value", defaultValue = "0")
        public static float value = 0.0F;
    }
}
