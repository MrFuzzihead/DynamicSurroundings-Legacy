package com.mrfuzzihead.dynamicsurroundings.client.weather;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.client.weather.WeatherBackend.Owner;

/**
 * Pins the single-owner matrix. The previous code let DS and Angelica both cancel the same vanilla method; the order
 * of two HEAD injectors decided whether the player saw DS weather, Angelica weather, both, or neither. These cases
 * are the contract the mixin now relies on.
 */
public class WeatherBackendTest {

    @Test
    public void angelicaInstancedWeatherWinsOverEverythingElse() {
        assertEquals(Owner.ANGELICA_INSTANCED, WeatherBackend.resolve(true, true, true, true, 10));
        assertEquals(Owner.ANGELICA_INSTANCED, WeatherBackend.resolve(false, false, true, false, 0));
    }

    @Test
    public void externalProviderOwnsTheFrameWhenAngelicaIsNotInstanced() {
        assertEquals(Owner.EXTERNAL_PROVIDER, WeatherBackend.resolve(true, true, false, true, 10));
        assertEquals(Owner.EXTERNAL_PROVIDER, WeatherBackend.resolve(false, false, false, true, 0));
    }

    @Test
    public void externalWeatherModsOwnTheFrameUnlessAngelicaIsInstanced() {
        assertEquals(Owner.EXTERNAL_WEATHER_MOD, WeatherBackend.resolve(true, true, false, false, 10, true));
        assertEquals(Owner.EXTERNAL_WEATHER_MOD, WeatherBackend.resolve(false, false, false, false, 0, true));
        assertEquals(Owner.ANGELICA_INSTANCED, WeatherBackend.resolve(true, true, true, false, 10, true));
    }

    @Test
    public void dynamicSurroundingsOwnsTheFrameWhenItIsTheOnlyCandidate() {
        assertEquals(Owner.DYNAMIC_SURROUNDINGS, WeatherBackend.resolve(true, true, false, false, 10));
        assertEquals(Owner.DYNAMIC_SURROUNDINGS, WeatherBackend.resolve(true, true, false, false, 15));
    }

    @Test
    public void vanillaOwnsTheFrameWhenDsIsDisabledOrHasNoWeather() {
        assertEquals(Owner.VANILLA, WeatherBackend.resolve(false, true, false, false, 10));
        assertEquals(Owner.VANILLA, WeatherBackend.resolve(true, false, false, false, 10));
        assertEquals(Owner.VANILLA, WeatherBackend.resolve(true, true, false, false, 0));
        assertEquals(Owner.VANILLA, WeatherBackend.resolve(false, false, false, false, 0));
    }
}
