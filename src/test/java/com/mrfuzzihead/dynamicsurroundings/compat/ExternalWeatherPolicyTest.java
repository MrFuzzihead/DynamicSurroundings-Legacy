package com.mrfuzzihead.dynamicsurroundings.compat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ExternalWeatherPolicyTest {

    @Test
    public void eitherConfiguredExternalWeatherModDefersOwnership() {
        assertFalse(ExternalWeatherPolicy.isDeferred(false, false));
        assertTrue(ExternalWeatherPolicy.isDeferred(true, false));
        assertTrue(ExternalWeatherPolicy.isDeferred(false, true));
        assertTrue(ExternalWeatherPolicy.isDeferred(true, true));
    }
}
