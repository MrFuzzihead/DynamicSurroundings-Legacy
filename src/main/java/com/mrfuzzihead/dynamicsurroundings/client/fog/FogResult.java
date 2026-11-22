package com.mrfuzzihead.dynamicsurroundings.client.fog;

import javax.annotation.Nonnull;

import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Relative fog range: multipliers applied to the fog start/end that vanilla, NotFine or another mod already produced.
 *
 * <p>
 * The old implementation stored absolute distances computed from {@code event.farPlaneDistance}. That silently threw
 * away NotFine's {@code TERRAIN_FOG}/{@code FOG_NEAR_DISTANCE} values and re-enabled fog when the user had disabled
 * it. Keeping scales preserves the external geometry: NotFine's 1 % near distance stays 1 %, and its 16 M "off"
 * distance stays effectively infinite.
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class FogResult {

    /** Vanilla's linear fog start as a fraction of the far plane. */
    public static final float DEFAULT_PLANE_SCALE = 0.75F;
    private int fogMode;
    private float startScale;
    private float endScale;

    public FogResult() {
        this.fogMode = 0;
        this.startScale = 1.0F;
        this.endScale = 1.0F;
    }

    public FogResult(int fogMode, float startScale, float endScale) {
        this.set(fogMode, startScale, endScale);
    }

    public FogResult(float startScale, float endScale) {
        this.set(startScale, endScale);
    }

    public FogResult(@Nonnull RenderFogEvent event) {
        this.set(event);
    }

    public void set(int fogMode, float startScale, float endScale) {
        this.fogMode = fogMode;
        this.endScale = Math.max(0.0F, endScale);
        float clampedStart = Math.max(0.0F, Math.min(startScale, this.endScale));
        this.startScale = fogMode < 0 ? 0.0F : clampedStart;
    }

    public void set(float startScale, float endScale) {
        this.set(0, startScale, endScale);
    }

    public void set(@Nonnull RenderFogEvent event) {
        this.set(event.fogMode, 1.0F, 1.0F);
    }

    public int getFogMode() {
        return this.fogMode;
    }

    public float getStartScale() {
        return this.startScale;
    }

    public float getEndScale() {
        return this.endScale;
    }

    public boolean isValid(@Nonnull RenderFogEvent event) {
        return this.endScale > this.startScale && event.fogMode == this.fogMode;
    }

    @Override
    public String toString() {
        return String.format("[mode: %d, startScale: %f, endScale: %f]", this.fogMode, this.startScale, this.endScale);
    }
}
