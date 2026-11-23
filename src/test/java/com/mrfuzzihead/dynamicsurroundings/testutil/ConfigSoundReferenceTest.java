package com.mrfuzzihead.dynamicsurroundings.testutil;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mrfuzzihead.dynamicsurroundings.data.config.BiomeConfig;
import com.mrfuzzihead.dynamicsurroundings.data.config.BlockConfig;
import com.mrfuzzihead.dynamicsurroundings.data.config.SoundConfig;

/** Every dynamicsurroundings sound referenced by biome/block JSON must be a sounds.json event. */
public class ConfigSoundReferenceTest {

    @Test
    public void biomeAndBlockSoundsResolveToEvents() throws Exception {
        Set<String> events = soundEvents();
        BiomeConfig biomes = BiomeConfig.load("dynamicsurroundings");
        for (BiomeConfig.Entry entry : biomes.entries) {
            for (SoundConfig sound : entry.sounds) {
                assertResolved(events, sound.sound);
            }
        }

        BlockConfig blocks = BlockConfig.load("blocks");
        for (BlockConfig.Entry entry : blocks.entries) {
            for (SoundConfig sound : entry.sounds) {
                assertResolved(events, sound.sound);
            }
        }
    }

    private static void assertResolved(Set<String> events, String sound) {
        if (sound == null || !sound.startsWith("dynamicsurroundings:")) {
            return;
        }

        String key = sound.substring("dynamicsurroundings:".length());
        assertTrue("missing sounds.json event " + sound, events.contains(key));
    }

    private static Set<String> soundEvents() throws Exception {
        InputStream stream = ConfigSoundReferenceTest.class
            .getResourceAsStream("/assets/dynamicsurroundings/sounds.json");
        Type type = new TypeToken<Map<String, JsonObject>>() {}.getType();
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Map<String, JsonObject> parsed = new Gson().fromJson(reader, type);
            return new HashSet<>(parsed.keySet());
        }
    }
}
