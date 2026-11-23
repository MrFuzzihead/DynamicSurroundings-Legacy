package com.mrfuzzihead.dynamicsurroundings.client.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Property;

import org.junit.Test;

public class LazyConfigElementTest {

    @Test
    public void childrenAreGeneratedOnlyWhenOpened() {
        AtomicInteger generated = new AtomicInteger();
        LazyConfigElement element = new LazyConfigElement("Sounds", "Sounds", () -> {
            generated.incrementAndGet();
            ConfigCategory category = new ConfigCategory("Sounds");
            category.put("one", new Property("one", "false", Property.Type.BOOLEAN));
            return category;
        });

        assertEquals(0, generated.get());
        assertFalse(element.isBuilt());

        assertEquals(
            1,
            element.getChildElements()
                .size());
        assertEquals(1, generated.get());
        assertTrue(element.isBuilt());
        assertEquals(
            1,
            element.getChildElements()
                .size());
        assertEquals(1, generated.get());
    }
}
