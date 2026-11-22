package com.mrfuzzihead.dynamicsurroundings.client;

import javax.annotation.Nonnull;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent.FogColors;
import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;
import net.minecraftforge.event.world.WorldEvent.Load;

import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.fog.BiomeFogColorCalculator;
import com.mrfuzzihead.dynamicsurroundings.client.fog.BiomeFogRangeCalculator;
import com.mrfuzzihead.dynamicsurroundings.client.fog.FogResult;
import com.mrfuzzihead.dynamicsurroundings.client.fog.HazeFogRangeCalculator;
import com.mrfuzzihead.dynamicsurroundings.client.fog.HolisticFogColorCalculator;
import com.mrfuzzihead.dynamicsurroundings.client.fog.HolisticFogRangeCalculator;
import com.mrfuzzihead.dynamicsurroundings.client.fog.MorningFogRangeCalculator;
import com.mrfuzzihead.dynamicsurroundings.client.fog.WeatherFogRangeCalculator;
import com.mrfuzzihead.dynamicsurroundings.event.DiagnosticEvent;
import com.mrfuzzihead.dynamicsurroundings.util.Color;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FogEffectHandler implements IClientEffectHandler {

    protected HolisticFogColorCalculator fogColor = new HolisticFogColorCalculator();
    protected HolisticFogRangeCalculator fogRange = new HolisticFogRangeCalculator();

    @Override
    public boolean hasEvents() {
        return true;
    }

    private boolean doFog() {
        return ModOptions.enableBiomeFog || ModOptions.allowDesertFog
            || ModOptions.enableElevationHaze
            || ModOptions.enableMorningFog
            || ModOptions.enableWeatherFog;
    }

    @Override
    public void process(World world, EntityPlayer player) {
        if (this.doFog()) {
            this.fogRange.tick();
            this.fogColor.tick();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void fogColorEvent(FogColors event) {
        if (this.doFog()) {
            Material material = event.block.getMaterial();
            if (material != Material.lava && material != Material.water) {
                Color color = this.fogColor.calculate(event);
                if (color != null) {
                    event.red = color.red;
                    event.green = color.green;
                    event.blue = color.blue;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void fogRenderEvent(RenderFogEvent event) {
        if (this.doFog()) {
            Material material = event.block.getMaterial();
            if (material != Material.lava && material != Material.water && event.fogMode >= 0) {
                FogResult result = this.fogRange.calculate(event);
                if (result != null && result.isValid(event)) {
                    float baseStart;
                    float baseEnd;
                    if (ModOptions.respectExternalFog) {
                        baseStart = GL11.glGetFloat(GL11.GL_FOG_START);
                        baseEnd = GL11.glGetFloat(GL11.GL_FOG_END);
                    } else {
                        baseStart = event.farPlaneDistance * FogResult.DEFAULT_PLANE_SCALE;
                        baseEnd = event.farPlaneDistance;
                    }

                    float[] range = computeRange(result, baseStart, baseEnd);
                    if (range != null) {
                        GL11.glFogf(GL11.GL_FOG_START, range[0]);
                        GL11.glFogf(GL11.GL_FOG_END, range[1]);
                    }
                }
            }
        }
    }

    /**
     * Applies a relative {@link FogResult} to the current base range. Kept separate from GL so the NotFine/Angelica
     * composition rule can be tested without a context.
     */
    static float[] computeRange(FogResult result, float baseStart, float baseEnd) {
        if (result == null || baseEnd <= 0.0F
            || baseEnd <= baseStart
            || result.getStartScale() == 1.0F && result.getEndScale() == 1.0F) {
            return null;
        }

        float start = baseStart * result.getStartScale();
        float end = baseEnd * result.getEndScale();
        return end > start ? new float[] { start, end } : null;
    }

    @SubscribeEvent
    public void diagnostics(DiagnosticEvent.Gather event) {
        if (this.doFog()) {
            event.output.add("Fog Range: " + this.fogRange.toString());
            event.output.add("Fog Color: " + this.fogColor.toString());
        } else {
            event.output.add("FOG: IGNORED");
        }
    }

    @SubscribeEvent(receiveCanceled = false, priority = EventPriority.LOWEST)
    public void onWorldLoad(@Nonnull Load event) {
        if (event.world.isRemote) {
            this.setupTheme(event.world);
        }
    }

    protected void setupTheme(@Nonnull World world) {
        this.fogColor = new HolisticFogColorCalculator();
        this.fogRange = new HolisticFogRangeCalculator();
        if (ModOptions.enableBiomeFog) {
            this.fogColor.add(new BiomeFogColorCalculator());
            this.fogRange.add(new BiomeFogRangeCalculator());
        }

        if (ModOptions.enableElevationHaze) {
            this.fogRange.add(new HazeFogRangeCalculator());
        }

        if (ModOptions.enableMorningFog) {
            this.fogRange.add(new MorningFogRangeCalculator());
        }

        if (ModOptions.enableWeatherFog) {
            this.fogRange.add(new WeatherFogRangeCalculator());
        }
    }
}
