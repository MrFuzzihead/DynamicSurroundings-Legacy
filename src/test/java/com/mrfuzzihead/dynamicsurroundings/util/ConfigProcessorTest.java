package com.mrfuzzihead.dynamicsurroundings.util;

import static org.junit.Assert.assertEquals;

import net.minecraftforge.common.config.Configuration;

import org.junit.Test;

public class ConfigProcessorTest {

    @Test
    public void unannotatedFloatOptionsAreNotClampedToTheSmallestPositiveFloat() {
        Configuration config = new Configuration();
        config.get("test", "value", 0.0F)
            .set(-1.5F);

        ConfigProcessor.process(config, TestOptions.class);

        assertEquals(-1.5F, TestOptions.value, 0.0F);
    }

    public static final class TestOptions {

        @ConfigProcessor.Parameter(category = "test", property = "value", defaultValue = "0")
        public static float value = 0.0F;
    }
}
