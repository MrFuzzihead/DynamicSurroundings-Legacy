package com.mrfuzzihead.dynamicsurroundings.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;

import net.minecraft.util.Vec3;

import org.junit.Test;

/**
 * {@link RgbColor} holds floats in the 0..1 range but is constructed from the 0..255 integers the config files and the
 * sound/aurora definitions use, so the conversions are worth pinning down.
 */
public class RgbColorTest {

    private static final float EPSILON = 1.0e-5F;

    @Test
    public void integerCtorScalesIntoTheUnitRange() {
        RgbColor color = new RgbColor(255, 127, 51);
        assertEquals(1.0F, color.red, EPSILON);
        assertEquals(127.0F / 255.0F, color.green, EPSILON);
        assertEquals(51.0F / 255.0F, color.blue, EPSILON);
    }

    @Test
    public void packedRgbUnpacksWithoutTheAlphaChannel() {
        RgbColor color = new RgbColor(0xFF8000);
        assertEquals(1.0F, color.red, 1.0F / 255.0F);
        assertEquals(128.0F / 255.0F, color.green, 1.0F / 255.0F);
        assertEquals(0.0F, color.blue, EPSILON);

        // The alpha byte is ignored on the way in and on the way out.
        assertEquals(0x00FF00, new RgbColor(0xFF00FF00).rgb());
    }

    @Test
    public void rgbRoundTripsThroughTheConstructor() {
        assertEquals(0xFF0000, RgbColor.RED.rgb());
        assertEquals(0x00FF00, RgbColor.GREEN.rgb());
        assertEquals(0x0000FF, RgbColor.BLUE.rgb());
        assertEquals(0xFFFFFF, RgbColor.WHITE.rgb());
        assertEquals(0x000000, RgbColor.BLACK.rgb());

        RgbColor color = new RgbColor(12, 34, 56);
        int packed = color.rgb();
        RgbColor roundTripped = new RgbColor(packed);
        assertEquals(packed, roundTripped.rgb());
        assertEquals(12.0F / 255.0F, roundTripped.red, EPSILON);
        assertEquals(34.0F / 255.0F, roundTripped.green, EPSILON);
        assertEquals(56.0F / 255.0F, roundTripped.blue, EPSILON);
    }

    @Test
    public void rgbWithAlphaPacksTheAlphaByteOnTop() {
        int packed = RgbColor.WHITE.rgbWithAlpha(1.0F);
        assertEquals(0xFFFFFFFF, packed);
        // Packing an alpha of zero leaves the colour untouched.
        assertEquals(RgbColor.WHITE.rgb(), RgbColor.WHITE.rgbWithAlpha(0.0F));
    }

    @Test
    public void copyConstructorIsIndependent() {
        RgbColor source = new RgbColor(0.25F, 0.5F, 0.75F);
        RgbColor copy = new RgbColor(source);
        copy.scale(2.0F);
        assertEquals(0.25F, source.red, EPSILON);
        assertEquals(0.5F, source.green, EPSILON);
        assertEquals(0.75F, source.blue, EPSILON);
    }

    @Test
    public void scaleMultipliesInPlaceAndTheStaticVariantDoesNot() {
        RgbColor color = new RgbColor(0.5F, 0.5F, 0.5F);
        RgbColor scaled = color.scale(2.0F);
        assertSameInstance(color, scaled);
        assertEquals(1.0F, color.red, EPSILON);

        RgbColor original = new RgbColor(0.5F, 0.5F, 0.5F);
        RgbColor result = RgbColor.scale(original, 0.5F);
        assertEquals(0.5F, original.red, EPSILON);
        assertEquals(0.25F, result.red, EPSILON);
    }

    @Test
    public void perChannelScaleOnlyTouchesItsChannel() {
        RgbColor color = new RgbColor(1.0F, 1.0F, 1.0F).scale(0.5F, 0.25F, 0.125F);
        assertEquals(0.5F, color.red, EPSILON);
        assertEquals(0.25F, color.green, EPSILON);
        assertEquals(0.125F, color.blue, EPSILON);
    }

    @Test
    public void addAndMixBlendTheChannels() {
        RgbColor added = new RgbColor(0.1F, 0.2F, 0.3F).add(0.1F, 0.1F, 0.1F);
        assertEquals(0.2F, added.red, EPSILON);
        assertEquals(0.3F, added.green, EPSILON);
        assertEquals(0.4F, added.blue, EPSILON);

        RgbColor mixed = new RgbColor(0.0F, 0.0F, 0.0F).mix(1.0F, 1.0F, 1.0F);
        assertEquals(0.5F, mixed.red, EPSILON);
        assertEquals(0.5F, mixed.green, EPSILON);
        assertEquals(0.5F, mixed.blue, EPSILON);

        RgbColor mixedWithColor = new RgbColor(0.0F, 0.5F, 1.0F).mix(new RgbColor(1.0F, 0.5F, 0.0F));
        assertEquals(0.5F, mixedWithColor.red, EPSILON);
        assertEquals(0.5F, mixedWithColor.green, EPSILON);
        assertEquals(0.5F, mixedWithColor.blue, EPSILON);
    }

    @Test
    public void blendIsARootMeanSquareSoItNeverOvershoots() {
        // blend(0, 1, 0.5) = sqrt(0.5) rather than the arithmetic 0.5.
        RgbColor half = new RgbColor(0.0F, 0.0F, 0.0F).blend(new RgbColor(1.0F, 1.0F, 1.0F), 0.5F);
        assertEquals((float) Math.sqrt(0.5), half.red, EPSILON);

        assertEquals(0.0F, new RgbColor(0.0F, 0.0F, 0.0F).blend(new RgbColor(1.0F, 1.0F, 1.0F), 0.0F).red, EPSILON);
        assertEquals(1.0F, new RgbColor(0.0F, 0.0F, 0.0F).blend(new RgbColor(1.0F, 1.0F, 1.0F), 1.0F).red, EPSILON);
    }

    @Test
    public void luminanceBrightensAndClamps() {
        RgbColor brightened = new RgbColor(0.5F, 0.5F, 0.5F).luminance(0.5F);
        assertEquals(0.75F, brightened.red, EPSILON);

        RgbColor clamped = new RgbColor(1.0F, 1.0F, 1.0F).luminance(1.0F);
        assertEquals(1.0F, clamped.red, EPSILON);

        RgbColor darkened = new RgbColor(0.5F, 0.5F, 0.5F).luminance(-1.0F);
        assertEquals(0.0F, darkened.red, EPSILON);
    }

    @Test
    public void adjustStopsAtTheTargetColor() {
        RgbColor color = new RgbColor(0.0F, 0.0F, 0.0F);
        color.adjust(Vec3.createVectorHelper(0.6D, 0.6D, 0.6D), new RgbColor(1.0F, 1.0F, 1.0F));
        assertEquals(0.6F, color.red, 1.0e-4F);

        // Overshooting in one step snaps to the target.
        color.adjust(Vec3.createVectorHelper(0.6D, 0.6D, 0.6D), new RgbColor(1.0F, 1.0F, 1.0F));
        assertEquals(1.0F, color.red, 1.0e-4F);
        assertEquals(1.0F, color.green, 1.0e-4F);
        assertEquals(1.0F, color.blue, 1.0e-4F);
    }

    @Test
    public void transitionToReturnsAPerIterationDelta() {
        Vec3 delta = new RgbColor(0.0F, 0.0F, 0.0F).transitionTo(new RgbColor(1.0F, 0.5F, 0.0F), 4);
        assertEquals(0.25D, delta.xCoord, 1.0e-6);
        assertEquals(0.125D, delta.yCoord, 1.0e-6);
        assertEquals(0.0D, delta.zCoord, 1.0e-6);
    }

    @Test
    public void toVec3dCarriesTheChannels() {
        Vec3 vec = new RgbColor(0.1F, 0.2F, 0.3F).toVec3d();
        assertEquals(0.1F, (float) vec.xCoord, EPSILON);
        assertEquals(0.2F, (float) vec.yCoord, EPSILON);
        assertEquals(0.3F, (float) vec.zCoord, EPSILON);
    }

    @Test
    public void equalsComparesTheChannelsAndRejectsForeignTypes() {
        assertEquals(new RgbColor(1, 2, 3), new RgbColor(1, 2, 3));
        assertFalse(new RgbColor(1, 2, 3).equals(new RgbColor(3, 2, 1)));
        assertFalse(new RgbColor(1, 2, 3).equals(null));
        assertFalse(new RgbColor(1, 2, 3).equals("not a color"));
    }

    @Test
    public void toStringReportsTheIntegerChannels() {
        assertEquals("[r:255,g:0,b:0]", RgbColor.RED.toString());
        assertEquals("[r:0,g:0,b:255]", RgbColor.BLUE.toString());
    }

    @Test
    public void immutableColorsRejectEveryMutator() {
        RgbColor immutable = RgbColor.RED.asImmutable();
        assertNotSame(RgbColor.RED, immutable);
        assertEquals(RgbColor.RED, immutable);

        try {
            immutable.scale(2.0F);
            fail("scale should not be allowed on an immutable color");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        try {
            immutable.mix(1.0F, 1.0F, 1.0F);
            fail("mix should not be allowed on an immutable color");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        try {
            immutable.adjust(Vec3.createVectorHelper(1.0D, 1.0D, 1.0D), RgbColor.WHITE);
            fail("adjust should not be allowed on an immutable color");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        try {
            immutable.scale(2.0F, 2.0F, 2.0F);
            fail("per-channel scale should not be allowed on an immutable color");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        try {
            immutable.add(1.0F, 1.0F, 1.0F);
            fail("add should not be allowed on an immutable color");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        try {
            immutable.add(new RgbColor(1.0F, 1.0F, 1.0F));
            fail("color add should not be allowed on an immutable color");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        try {
            immutable.blend(new RgbColor(1.0F, 1.0F, 1.0F), 0.5F);
            fail("blend should not be allowed on an immutable color");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        try {
            immutable.mix(new RgbColor(1.0F, 1.0F, 1.0F));
            fail("color mix should not be allowed on an immutable color");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        assertEquals(0xFF0000, RgbColor.RED.rgb());
    }

    @Test
    public void equalColorsShareAHashCode() {
        assertEquals(new RgbColor(1, 2, 3).hashCode(), new RgbColor(1, 2, 3).hashCode());
    }

    @Test
    public void immutablePaletteKeepsItsValues() {
        assertEquals(0xD2B48C, RgbColor.TAN.rgb());
        assertEquals(0x2F4F4F, RgbColor.DARKSLATEGRAY.rgb());
        assertEquals(0xAAAAAA, RgbColor.MC_GRAY.rgb());
    }

    @Test
    public void integerRoundTripsHoldForRandomColors() {
        Random random = new Random(0xC0105EEDL);
        for (int i = 0; i < 1000; i++) {
            int rgb = random.nextInt(0x1000000);
            RgbColor color = new RgbColor(rgb);
            assertEquals("rgb " + Integer.toHexString(rgb), rgb, color.rgb());
            assertEquals(new RgbColor(rgb), color);
        }
    }

    private static void assertSameInstance(Object expected, Object actual) {
        assertTrue("expected the same instance", expected == actual);
    }
}
