package com.mrfuzzihead.dynamicsurroundings.client.footsteps.parsers;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.client.footsteps.engine.interfaces.ILibrary;

public class AcousticsJsonReaderTest {

    private final ILibrary library = mock(ILibrary.class);

    @Test
    public void missingTypeIsAJsonParseException() throws Exception {
        assertParseError("{\"engineversion\":0,\"contents\":{}}", "type");
    }

    @Test
    public void missingEngineVersionIsAJsonParseException() throws Exception {
        assertParseError("{\"type\":\"library\",\"contents\":{}}", "engineversion");
    }

    @Test
    public void emptySoundNameIsRejected() throws Exception {
        assertParseError(
            "{\"type\":\"library\",\"engineversion\":0,\"contents\":{\"a\":{\"walk\":\"\"}}}",
            "Sound name");
    }

    @Test
    public void delayedWithoutDelayBoundsIsRejected() throws Exception {
        assertParseError(
            "{\"type\":\"library\",\"engineversion\":0,\"contents\":{\"a\":{\"walk\":{\"type\":\"delayed\",\"name\":\"x\"}}}}",
            "delay");
    }

    private void assertParseError(String json, String expectedText) throws Exception {
        try {
            new AcousticsJsonReader("").parseJSONUnsafe(json, this.library);
            fail("expected JsonParseException");
        } catch (JsonParseException expected) {
            assertTrue(
                expected.getMessage(),
                expected.getMessage()
                    .contains(expectedText));
        }
    }
}
