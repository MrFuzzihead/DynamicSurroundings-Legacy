package com.mrfuzzihead.dynamicsurroundings.data.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import net.minecraft.world.biome.BiomeGenBase;

import org.junit.Test;

import biomesoplenty.client.fog.IBiomeFog;

/**
 * The old BOP integration read {@code fogColor}/{@code fogDensity} fields from a class that does not exist in the
 * BOP 1.7.10 releases. BOP actually exposes those values through the public {@link IBiomeFog} interface; these tests
 * pin the bridge and the no-op fallback used when BOP is absent.
 */
public class BOPFogCompatTest {

    @Test
    public void recognisesBiomesThatImplementTheBopFogApi() {
        BOPFogCompat compat = new BOPFogCompat();

        assertTrue(compat.isFogBiome(fogBiome(0x123456, 0.6F)));
        assertFalse(compat.isFogBiome(mock(BiomeGenBase.class)));
    }

    @Test
    public void readsColourAndDensityFromTheBopApi() {
        BOPFogCompat compat = new BOPFogCompat();
        BiomeGenBase biome = fogBiome(0x123456, 0.6F);

        assertEquals(0x123456, compat.getFogColor(biome));
        assertEquals(0.6F, compat.getFogDensity(biome), 0.0F);
    }

    @Test
    public void integrationUsesTheBridgeWhenBopIsAvailable() {
        IBOPFog integration = BOPFogIntegration.get();
        assertNotNull(integration);
        assertTrue(integration.isFogBiome(fogBiome(0x010203, 0.4F)));
    }

    @Test
    public void noOpFallbackNeverReportsFog() {
        assertFalse(IBOPFog.NONE.isFogBiome(fogBiome(0x123456, 0.6F)));
        assertEquals(0, IBOPFog.NONE.getFogColor(fogBiome(0x123456, 0.6F)));
        assertEquals(0.0F, IBOPFog.NONE.getFogDensity(fogBiome(0x123456, 0.6F)), 0.0F);
    }

    private static BiomeGenBase fogBiome(int color, float density) {
        BiomeGenBase biome = mock(BiomeGenBase.class, withSettings().extraInterfaces(IBiomeFog.class));
        IBiomeFog api = (IBiomeFog) biome;
        when(api.getFogColour(0, 0, 0)).thenReturn(color);
        when(api.getFogDensity(0, 0, 0)).thenReturn(density);
        return biome;
    }
}
