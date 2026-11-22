package com.mrfuzzihead.dynamicsurroundings.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.annotations.SerializedName;
import com.mrfuzzihead.dynamicsurroundings.data.config.BiomeConfig;
import com.mrfuzzihead.dynamicsurroundings.data.config.BlockConfig;
import com.mrfuzzihead.dynamicsurroundings.data.config.DimensionConfig;

/**
 * Every data file the mod ships is read through {@link JsonUtils}, which wraps Gson and silently falls back to a
 * default instance when the stream is missing. That fallback (and the mod-id to file-name mapping) is what the
 * registries rely on, so it is tested directly.
 */
public class JsonUtilsTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void loadsASimplePojoFromAStream() {
        Fixture fixture = JsonUtils.load(stream("{\"name\":\"hello\",\"count\":7,\"ratio\":1.5}"), Fixture.class);
        assertEquals("hello", fixture.name);
        assertEquals(7, fixture.count);
        assertEquals(1.5F, fixture.ratio, 0.0F);
    }

    @Test
    public void missingFieldsKeepTheirDefaults() {
        Fixture fixture = JsonUtils.load(stream("{\"name\":\"only\"}"), Fixture.class);
        assertEquals("only", fixture.name);
        assertEquals(0, fixture.count);
    }

    @Test
    public void loadsFromAFile() throws Exception {
        File file = folder.newFile("fixture.json");
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("{\"name\":\"from-disk\",\"count\":3}".getBytes("UTF-8"));
        }

        Fixture fixture = JsonUtils.load(file, Fixture.class);
        assertEquals("from-disk", fixture.name);
        assertEquals(3, fixture.count);
    }

    @Test(expected = java.io.FileNotFoundException.class)
    public void missingFileIsReportedRatherThanSilentlyDefaulted() throws Exception {
        // load(File) opens the stream eagerly, so callers see the exception; only the classpath overload falls back
        // to a default instance.
        File missing = new File(folder.getRoot(), "nope.json");
        assertFalse(missing.exists());
        JsonUtils.load(missing, Fixture.class);
    }

    @Test
    public void modIdMapsToTheBundledResourceName() throws Exception {
        DimensionConfig config = JsonUtils.load("dimensions", DimensionConfig.class);
        assertEquals(3, config.entries.size());
        assertEquals(Integer.valueOf(-1), config.entries.get(0).dimensionId);
        assertEquals(Boolean.TRUE, config.entries.get(0).hasWeather);
        assertEquals(Integer.valueOf(128), config.entries.get(1).cloudHeight);
        assertEquals("Erebus", config.entries.get(2).name);
        assertEquals(Integer.valueOf(24), config.entries.get(2).seaLevel);
    }

    @Test
    public void illegalCharactersInAModIdBecomeUnderscores() throws Exception {
        // The shipped file is BiomesOPlenty_blocks.json and the mod id it belongs to is BiomesOPlenty:blocks.
        BlockConfig config = JsonUtils.load("BiomesOPlenty:blocks", BlockConfig.class);
        assertFalse(config.entries.isEmpty());
        assertTrue(config.entries.get(0).blocks.size() > 0);
    }

    @Test
    public void nullStreamLoadsAsNull() {
        assertEquals(null, JsonUtils.load((InputStream) null, Fixture.class));
    }

    @Test
    public void jsonNullFallsBackToTheClassDefault() throws Exception {
        File file = folder.newFile("null.json");
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("null".getBytes("UTF-8"));
        }

        Fixture fixture = JsonUtils.load(file, Fixture.class);
        assertNotNull(fixture);
        assertEquals("default", fixture.name);
    }

    @Test
    public void unknownModIdYieldsAnEmptyConfig() throws Exception {
        DimensionConfig config = JsonUtils.load("this-mod-does-not-exist-4f8a", DimensionConfig.class);
        assertNotNull(config);
        assertTrue(config.entries.isEmpty());
    }

    @Test
    public void bundledBiomeConfigParses() throws Exception {
        BiomeConfig config = BiomeConfig.load("dynamicsurroundings");
        assertFalse(config.entries.isEmpty());

        BiomeConfig.Entry first = config.entries.get(0);
        assertNotNull(first.biomeName);
        assertTrue(first.biomeName.contains("hills"));
        assertFalse(first.sounds.isEmpty());
        assertEquals("dynamicsurroundings:wind", first.sounds.get(0).sound);
        assertEquals(Float.valueOf(0.5F), first.sounds.get(0).volume);
    }

    @Test
    public void bundledBlockConfigParses() throws Exception {
        BlockConfig config = BlockConfig.load("blocks");
        assertFalse(config.entries.isEmpty());

        BlockConfig.Entry first = config.entries.get(0);
        assertTrue(first.blocks.contains("minecraft:dirt"));
        assertFalse(first.effects.isEmpty());
        assertEquals("dust", first.effects.get(0).effect);
        assertEquals(Integer.valueOf(500), first.effects.get(0).chance);
    }

    @Test
    public void dimensionConfigStartsEmpty() throws Exception {
        DimensionConfig config = JsonUtils.load("this-mod-does-not-exist-4f8a", DimensionConfig.class);
        assertNotNull(config.entries);
        assertTrue(config.entries.isEmpty());
    }

    private static InputStream stream(String json) {
        try {
            return new ByteArrayInputStream(json.getBytes("UTF-8"));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /** A minimal Gson target. */
    public static final class Fixture {

        @SerializedName("name")
        public String name = "default";
        @SerializedName("count")
        public int count = 0;
        @SerializedName("ratio")
        public float ratio = 0.0F;
    }
}
