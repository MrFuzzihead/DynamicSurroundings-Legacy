package com.mrfuzzihead.dynamicsurroundings.client.footsteps.util.property.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import org.junit.Test;

/**
 * The footstep engine reads its tuning values out of JSON as strings and parses them on access. A missing key and a
 * malformed value are distinct failures for it, so both exception paths are covered.
 */
public class ConfigPropertyTest {

    @Test
    public void typedGettersParseTheStoredStrings() {
        ConfigProperty props = load(
            "{\"name\":\"grass\",\"count\":\"7\",\"ratio\":\"1.5\",\"big\":\"9000000000\",\"precise\":\"2.25\","
                + "\"flag\":\"true\"}");

        assertEquals("grass", props.getString("name"));
        assertEquals(7, props.getInteger("count"));
        assertEquals(1.5F, props.getFloat("ratio"), 0.0F);
        assertEquals(9000000000L, props.getLong("big"));
        assertEquals(2.25D, props.getDouble("precise"), 0.0D);
        assertTrue(props.getBoolean("flag"));
    }

    @Test
    public void booleansOnlyRecogniseTrue() {
        ConfigProperty props = load("{\"a\":\"true\",\"b\":\"false\",\"c\":\"yes\",\"d\":\"TRUE\"}");
        assertTrue(props.getBoolean("a"));
        assertFalse(props.getBoolean("b"));
        // Boolean.parseBoolean is lenient for false-ish input, which the engine relies on.
        assertFalse(props.getBoolean("c"));
        assertTrue(props.getBoolean("d"));
    }

    @Test
    public void missingKeysThrowMissingProperty() {
        ConfigProperty props = load("{}");
        for (String getter : new String[] { "string", "boolean", "integer", "float", "long", "double" }) {
            try {
                switch (getter) {
                    case "string":
                        props.getString("nope");
                        break;
                    case "boolean":
                        props.getBoolean("nope");
                        break;
                    case "integer":
                        props.getInteger("nope");
                        break;
                    case "float":
                        props.getFloat("nope");
                        break;
                    case "long":
                        props.getLong("nope");
                        break;
                    default:
                        props.getDouble("nope");
                        break;
                }
                fail("missing key should throw for " + getter);
            } catch (PropertyMissingException expected) {
                // expected
            }
        }
    }

    @Test
    public void malformedNumbersThrowTypeErrors() {
        ConfigProperty props = load("{\"i\":\"not-a-number\",\"f\":\"x\",\"l\":\"y\",\"d\":\"z\"}");
        assertTypeError(new Runnable() {

            @Override
            public void run() {
                props.getInteger("i");
            }
        });
        assertTypeError(new Runnable() {

            @Override
            public void run() {
                props.getFloat("f");
            }
        });
        assertTypeError(new Runnable() {

            @Override
            public void run() {
                props.getLong("l");
            }
        });
        assertTypeError(new Runnable() {

            @Override
            public void run() {
                props.getDouble("d");
            }
        });
    }

    @Test
    public void setPropertyUsesToStringAndIsExposed() {
        ConfigProperty props = load("{}");
        props.setProperty("count", 12);
        props.setProperty("ratio", 0.5F);
        assertEquals(12, props.getInteger("count"));
        assertEquals(0.5F, props.getFloat("ratio"), 0.0F);

        Map<String, String> all = props.getAllProperties();
        assertEquals(2, all.size());
        assertEquals("12", all.get("count"));
    }

    @Test
    public void fromStreamReturnsAnEmptyObjectForANullStream() {
        ConfigProperty props = ConfigProperty.fromStream(null);
        assertTrue(
            props.getAllProperties()
                .isEmpty());
        assertFalse(ConfigProperty.loadStream(props, null));
    }

    @Test
    public void jsonNullLoadsAsAnEmptyObject() throws Exception {
        ConfigProperty props = load("null");
        assertTrue(
            props.getAllProperties()
                .isEmpty());
        assertFalse(ConfigProperty.loadStream(props, new ByteArrayInputStream("null".getBytes("UTF-8"))));
    }

    @Test
    public void emptyObjectLeavesNoProperties() {
        ConfigProperty props = load("{}");
        assertNull(
            props.getAllProperties()
                .get("anything"));
    }

    private static ConfigProperty load(String json) {
        try {
            InputStream stream = new ByteArrayInputStream(json.getBytes("UTF-8"));
            return ConfigProperty.fromStream(stream);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static void assertTypeError(Runnable action) {
        try {
            action.run();
            fail("malformed value should throw a type error");
        } catch (PropertyTypeException expected) {
            // expected
        }
    }
}
