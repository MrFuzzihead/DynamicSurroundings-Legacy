package com.mrfuzzihead.dynamicsurroundings.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class JsonUtils {

    public static <T> T load(File file, Class<T> clazz) throws Exception {
        InputStream stream = null;

        try {
            stream = new FileInputStream(file);
            if (stream != null) {
                T loaded = load(stream, clazz);
                return loaded != null ? loaded : clazz.newInstance();
            }
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (Throwable var11) {}
        }

        return clazz.newInstance();
    }

    public static <T> T load(String modId, Class<T> clazz) throws Exception {
        String fileName = modId.replaceAll("[^a-zA-Z0-9.-]", "_");
        InputStream stream = null;

        try {
            stream = clazz.getResourceAsStream("/assets/dynamicsurroundings/data/" + fileName + ".json");
            if (stream != null) {
                T loaded = load(stream, clazz);
                return loaded != null ? loaded : clazz.newInstance();
            }
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (Throwable var12) {}
        }

        return clazz.newInstance();
    }

    public static <T> T load(InputStream stream, Class<T> clazz) {
        if (stream == null) {
            return null;
        }

        InputStreamReader reader = null;
        JsonReader reader2 = null;

        Object var4;
        try {
            reader = new InputStreamReader(stream);
            reader2 = new JsonReader(reader);
            var4 = new Gson().fromJson(reader, clazz);
        } finally {
            try {
                if (reader2 != null) {
                    reader2.close();
                }

                if (reader != null) {
                    reader.close();
                }
            } catch (Exception var11) {}
        }

        return (T) var4;
    }
}
