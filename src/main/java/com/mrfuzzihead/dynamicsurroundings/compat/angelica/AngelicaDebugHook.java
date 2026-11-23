package com.mrfuzzihead.dynamicsurroundings.compat.angelica;

import net.minecraft.client.multiplayer.WorldClient;

import com.gtnewhorizons.angelica.api.debug.DebugContext;
import com.gtnewhorizons.angelica.api.debug.DebugEntryCategory;
import com.gtnewhorizons.angelica.api.debug.DebugEntrySpec;
import com.gtnewhorizons.angelica.api.debug.DebugGroupHandle;
import com.gtnewhorizons.angelica.api.debug.DebugGroupSpec;
import com.gtnewhorizons.angelica.api.debug.DebugScreenApi;
import com.gtnewhorizons.angelica.api.debug.DebugScreenDisplayer;
import com.gtnewhorizons.angelica.api.debug.DebugScreenEntry;
import com.mrfuzzihead.dynamicsurroundings.client.EnvironStateHandler;
import com.mrfuzzihead.dynamicsurroundings.client.weather.Weather;
import com.mrfuzzihead.dynamicsurroundings.client.weather.WeatherBackend;
import com.mrfuzzihead.dynamicsurroundings.compat.notfine.NotFineBridge;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Direct Angelica debug-screen registration. Loaded only when Angelica is present. */
@SideOnly(Side.CLIENT)
final class AngelicaDebugHook {

    private static final String GROUP_ID = "dynamicsurroundings";
    private static final String ENTRY_ID = "dynamicsurroundings.state";

    private AngelicaDebugHook() {}

    static void register() {
        DebugGroupHandle group = DebugScreenApi.registerGroup(
            new DebugGroupSpec(GROUP_ID).title("Dynamic Surroundings")
                .accent(0x55AAFF)
                .side(DebugGroupHandle.LEFT));
        DebugScreenApi.registerEntry(
            new DebugEntrySpec(ENTRY_ID, DebugEntryCategory.SCREEN_TEXT).name("Dynamic Surroundings")
                .text(handle -> new DebugScreenEntry() {

                    @Override
                    public void display(DebugScreenDisplayer displayer, DebugContext context) {
                        displayer.fact(group, "Weather backend")
                            .value(
                                context.world() instanceof WorldClient
                                    ? WeatherBackend.owner((WorldClient) context.world())
                                        .name()
                                    : "n/a");
                        displayer.fact(group, "Dimension")
                            .value(context.world().provider.getDimensionName());
                        displayer.fact(group, "Biome")
                            .value(
                                EnvironStateHandler.EnvironState.getSnapshot()
                                    .getBiomeName());
                        displayer.fact(group, "Conditions")
                            .value(
                                EnvironStateHandler.EnvironState.getSnapshot()
                                    .getConditions());
                        displayer.line(group)
                            .text(Weather.diagnostic());
                        displayer.fact(group, "NotFine downfall")
                            .value(String.valueOf(NotFineBridge.downfallDistance(10)));
                    }
                }));
    }
}
