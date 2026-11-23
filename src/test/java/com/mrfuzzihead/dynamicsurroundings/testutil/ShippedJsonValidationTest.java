package com.mrfuzzihead.dynamicsurroundings.testutil;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Test;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/** Guards the shipped resource pack against malformed JSON. */
public class ShippedJsonValidationTest {

    @Test
    public void everyShippedJsonResourceParses() throws IOException, URISyntaxException {
        URL root = ShippedJsonValidationTest.class.getResource("/assets/dynamicsurroundings");
        assertNotNull("missing shipped assets", root);
        List<String> malformed = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(java.nio.file.Paths.get(root.toURI()))) {
            paths.filter(Files::isRegularFile)
                .filter(
                    path -> path.getFileName()
                        .toString()
                        .endsWith(".json"))
                .forEach(path -> {
                    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                        new JsonParser().parse(reader);
                    } catch (JsonSyntaxException | IOException e) {
                        malformed.add(path + ": " + e.getMessage());
                    }
                });
        }

        if (!malformed.isEmpty()) {
            throw new AssertionError("malformed shipped JSON:\n" + String.join("\n", malformed));
        }
    }

    @Test
    public void everyShippedJsonResourceHasUniqueKeys() throws IOException, URISyntaxException {
        URL root = ShippedJsonValidationTest.class.getResource("/assets/dynamicsurroundings");
        assertNotNull("missing shipped assets", root);
        List<String> duplicates = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(java.nio.file.Paths.get(root.toURI()))) {
            paths.filter(Files::isRegularFile)
                .filter(
                    path -> path.getFileName()
                        .toString()
                        .endsWith(".json"))
                .forEach(path -> {
                    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                        readValue(new JsonReader(reader), path.toString());
                    } catch (JsonSyntaxException | IOException e) {
                        duplicates.add(path + ": " + e.getMessage());
                    }
                });
        }

        if (!duplicates.isEmpty()) {
            throw new AssertionError("duplicate keys in shipped JSON:\n" + String.join("\n", duplicates));
        }
    }

    private static void readValue(JsonReader reader, String path) throws IOException {
        JsonToken token = reader.peek();
        if (token == JsonToken.BEGIN_OBJECT) {
            reader.beginObject();
            Set<String> names = new HashSet<>();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (!names.add(name)) {
                    throw new AssertionError("duplicate key '" + name + "' in " + path);
                }
                readValue(reader, path + "." + name);
            }
            reader.endObject();
        } else if (token == JsonToken.BEGIN_ARRAY) {
            reader.beginArray();
            while (reader.hasNext()) {
                readValue(reader, path);
            }
            reader.endArray();
        } else {
            reader.skipValue();
        }
    }
}
