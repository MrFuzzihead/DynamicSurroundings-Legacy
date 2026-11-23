package com.mrfuzzihead.dynamicsurroundings.compat;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.compat.angelica.AngelicaDebugEntries;

public class AngelicaDebugEntriesTest {

    @Test
    public void absentAngelicaIsANoOp() {
        assertFalse(Compat.isAngelicaLoaded());
        AngelicaDebugEntries.register();
    }
}
