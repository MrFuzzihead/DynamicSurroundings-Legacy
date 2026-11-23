package com.mrfuzzihead.dynamicsurroundings.client.fog;

import static org.junit.Assert.assertEquals;

import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;

import org.junit.Test;
import org.lwjgl.opengl.GL11;

public class FogRangeCalculatorsTest {

    private static RenderFogEvent event(float farPlane) {
        return new RenderFogEvent(null, null, null, 0.0, GL11.GL_LINEAR, farPlane);
    }

    @Test
    public void vanillaCalculatorUsesTheVanillaGeometry() {
        FogResult result = new VanillaFogRangeCalculator().calculate(event(128.0F));

        assertEquals(1.0F, result.getStartScale(), 0.0F);
        assertEquals(1.0F, result.getEndScale(), 0.0F);
    }

    @Test
    public void weatherFogScalesBothEndsWithoutExceedingTheBase() {
        FogResult result = new WeatherFogRangeCalculator().calculate(event(128.0F), 0.5F);

        assertEquals(0.55F, result.getStartScale(), 0.0001F);
        assertEquals(0.8F, result.getEndScale(), 0.0001F);
    }

    @Test
    public void holisticCalculatorTakesTheMostRestrictiveScale() {
        HolisticFogRangeCalculator holistic = new HolisticFogRangeCalculator();
        holistic.add(calculator(new FogResult(0, 0.5F, 0.8F)));
        holistic.add(calculator(new FogResult(0, 0.7F, 0.9F)));

        FogResult result = holistic.calculate(event(128.0F));

        assertEquals(0.5F, result.getStartScale(), 0.0F);
        assertEquals(0.8F, result.getEndScale(), 0.0F);
    }

    @Test
    public void holisticCalculatorClampsAnInvertedResult() {
        HolisticFogRangeCalculator holistic = new HolisticFogRangeCalculator();
        holistic.add(calculator(new FogResult(0, 0.9F, 0.5F)));

        FogResult result = holistic.calculate(event(128.0F));

        assertEquals(0.5F, result.getStartScale(), 0.0F);
        assertEquals(0.5F, result.getEndScale(), 0.0F);
    }

    private static IFogRangeCalculator calculator(FogResult result) {
        return new IFogRangeCalculator() {

            @Override
            public FogResult calculate(RenderFogEvent event) {
                return result;
            }

            @Override
            public void tick() {}
        };
    }
}
