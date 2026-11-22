package com.mrfuzzihead.dynamicsurroundings.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSavedData;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.mrfuzzihead.dynamicsurroundings.testutil.WorldTestSupport;

public class DimensionEffectDataFileTest {

    @Test
    public void repeatedReadsAndSettersOnlyMarkDirtyWhenDataChanges() {
        World world = mock(World.class);
        WorldProvider provider = mock(WorldProvider.class);
        provider.dimensionId = 0;
        WorldTestSupport.setProvider(world, provider);
        when(world.loadItemData(DimensionEffectDataFile.class, "dynamicsurroundings")).thenReturn(null);

        DimensionEffectData data = DimensionEffectDataFile.get(world);

        ArgumentCaptor<WorldSavedData> captor = ArgumentCaptor.forClass(WorldSavedData.class);
        verify(world).setItemData(anyString(), captor.capture());
        DimensionEffectDataFile file = (DimensionEffectDataFile) captor.getValue();
        file.setDirty(false);

        when(world.loadItemData(DimensionEffectDataFile.class, "dynamicsurroundings")).thenReturn(file);
        DimensionEffectDataFile.get(world);
        assertFalse("reading existing data must not rewrite it", file.isDirty());
        verify(world, times(1)).loadItemData(DimensionEffectDataFile.class, "dynamicsurroundings");

        data.setRainIntensity(0.5F);
        assertTrue("a real setter must mark the saved data dirty", file.isDirty());

        DimensionEffectDataFile.unload(world);
        DimensionEffectDataFile.get(world);
        verify(world, times(2)).loadItemData(DimensionEffectDataFile.class, "dynamicsurroundings");
    }
}
