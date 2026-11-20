package com.mrfuzzihead.dynamicsurroundings.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.minecraft.world.biome.BiomeGenBase;

import org.junit.Test;

public class BiomeRegistryTest {

    @Test
    public void nullBiomeResolvesToThePlaceholderName() {
        assertEquals("(Bad Biome)", BiomeRegistry.resolveName(null));
    }

    @Test
    public void unnamedBiomesFallBackToTheNumericId() {
        BiomeGenBase unnamed = new BiomeGenBase(200) {

            {
                this.setBiomeName("");
            }
        };
        assertEquals("#200", BiomeRegistry.resolveName(unnamed));
    }

    @Test
    public void namedBiomesUseTheirName() {
        BiomeGenBase named = new BiomeGenBase(201) {

            {
                this.setBiomeName("Test Biomes");
            }
        };
        assertEquals("Test Biomes", BiomeRegistry.resolveName(named));
    }

    @Test
    public void pseudoBiomesAreRegisteredWithNegativeIds() {
        BiomeGenBase[] pseudoBiomes = { BiomeRegistry.UNDERGROUND, BiomeRegistry.PLAYER, BiomeRegistry.UNDERWATER,
            BiomeRegistry.UNDEROCEAN, BiomeRegistry.UNDERDEEPOCEAN, BiomeRegistry.UNDERRIVER, BiomeRegistry.OUTERSPACE,
            BiomeRegistry.CLOUDS };
        for (BiomeGenBase biome : pseudoBiomes) {
            assertNotNull(biome);
            assertTrue("id " + biome.biomeID, biome.biomeID < 0);
            assertNotNull("name of " + biome.biomeID, biome.biomeName);
        }
    }

    @Test
    public void pseudoBiomesNeverSpawnWeather() {
        assertTrue(!BiomeRegistry.UNDERGROUND.canSpawnLightningBolt());
        assertTrue(!BiomeRegistry.OUTERSPACE.getEnableSnow());
    }

    @Test
    public void registryLookupsAreSafeForUnregisteredBiomes() {
        // hasDust/hasAurora read from the JSON-populated registry and must default to false for a biome the scan
        // never saw rather than throwing.
        BiomeGenBase unknown = new BiomeGenBase(202) {

            {
                this.setBiomeName("Never Configured");
            }
        };
        assertTrue(!BiomeRegistry.hasDust(unknown));
        assertTrue(!BiomeRegistry.hasAurora(unknown));
        assertTrue(!BiomeRegistry.hasFog(unknown));
        assertEquals(0.0F, BiomeRegistry.getFogDensity(unknown), 0.0F);
    }

    @Test
    public void scanningTheBiomeArrayRegistersTheVanillaBiomes() {
        // The first lookup for an unknown biome triggers the full JSON-driven rescan.
        BiomeRegistry.initialize();
        assertTrue(BiomeRegistry.hasPrecipitation(BiomeGenBase.plains));
        assertTrue(BiomeRegistry.getFogDensity(BiomeGenBase.plains) >= 0.0F);
        // A vanilla ocean still resolves even though the config only carries regexes for modded biomes.
        assertEquals("Ocean", BiomeRegistry.resolveName(BiomeGenBase.ocean));
    }
}
