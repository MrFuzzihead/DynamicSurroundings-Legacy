package com.mrfuzzihead.dynamicsurroundings.client.fog;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;
import com.mrfuzzihead.dynamicsurroundings.testutil.EnvironStateTestSupport;

public class MorningAndHazeFogTest {

    private final int originalChance = ModOptions.morningFogChance;

    @Before
    public void installWorld() {
        World world = mock(World.class);
        when(world.getWorldTime()).thenReturn(0L);
        when(world.getCelestialAngle(0.0F)).thenReturn(0.73F);
        EnvironStateTestSupport.setPlayer(player(world));
        EnvironStateTestSupport.setDimensionId(0);
        ModOptions.morningFogChance = 1;
    }

    @After
    public void reset() {
        EnvironStateTestSupport.reset();
        ModOptions.morningFogChance = this.originalChance;
    }

    @Test
    public void morningFogAppliesInsideTheDawnWindow() {
        MorningFogRangeCalculator calculator = new MorningFogRangeCalculator();
        calculator.tick();

        FogResult result = calculator.calculate(event(128.0F));

        assertEquals(11.0F / (128.0F * FogResult.DEFAULT_PLANE_SCALE), result.getStartScale(), 1.0e-5F);
        assertEquals(0.25F, result.getEndScale(), 1.0e-5F);
    }

    @Test
    public void morningFogIsSkippedOutsideTheWindowAndInTheNether() {
        World world = mock(World.class);
        when(world.getWorldTime()).thenReturn(0L);
        when(world.getCelestialAngle(0.0F)).thenReturn(0.5F);
        EnvironStateTestSupport.setPlayer(player(world));
        MorningFogRangeCalculator outside = new MorningFogRangeCalculator();
        outside.tick();
        assertEquals(
            1.0F,
            outside.calculate(event(128.0F))
                .getStartScale(),
            0.0F);

        when(world.getCelestialAngle(0.0F)).thenReturn(0.73F);
        EnvironStateTestSupport.setDimensionId(-1);
        MorningFogRangeCalculator nether = new MorningFogRangeCalculator();
        nether.tick();
        assertEquals(
            1.0F,
            nether.calculate(event(128.0F))
                .getStartScale(),
            0.0F);
    }

    @Test
    public void hazeFogScalesAtTheCloudBand() {
        DimensionRegistry dimension = mock(DimensionRegistry.class);
        when(dimension.getHasHaze()).thenReturn(true);
        when(dimension.getCloudHeight()).thenReturn(128);
        when(dimension.hasExplicitCloudHeight()).thenReturn(true);
        EnvironStateTestSupport.setDimensionInfo(dimension);
        EntityPlayer player = mock(EntityPlayer.class);
        player.posY = 128.0D;
        player.eyeHeight = 0.0F;
        EnvironStateTestSupport.setPlayer(player);

        FogResult result = new HazeFogRangeCalculator().calculate(event(128.0F));

        assertEquals((1.0F - 0.95F) / FogResult.DEFAULT_PLANE_SCALE, result.getStartScale(), 1.0e-5F);
        assertEquals(1.0F - 0.6F, result.getEndScale(), 1.0e-5F);
    }

    @Test
    public void hazeFogIsNeutralOutsideTheBandAndWithoutDimensionData() {
        EntityPlayer player = mock(EntityPlayer.class);
        player.posY = 0.0D;
        EnvironStateTestSupport.setPlayer(player);
        DimensionRegistry dimension = mock(DimensionRegistry.class);
        when(dimension.getHasHaze()).thenReturn(true);
        when(dimension.getCloudHeight()).thenReturn(128);
        when(dimension.hasExplicitCloudHeight()).thenReturn(true);
        EnvironStateTestSupport.setDimensionInfo(dimension);

        assertEquals(
            1.0F,
            new HazeFogRangeCalculator().calculate(event(128.0F))
                .getStartScale(),
            0.0F);

        EnvironStateTestSupport.setDimensionInfo(null);
        assertEquals(
            1.0F,
            new HazeFogRangeCalculator().calculate(event(128.0F))
                .getStartScale(),
            0.0F);
    }

    private static EntityPlayer player(World world) {
        EntityPlayer player = mock(EntityPlayer.class);
        player.worldObj = world;
        return player;
    }

    private static RenderFogEvent event(float farPlane) {
        return new RenderFogEvent(null, null, null, 0.0, GL11.GL_LINEAR, farPlane);
    }
}
