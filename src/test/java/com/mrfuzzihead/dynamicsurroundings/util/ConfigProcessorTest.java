package com.mrfuzzihead.dynamicsurroundings.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

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
    public void invalidEnumValuesAreReportedAndKeepTheDefault() {
        Configuration config = new Configuration();
        config.get("types", "mode", "ALPHA")
            .set("GAMMA");
        EnumOptions.mode = EnumOptions.Mode.ALPHA;

        ConfigProcessor.process(config, EnumOptions.class);

        assertEquals(EnumOptions.Mode.ALPHA, EnumOptions.mode);
        assertTrue(
            ConfigProcessor.getLastErrors()
                .stream()
                .anyMatch(error -> error.contains("types.mode")));
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

    @Test
    public void annotatedBoundsAreClamped() {
        Configuration config = new Configuration();
        config.get("bounds", "count", "3")
            .set(99);
        config.get("bounds", "scale", "1.0")
            .set(-99.0F);

        ConfigProcessor.process(config, BoundsOptions.class);

        assertEquals(5, BoundsOptions.count);
        assertEquals(0.5F, BoundsOptions.scale, 0.0F);
    }

    @Test
    public void restartAndHiddenFlagsAreApplied() {
        Configuration config = new Configuration();

        ConfigProcessor.process(config, BoundsOptions.class);

        Property restart = config.getCategory("bounds")
            .get("restart");
        assertTrue(restart.requiresMcRestart());
        assertFalse(restart.requiresWorldRestart());
        Property visible = config.getCategory("bounds")
            .get("visible");
        assertFalse(visible.requiresMcRestart());
        assertFalse(visible.requiresWorldRestart());
        assertTrue(visible.showInGui());
        assertFalse(
            config.getCategory("bounds")
                .get("hidden")
                .showInGui());
    }

    @Test
    public void stringListDefaultsAreSplitOnCommas() {
        Configuration config = new Configuration();

        ConfigProcessor.process(config, BoundsOptions.class);

        assertArrayEquals(new String[] { "alpha", "beta" }, BoundsOptions.names);
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

    public static final class BoundsOptions {

        @ConfigProcessor.Parameter(category = "bounds", property = "count", defaultValue = "3")
        @ConfigProcessor.MinMaxInt(min = 1, max = 5)
        public static int count = 3;

        @ConfigProcessor.Parameter(category = "bounds", property = "scale", defaultValue = "1")
        @ConfigProcessor.MinMaxFloat(min = 0.5F, max = 2.0F)
        public static float scale = 1.0F;

        @ConfigProcessor.Parameter(category = "bounds", property = "restart", defaultValue = "1")
        @ConfigProcessor.RestartRequired(world = false)
        public static int restart = 1;

        @ConfigProcessor.Parameter(category = "bounds", property = "visible", defaultValue = "1")
        public static int visible = 1;

        @ConfigProcessor.Parameter(category = "bounds", property = "hidden", defaultValue = "1")
        @ConfigProcessor.Hidden
        public static int hidden = 1;

        @ConfigProcessor.Parameter(category = "bounds", property = "names", defaultValue = "alpha,beta")
        public static String[] names = new String[0];
    }

    public static final class TestOptions {

        @ConfigProcessor.Parameter(category = "test", property = "value", defaultValue = "0")
        public static float value = 0.0F;
    }
}
