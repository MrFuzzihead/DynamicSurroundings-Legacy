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
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

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
}
