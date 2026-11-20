package com.mrfuzzihead.dynamicsurroundings.client.sound.fix;

import java.lang.reflect.Field;

import cpw.mods.fml.relauncher.ReflectionHelper;
import paulscode.sound.Source;

public class SoundFixMethods {

    public static final Field removed = ReflectionHelper.findField(Source.class, new String[] { "removed" });

    private SoundFixMethods() {}

    public static Source removeSource(Source source) {
        try {
            if (removed.getBoolean(source)) {
                source.cleanup();
                return null;
            }
        } catch (IllegalArgumentException var2) {
            var2.printStackTrace();
        } catch (IllegalAccessException var3) {
            var3.printStackTrace();
        }

        return source;
    }

    public static void cleanupSource(Source source) {
        if (source.toStream) {
            try {
                removed.setBoolean(source, true);
            } catch (IllegalArgumentException var2) {
                var2.printStackTrace();
            } catch (IllegalAccessException var3) {
                var3.printStackTrace();
            }
        } else {
            source.cleanup();
        }
    }
}
