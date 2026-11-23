package com.mrfuzzihead.dynamicsurroundings.compat;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class CompatTest {

    @Test
    public void absentModsAreNotReportedAsActive() {
        assertFalse(Compat.isAngelicaLoaded());
        assertFalse(Compat.isNotFineLoaded());
        assertFalse(Compat.isAngelicaBopFogActive());
    }
}
