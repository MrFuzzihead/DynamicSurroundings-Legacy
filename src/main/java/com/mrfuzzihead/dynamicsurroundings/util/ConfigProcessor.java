package com.mrfuzzihead.dynamicsurroundings.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.apache.commons.lang3.StringUtils;

import com.mrfuzzihead.dynamicsurroundings.ModLog;

public final class ConfigProcessor {

    public static void process(Configuration config, Class<?> clazz) {
        process(config, clazz, null);
    }

    public static void process(Configuration config, Class<?> clazz, Object parameters) {
        for (Field field : clazz.getFields()) {
            ConfigProcessor.Parameter annotation = field.getAnnotation(ConfigProcessor.Parameter.class);
            if (annotation != null) {
                String category = annotation.category();
                String property = annotation.property();
                String comment = field.getAnnotation(ConfigProcessor.Comment.class) != null
                    ? field.getAnnotation(ConfigProcessor.Comment.class)
                        .value()
                    : "";

                try {
                    Object defaultValue = field.get(parameters);
                    if (defaultValue instanceof Boolean) {
                        field.set(
                            parameters,
                            config.getBoolean(property, category, Boolean.valueOf(annotation.defaultValue()), comment));
                    } else if (defaultValue instanceof Integer) {
                        int minInt = Integer.MIN_VALUE;
                        int maxInt = Integer.MAX_VALUE;
                        ConfigProcessor.MinMaxInt mmi = field.getAnnotation(ConfigProcessor.MinMaxInt.class);
                        if (mmi != null) {
                            minInt = mmi.min();
                            maxInt = mmi.max();
                        }

                        field.set(
                            parameters,
                            config.getInt(
                                property,
                                category,
                                Integer.valueOf(annotation.defaultValue()),
                                minInt,
                                maxInt,
                                comment));
                    } else if (defaultValue instanceof Float) {
                        float minFloat = -Float.MAX_VALUE;
                        float maxFloat = Float.MAX_VALUE;
                        ConfigProcessor.MinMaxFloat mmf = field.getAnnotation(ConfigProcessor.MinMaxFloat.class);
                        if (mmf != null) {
                            minFloat = mmf.min();
                            maxFloat = mmf.max();
                        }

                        field.set(
                            parameters,
                            config.getFloat(
                                property,
                                category,
                                Float.valueOf(annotation.defaultValue()),
                                minFloat,
                                maxFloat,
                                comment));
                    } else if (defaultValue instanceof String) {
                        field.set(parameters, config.getString(property, category, annotation.defaultValue(), comment));
                    } else if (defaultValue instanceof String[]) {
                        field.set(
                            parameters,
                            config.getStringList(
                                property,
                                category,
                                StringUtils.split(annotation.defaultValue(), ','),
                                comment));
                    }

                    Property prop = config.getCategory(category)
                        .get(property);
                    if (field.getAnnotation(ConfigProcessor.RestartRequired.class) != null) {
                        ConfigProcessor.RestartRequired restart = field
                            .getAnnotation(ConfigProcessor.RestartRequired.class);
                        prop.setRequiresMcRestart(restart.server());
                        prop.setRequiresWorldRestart(restart.world());
                    } else {
                        prop.setRequiresMcRestart(false);
                        prop.setRequiresWorldRestart(false);
                    }

                    prop.setShowInGui(field.getAnnotation(ConfigProcessor.Hidden.class) == null);
                } catch (Throwable var15) {
                    ModLog.error("Unable to parse configuration", var15);
                }
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface Comment {

        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface Hidden {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface MinMaxFloat {

        float min() default -Float.MAX_VALUE;

        float max() default Float.MAX_VALUE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface MinMaxInt {

        int min() default Integer.MIN_VALUE;

        int max() default Integer.MAX_VALUE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface Parameter {

        String category();

        String property();

        String defaultValue();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface RestartRequired {

        boolean world() default true;

        boolean server() default true;
    }
}
