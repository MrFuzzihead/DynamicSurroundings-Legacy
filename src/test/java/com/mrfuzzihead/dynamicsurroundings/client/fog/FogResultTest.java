package com.mrfuzzihead.dynamicsurroundings.client.fog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;

import org.junit.Test;
import org.lwjgl.opengl.GL11;

public class FogResultTest {

    private static RenderFogEvent event(int mode, float farPlane) {
        return new RenderFogEvent(null, null, null, 0.0, mode, farPlane);
    }

    @Test
    public void vanillaGeometryIsExpressedAsScales() {
        FogResult result = new FogResult(event(0, 128.0F));

        assertEquals(1.0F, result.getStartScale(), 0.0F);
        assertEquals(1.0F, result.getEndScale(), 0.0F);
        assertTrue(
            "a 1:1 scale is cacheable and means 'leave the external fog untouched'",
            result.isValid(event(0, 128.0F)));
    }

    @Test
    public void startIsClampedToEndSoTheRangeCannotInvert() {
        FogResult result = new FogResult(0, 0.9F, 0.5F);

        assertEquals(0.5F, result.getStartScale(), 0.0F);
        assertEquals(0.5F, result.getEndScale(), 0.0F);
        assertTrue("an empty range is cacheable but the handler will not write it", result.isValid(event(0, 128.0F)));
    }

    @Test
    public void modeMismatchInvalidatesTheCachedResult() {
        FogResult result = new FogResult(0, 0.75F, 1.0F);
        assertFalse(result.isValid(event(GL11.GL_EXP, 128.0F)));
    }

    @Test
    public void densityModeHasNoStart() {
        FogResult result = new FogResult(-1, 0.75F, 1.0F);
        assertEquals(0.0F, result.getStartScale(), 0.0F);
        assertEquals(1.0F, result.getEndScale(), 0.0F);
    }
}
