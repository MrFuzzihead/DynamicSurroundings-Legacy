package com.mrfuzzihead.dynamicsurroundings.testutil;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/** Every dynamicsurroundings sound event in sounds.json must point at a bundled ogg. */
public class SoundAssetValidationTest {

    @Test
    public void everySoundEventHasAnAsset() throws Exception {
        InputStream stream = getClass().getResourceAsStream("/assets/dynamicsurroundings/sounds.json");
        assertTrue("sounds.json is missing", stream != null);

        Type type = new TypeToken<Map<String, JsonObject>>() {}.getType();
        Map<String, JsonObject> sounds;
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            sounds = new Gson().fromJson(reader, type);
        }

        for (Map.Entry<String, JsonObject> entry : sounds.entrySet()) {
            JsonArray variants = entry.getValue()
                .getAsJsonArray("sounds");
            for (JsonElement variant : variants) {
                String name = variant.isJsonObject() ? variant.getAsJsonObject()
                    .get("name")
                    .getAsString() : variant.getAsString();
                if (!name.startsWith("dynamicsurroundings:")) {
                    continue;
                }

                String path = name.substring("dynamicsurroundings:".length());
                String resource = "/assets/dynamicsurroundings/sounds/" + path + ".ogg";
                assertTrue(
                    "missing sound asset " + resource + " for event " + entry.getKey(),
                    getClass().getResourceAsStream(resource) != null);
            }
        }
    }
}
