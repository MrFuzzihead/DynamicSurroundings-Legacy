package com.mrfuzzihead.dynamicsurroundings.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;

import net.minecraftforge.common.config.Configuration;

import org.junit.Test;

public class ConfigSchemaTest {

    @Test
    public void schemaAppliesCategoryMetadataAndFindsUnknownOptions() {
        ConfigSchema schema = new ConfigSchema().category("test", "Test category", "Known");
        Configuration config = new Configuration();
        config.get("test", "Known", "value");
        config.get("test", "Removed", "value");

        schema.apply(config);
        List<String> unknown = schema.unknownProperties(config, Options.class);

        assertEquals(
            "Test category",
            config.getCategory("test")
                .getComment());
        assertFalse(
            config.getCategory("test")
                .requiresMcRestart());
        assertFalse(
            config.getCategory("test")
                .requiresWorldRestart());
        assertEquals(1, unknown.size());
        assertEquals("test.Removed", unknown.get(0));
    }

    public static final class Options {

        @ConfigProcessor.Parameter(category = "test", property = "Known", defaultValue = "value")
        public static String known = "value";
    }
}
