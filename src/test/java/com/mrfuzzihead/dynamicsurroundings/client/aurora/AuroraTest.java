package com.mrfuzzihead.dynamicsurroundings.client.aurora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.data.AuroraPreset;

public class AuroraTest {

    private final boolean originalBands = ModOptions.auroraMultipleBands;

    @After
    public void restoreBands() {
        ModOptions.auroraMultipleBands = this.originalBands;
    }

    @Test
    public void oneBandUsesThePresetLength() {
        ModOptions.auroraMultipleBands = false;
        Aurora aurora = new Aurora(0.0F, 0.0F, 1234L, 0, 0);
        Node[][] bands = aurora.getNodeList();

        assertEquals(1, bands.length);
        assertEquals(AuroraPreset.get(0).length, bands[0].length);
    }

    @Test
    public void threeBandsShareTheLengthAndAreOffset() {
        ModOptions.auroraMultipleBands = true;
        Aurora aurora = new Aurora(0.0F, 0.0F, 1234L, 0, 0);
        Node[][] bands = aurora.getNodeList();

        assertEquals(3, bands.length);
        assertEquals(bands[0].length, bands[1].length);
        assertEquals(bands[0].length, bands[2].length);
        assertEquals(bands[0][0].posY - 2.0F, bands[1][0].posY, 1.0e-5F);
    }

    @Test
    public void generationIsDeterministicForASeed() {
        ModOptions.auroraMultipleBands = false;
        Aurora first = new Aurora(10.0F, 20.0F, 99L, 0, 0);
        Aurora second = new Aurora(10.0F, 20.0F, 99L, 0, 0);
        Aurora different = new Aurora(10.0F, 20.0F, 100L, 0, 0);

        Node[] firstNodes = first.getNodeList()[0];
        Node[] secondNodes = second.getNodeList()[0];
        Node[] differentNodes = different.getNodeList()[0];
        boolean differs = false;
        for (int i = 0; i < firstNodes.length; i++) {
            assertEquals(firstNodes[i].posX, secondNodes[i].posX, 0.0F);
            assertEquals(firstNodes[i].posY, secondNodes[i].posY, 0.0F);
            assertEquals(firstNodes[i].posZ, secondNodes[i].posZ, 0.0F);
            differs |= Float.compare(firstNodes[i].posX, differentNodes[i].posX) != 0;
        }
        assertTrue("a different seed should change the band", differs);
    }

    @Test
    public void fadeInAndOutChangeAlphaAtTheExpectedRate() {
        ModOptions.auroraMultipleBands = false;
        Aurora aurora = new Aurora(0.0F, 0.0F, 1L, 0, 0);
        assertEquals(1, aurora.getAlpha());

        for (int i = 0; i < 10; i++) {
            aurora.update();
        }
        assertEquals(2, aurora.getAlpha());

        aurora.die();
        assertFalse(aurora.isAlive());
        for (int i = 0; i < 10; i++) {
            aurora.update();
        }
        assertEquals(1, aurora.getAlpha());
    }

    @Test
    public void translationMovesTheNodesWithoutChangingTheirIdentity() {
        ModOptions.auroraMultipleBands = false;
        Aurora aurora = new Aurora(0.0F, 0.0F, 7L, 0, 0);
        Node node = aurora.getNodeList()[0][10];
        float originalZ = node.posZ;

        aurora.translate(0.0F);

        assertNotEquals(originalZ, node.getModdedZ(), 0.0F);
        assertEquals(node.posX, node.tetX, 0.0F);
    }
}
