package com.mrfuzzihead.dynamicsurroundings.compat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.compat.angelica.IrisPhaseBridge;

public class IrisPhaseBridgeTest {

    @Test
    public void absentIrisIsANoOp() {
        assertFalse(IrisPhaseBridge.isAvailable());
        assertNull(IrisPhaseBridge.push("CLOUDS"));
        IrisPhaseBridge.pop(null);
    }
}
