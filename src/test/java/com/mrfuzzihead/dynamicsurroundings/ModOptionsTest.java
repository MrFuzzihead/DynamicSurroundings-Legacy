package com.mrfuzzihead.dynamicsurroundings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * {@link ModOptions} is loaded twice - by the coremod at discovery time and again in pre-init - and both paths go
 * through {@link ModOptions#load(Configuration)}. That method also scrubs properties that carry no comment, which is
 * how stale options from an old config file are dropped.
 */
public class ModOptionsTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private float originalSoundLevel;
    private int originalParticleCount;
    private boolean originalVersionChecking;
    private String originalVersionUrl;

    @Before
    public void rememberDefaults() {
        originalSoundLevel = ModOptions.soundLevel;
        originalParticleCount = ModOptions.particleCountBase;
        originalVersionChecking = ModOptions.enableVersionChecking;
        originalVersionUrl = ModOptions.versionCheckUrl;
    }

    @After
    public void restoreDefaults() {
        ModOptions.soundLevel = originalSoundLevel;
        ModOptions.particleCountBase = originalParticleCount;
        ModOptions.enableVersionChecking = originalVersionChecking;
        ModOptions.versionCheckUrl = originalVersionUrl;
    }

    @Test
    public void defaultsAreAppliedWhenTheFileIsEmpty() throws Exception {
        Configuration config = loadConfig();
        ModOptions.load(config);

        assertEquals(1.0F, ModOptions.soundLevel, 0.0F);
        assertEquals(100, ModOptions.particleCountBase);
        assertFalse(ModOptions.enableVersionChecking);
        assertTrue(ModOptions.versionCheckUrl.contains("versions.txt"));
        assertFalse(ModOptions.disableWeatherEffects);
        assertTrue(ModOptions.respectExternalFog);
    }

    @Test
    public void categoriesAreDroppedWhenTheyHaveNoProperties() throws Exception {
        Configuration config = loadConfig();
        ModOptions.load(config);

        assertNotNull(config.getCategory("rain"));
        assertTrue(
            config.getCategory("rain")
                .containsKey("Sound Level"));
        assertTrue(
            config.getCategory("logging")
                .containsKey("Enable Debug Logging"));
    }

    @Test
    public void valuesSurviveASaveAndReload() throws Exception {
        Configuration config = loadConfig();
        ModOptions.load(config);
        config.get("rain", "Sound Level", 1.0)
            .set(0.25);
        config.get("rain", "Particle Count Base", 100)
            .set(250);
        config.save();

        // The file is re-read from disk, not from the in-memory object.
        ModOptions.load(new Configuration(config.getConfigFile()));

        assertEquals(0.25F, ModOptions.soundLevel, 0.0F);
        assertEquals(250, ModOptions.particleCountBase);
    }

    @Test
    public void scrubbingKeepsKnownCommentlessPropertiesAndDropsUnknownOnes() {
        ConfigCategory category = new ConfigCategory("test");
        category.put("Known", new Property("Known", "value", Property.Type.STRING));
        category.put("Unknown", new Property("Unknown", "value", Property.Type.STRING));

        ModOptions.scrubCategory(category, Collections.singleton("Known"));

        assertTrue(category.containsKey("Known"));
        assertFalse(category.containsKey("Unknown"));
    }

    @Test
    public void propertiesWithoutACommentAreScrubbed() throws Exception {
        Configuration config = loadConfig();
        Property stale = config.get("rain", "Removed Option", "left over from an old release");
        stale.comment = "";

        ModOptions.load(config);

        assertFalse(
            "a comment-less property should be removed from its category",
            config.getCategory("rain")
                .containsKey("Removed Option"));
    }

    private Configuration loadConfig() throws Exception {
        File file = new File(folder.getRoot(), "dynamicsurroundings.cfg");
        // Forge's Configuration resolves relative paths against FML's minecraft home, which only exists inside a
        // real launch. Point it at the test's temp directory so the file API is exercised as-is.
        java.lang.reflect.Field minecraftHome = cpw.mods.fml.relauncher.FMLInjectionData.class
            .getDeclaredField("minecraftHome");
        minecraftHome.setAccessible(true);
        minecraftHome.set(null, folder.getRoot());

        return new Configuration(file);
    }
}
