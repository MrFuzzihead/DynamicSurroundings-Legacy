package com.mrfuzzihead.dynamicsurroundings.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;

/**
 * Declarative category metadata for {@code ModOptions}; keeps comments, order and unknown-option reporting together.
 */
public final class ConfigSchema {

    private final List<Category> categories = new ArrayList<>();

    public ConfigSchema category(String name, String comment, String... order) {
        this.categories.add(new Category(name, comment, Arrays.asList(order)));
        return this;
    }

    public void apply(Configuration config) {
        for (Category category : this.categories) {
            config.setCategoryRequiresMcRestart(category.name, false);
            config.setCategoryRequiresWorldRestart(category.name, false);
            config.setCategoryComment(category.name, category.comment);
            if (!category.order.isEmpty()) {
                config.setCategoryPropertyOrder(category.name, new ArrayList<>(category.order));
            }
        }
    }

    public Map<String, Set<String>> knownProperties(Class<?> owner) {
        Map<String, Set<String>> known = new HashMap<>();
        for (java.lang.reflect.Field field : owner.getFields()) {
            ConfigProcessor.Parameter annotation = field.getAnnotation(ConfigProcessor.Parameter.class);
            if (annotation != null) {
                known.computeIfAbsent(annotation.category(), key -> new HashSet<>())
                    .add(annotation.property());
            }
        }
        return known;
    }

    public List<String> unknownProperties(Configuration config, Class<?> owner) {
        Map<String, Set<String>> known = knownProperties(owner);
        List<String> unknown = new ArrayList<>();
        for (String category : config.getCategoryNames()) {
            Set<String> expected = known.get(category);
            if (expected == null) {
                continue;
            }
            ConfigCategory actual = config.getCategory(category);
            for (String property : actual.keySet()) {
                if (!expected.contains(property)) {
                    unknown.add(category + "." + property);
                }
            }
        }
        return unknown;
    }

    public List<String> getCategoryNames() {
        List<String> names = new ArrayList<>();
        for (Category category : this.categories) {
            names.add(category.name);
        }
        return Collections.unmodifiableList(names);
    }

    private static final class Category {

        private final String name;
        private final String comment;
        private final List<String> order;

        private Category(String name, String comment, List<String> order) {
            this.name = name;
            this.comment = comment;
            this.order = order;
        }
    }
}
