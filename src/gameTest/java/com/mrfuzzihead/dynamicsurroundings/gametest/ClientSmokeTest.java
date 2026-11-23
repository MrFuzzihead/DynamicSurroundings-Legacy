package com.mrfuzzihead.dynamicsurroundings.gametest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.AuroraEffectHandler;
import com.mrfuzzihead.dynamicsurroundings.client.aurora.Aurora;
import com.mrfuzzihead.dynamicsurroundings.client.weather.WeatherBackend;
import com.mrfuzzihead.dynamicsurroundings.compat.CompatibilityDiagnostics;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Optional client-only smoke run, enabled by {@code -Ddynamicsurroundings.clientSmoke=true}.
 *
 * <p>
 * It creates a flat creative world, forces rain/night, renders 200 client ticks, and checks the render state that
 * cannot be asserted from a dedicated server: GL errors, the effective weather owner, fog start/end, and new Mixin or
 * uncaught-exception lines in the log. The Gradle wrapper is {@code ./gradlew clientSmokeTest}.
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class ClientSmokeTest {

    private static final String PREFIX = "[DS-CLIENT-SMOKE]";
    private static final int TARGET_FRAMES = 200;
    private static final int WORLD_LOAD_TIMEOUT_TICKS = 1200;
    private static final float EXPECTED_NEAR_FOG_PERCENT = parsePercent();

    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean launchRequested;
    private boolean configured;
    private boolean finished;
    private int ticks;
    private int loadTicks;
    private int frames;
    private int auroraFrames;
    private int glErrors;
    private Aurora smokeAurora;
    private long logOffset;
    private float fogStart = Float.NaN;
    private float fogEnd = Float.NaN;

    private static float parsePercent() {
        String value = System.getProperty("dynamicsurroundings.expectedNearFogPercent");
        if (value == null || value.isEmpty()) {
            return 0.0F;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0.0F;
        }
    }

    public static void install() {
        ClientSmokeTest test = new ClientSmokeTest();
        MinecraftForge.EVENT_BUS.register(test);
        FMLCommonHandler.instance()
            .bus()
            .register(test);
    }

    private ClientSmokeTest() {}

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.END || this.finished) {
            return;
        }

        if (this.mc.theWorld == null) {
            this.loadWorld();
            return;
        }

        if (this.mc.thePlayer == null) {
            if (++this.loadTicks > WORLD_LOAD_TIMEOUT_TICKS) {
                this.finish(false, "world did not produce a player");
            }
            return;
        }

        if (!this.configured) {
            this.configured = true;
            this.logOffset = logLength();
            this.mc.theWorld.getWorldInfo()
                .setRaining(true);
            this.mc.theWorld.getWorldInfo()
                .setRainTime(20000);
            this.mc.theWorld.setWorldTime(18000L);
            if (ModOptions.auroraEnable) {
                this.smokeAurora = new Aurora(0.0F, 0.0F, 1234L, 0, 0);
            }
        }

        this.ticks++;
        if (this.smokeAurora != null) {
            // Re-publish after AuroraEffectHandler's START-phase refresh; this keeps the renderer exercised
            // without depending on server-side aurora spawning.
            AuroraEffectHandler.currentAurora = this.smokeAurora;
        }
        int error = GL11.glGetError();
        if (error != 0) {
            this.glErrors++;
        }
        if (this.ticks >= TARGET_FRAMES) {
            this.finish(this.check(), "completed " + this.ticks + " client ticks");
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (this.mc.theWorld == null) {
            return;
        }
        this.fogStart = GL11.glGetFloat(GL11.GL_FOG_START);
        this.fogEnd = GL11.glGetFloat(GL11.GL_FOG_END);
        this.frames++;
        if (AuroraEffectHandler.currentAurora != null) {
            this.auroraFrames++;
        }
    }

    private void loadWorld() {
        if (this.launchRequested) {
            if (++this.loadTicks > WORLD_LOAD_TIMEOUT_TICKS) {
                this.finish(false, "world did not load");
            }
            return;
        }

        this.launchRequested = true;
        try {
            deleteRecursively(new File(this.mc.mcDataDir, "saves/ds_client_smoke"));
            this.mc.launchIntegratedServer(
                "ds_client_smoke",
                "DS Client Smoke",
                new WorldSettings(1234L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        } catch (Throwable t) {
            this.finish(false, "unable to launch the smoke world: " + t);
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        if (!file.delete()) {
            print("WARN unable to delete " + file);
        }
    }

    private boolean check() {
        boolean pass = true;
        if (this.glErrors != 0) {
            pass = false;
            print("FAIL glGetError count=" + this.glErrors);
        }

        WeatherBackend.Owner owner = WeatherBackend.owner(this.mc.theWorld);
        print("owner=" + owner + " fogStart=" + this.fogStart + " fogEnd=" + this.fogEnd);
        if (owner == WeatherBackend.Owner.ANGELICA_INSTANCED && WeatherBackend.isDsOwner(this.mc.theWorld)) {
            pass = false;
            print("FAIL DS claims ownership while Angelica instanced weather is active");
        }
        if (!Float.isFinite(this.fogStart) || !Float.isFinite(this.fogEnd)
            || this.fogEnd <= 0.0F
            || this.fogStart > this.fogEnd) {
            pass = false;
            print("FAIL invalid fog range");
        }
        List<String> diagnostics = CompatibilityDiagnostics.lines(this.mc.theWorld);
        boolean diagnosticsOk = diagnostics.size() >= 5 && diagnostics.stream()
            .anyMatch(line -> line.startsWith("Mods: "))
            && diagnostics.stream()
                .anyMatch(line -> line.startsWith("Fog: "))
            && diagnostics.stream()
                .anyMatch(line -> line.startsWith("NotFine: "));
        print("diagnostics: " + String.join(" | ", diagnostics));
        if (!diagnosticsOk) {
            pass = false;
            print("FAIL compatibility diagnostics are incomplete");
        }

        if (ModOptions.auroraEnable && this.auroraFrames == 0) {
            pass = false;
            print("FAIL aurora was enabled but never rendered");
        }
        if (EXPECTED_NEAR_FOG_PERCENT > 0.0F && this.fogEnd > 0.0F) {
            float actual = this.fogStart / this.fogEnd * 100.0F;
            if (actual > EXPECTED_NEAR_FOG_PERCENT + 1.0F) {
                pass = false;
                print("FAIL fog near=" + actual + "% expected <= " + EXPECTED_NEAR_FOG_PERCENT + "%");
            }
        }

        for (String line : newLogLines()) {
            String lower = line.toLowerCase();
            if (lower.contains("mixin apply failed") || lower.contains("mixintransformererror")
                || lower.contains("exception in thread")
                || lower.contains("unexpected error")) {
                pass = false;
                print("FAIL log: " + line.trim());
            }
        }
        return pass;
    }

    private List<String> newLogLines() {
        File log = new File(this.mc.mcDataDir, "logs/latest.log");
        if (!log.isFile()) {
            log = new File("logs/latest.log");
        }
        if (!log.isFile()) {
            return java.util.Collections.emptyList();
        }
        try {
            byte[] bytes = Files.readAllBytes(log.toPath());
            if (bytes.length <= this.logOffset) {
                return java.util.Collections.emptyList();
            }
            String text = new String(
                bytes,
                (int) this.logOffset,
                bytes.length - (int) this.logOffset,
                StandardCharsets.UTF_8);
            return java.util.Arrays.asList(text.split("\r?\n"));
        } catch (IOException e) {
            print("FAIL unable to read latest.log: " + e);
            return java.util.Collections.singletonList("Exception in thread smoke log reader");
        }
    }

    private long logLength() {
        File log = new File(this.mc.mcDataDir, "logs/latest.log");
        if (!log.isFile()) {
            log = new File("logs/latest.log");
        }
        return log.isFile() ? log.length() : 0L;
    }

    private void finish(boolean pass, String detail) {
        if (this.finished) {
            return;
        }
        this.finished = true;
        print((pass ? "PASS " : "FAIL ") + detail + " frames=" + this.frames + " auroraFrames=" + this.auroraFrames);
        FMLCommonHandler.instance()
            .exitJava(pass ? 0 : 1, false);
    }

    private static void print(String message) {
        System.out.println(PREFIX + " " + message);
    }
}
