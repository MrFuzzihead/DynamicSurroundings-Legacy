package com.mrfuzzihead.dynamicsurroundings.compat;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;

import com.mrfuzzihead.dynamicsurroundings.client.FogEffectHandler;
import com.mrfuzzihead.dynamicsurroundings.client.weather.Weather;
import com.mrfuzzihead.dynamicsurroundings.client.weather.WeatherBackend;
import com.mrfuzzihead.dynamicsurroundings.compat.notfine.NotFineBridge;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Shared compatibility diagnostics for the F3 text path and Angelica's debug screen. */
@SideOnly(Side.CLIENT)
public final class CompatibilityDiagnostics {

    private CompatibilityDiagnostics() {}

    public static String modSummary() {
        String iris = Compat.version(Compat.IRIS);
        if ("absent".equals(iris)) {
            iris = Compat.version(Compat.OCULUS);
        }
        return "Angelica " + Compat.version(Compat.ANGELICA)
            + ", NotFine "
            + Compat.version(Compat.NOTFINE)
            + ", Iris "
            + iris
            + ", BOP "
            + Compat.version(Compat.BIOMES_O_PLENTY);
    }

    public static String fogSummary() {
        return FogEffectHandler.effectiveFog();
    }

    public static List<String> lines(World world) {
        List<String> lines = new ArrayList<>();
        lines.add("Mods: " + modSummary());
        lines.add(
            "Weather backend: " + (world instanceof WorldClient ? WeatherBackend.owner((WorldClient) world) : "n/a"));
        lines.add("Fog: " + fogSummary());
        lines.add("NotFine: " + NotFineBridge.diagnostic());
        lines.add(Weather.diagnostic());
        return lines;
    }
}
