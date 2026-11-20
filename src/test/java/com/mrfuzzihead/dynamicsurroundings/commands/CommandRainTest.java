package com.mrfuzzihead.dynamicsurroundings.commands;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.storage.WorldInfo;

import org.junit.Before;
import org.junit.Test;

import com.mrfuzzihead.dynamicsurroundings.data.DimensionEffectData;

/**
 * {@code /rain status} is the only in-game diagnostic the mod has for its weather state, so its formatting is pinned
 * here rather than left to the in-game test.
 */
public class CommandRainTest {

    private World world;
    private WorldInfo info;
    private WorldProvider provider;
    private DimensionEffectData data;

    @Before
    public void setUp() {
        world = mock(World.class);
        info = mock(WorldInfo.class);
        provider = mock(WorldProvider.class);
        world.provider = provider;
        when(world.getWorldInfo()).thenReturn(info);

        data = new DimensionEffectData(0);
        data.setRainIntensity(0.5F);
    }

    @Test
    public void statusReportsTheDimensionsWeather() {
        when(info.getRainTime()).thenReturn(1200);
        when(world.isRaining()).thenReturn(true);
        when(provider.isSurfaceWorld()).thenReturn(true);
        when(world.getRainStrength(1.0F)).thenReturn(0.5F);

        String status = CommandRain.statusOutput(world, data);

        assertTrue(status, status.startsWith("dim 0: intensity: 50"));
        assertTrue(status, status.contains("isRaining: true"));
        assertTrue(status, status.contains("isSurface: true"));
        assertTrue(status, status.contains("strength: 50.0"));
        // 1200 ticks / 20 ticks per second / 60 seconds = 1.0 minutes.
        assertTrue(status, status.contains("timer: 1.0 minutes"));
    }

    @Test
    public void statusReportsAStoppedRainTimerBeforeItStarts() {
        when(info.getRainTime()).thenReturn(0);
        when(world.isRaining()).thenReturn(false);
        when(provider.isSurfaceWorld()).thenReturn(false);
        when(world.getRainStrength(1.0F)).thenReturn(0.0F);

        String status = CommandRain.statusOutput(world, data);

        assertTrue(status, status.contains("isRaining: false"));
        assertTrue(status, status.contains("isSurface: false"));
        assertTrue(status, status.contains("strength: 0.0"));
        assertTrue(status, status.contains("timer: 0.0 minutes"));
    }

    @Test
    public void statusIncludesTheIntensityWindow() {
        when(info.getRainTime()).thenReturn(24000);
        data.setMinRainIntensity(0.1F);
        data.setMaxRainIntensity(0.9F);

        String status = CommandRain.statusOutput(world, data);
        assertTrue(status, status.contains("[10,90]"));
        // 24000 / 20 / 60 = 20 minutes.
        assertTrue(status, status.contains("timer: 20.0 minutes"));
    }

    @Test
    public void commandIsRainWithTwoAliases() {
        CommandRain command = new CommandRain();
        org.junit.Assert.assertEquals("rain", command.getCommandName());
        org.junit.Assert.assertEquals(
            2,
            command.getCommandAliases()
                .size());
        assertTrue(
            command.getCommandAliases()
                .contains("r"));
        assertTrue(
            command.getCommandAliases()
                .contains("br"));
        org.junit.Assert.assertEquals(2, command.getRequiredPermissionLevel());
    }
}
