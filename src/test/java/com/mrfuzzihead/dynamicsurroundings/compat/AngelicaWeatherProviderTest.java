package com.mrfuzzihead.dynamicsurroundings.compat;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.compat.angelica.AngelicaWeatherProvider;

/**
 * The provider is only installed when Angelica's API is present. Without Angelica this class must stay inert; the
 * legacy weather path is selected by {@code WeatherBackend}.
 */
public class AngelicaWeatherProviderTest {

    @Test
    public void absentAngelicaLeavesTheProviderUnset() {
        AngelicaWeatherProvider.install(null);
        assertFalse(AngelicaWeatherProvider.isDsProvider(null));
    }
}
