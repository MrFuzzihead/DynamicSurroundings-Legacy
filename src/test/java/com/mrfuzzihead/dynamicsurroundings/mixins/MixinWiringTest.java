package com.mrfuzzihead.dynamicsurroundings.mixins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Scanner;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundPoolEntry;
import net.minecraft.client.particle.EntityFlameFX;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import org.junit.Test;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrfuzzihead.dynamicsurroundings.mixins.early.EntityRendererMixin;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.SoundManagerMixin;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.WorldMixin;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.WorldServerMixin;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors.EntityAccessor;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors.EntityFlameFXAccessor;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors.EntityLivingBaseAccessor;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors.EntityRendererAccessor;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors.SoundHandlerAccessor;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors.SoundManagerAccessor;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.paulscode.LibraryMixin;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.paulscode.StreamThreadMixin;

import paulscode.sound.Library;
import paulscode.sound.StreamThread;

/**
 * Pins the mixin wiring against the actual Minecraft/paulscode classes on the test classpath.
 *
 * <p>
 * The mixin annotation processor already validates member names while compiling, which is most of the safety net.
 * This test covers what the processor does not check and what a mapping or library bump would silently break: the
 * injector annotations still name the right member, the accessors still match the field types, and the vanilla
 * signatures the mixins rely on still exist. Failing here is much cheaper than finding out after a manual game launch.
 * </p>
 */
public class MixinWiringTest {

    // ---------------------------------------------------------------------------------------------------------
    // Injector declarations
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void weatherMixinHooksTheVanillaWeatherMethods() throws Exception {
        Inject world = WorldMixin.class.getDeclaredMethod("dynamicsurroundings$updateWeatherBody", CallbackInfo.class)
            .getAnnotation(Inject.class);
        assertNotNull(world);
        assertEquals("updateWeatherBody", world.method()[0]);
        assertFalse(world.remap());
        assertTrue(world.cancellable());
        assertEquals(1, world.require());
        assertAt("HEAD", world.at()[0]);

        Inject sleep = WorldServerMixin.class
            .getDeclaredMethod("dynamicsurroundings$resetRainAndThunder", CallbackInfo.class)
            .getAnnotation(Inject.class);
        assertNotNull(sleep);
        assertEquals("resetRainAndThunder", sleep.method()[0]);
        assertTrue(sleep.cancellable());

        Inject splashes = EntityRendererMixin.class
            .getDeclaredMethod("dynamicsurroundings$addRainParticles", CallbackInfo.class)
            .getAnnotation(Inject.class);
        assertNotNull(splashes);
        assertEquals("addRainParticles", splashes.method()[0]);
        assertTrue(splashes.cancellable());

        Inject rainSnow = EntityRendererMixin.class
            .getDeclaredMethod("dynamicsurroundings$renderRainSnow", float.class, CallbackInfo.class)
            .getAnnotation(Inject.class);
        assertNotNull(rainSnow);
        assertEquals("renderRainSnow", rainSnow.method()[0]);
        assertFalse(rainSnow.cancellable());
    }

    @Test
    public void soundMixinHooksTheVanillaSoundMethods() throws Exception {
        Inject url = SoundManagerMixin.class
            .getDeclaredMethod(
                "dynamicsurroundings$getURLForSoundResource",
                ResourceLocation.class,
                CallbackInfoReturnable.class)
            .getAnnotation(Inject.class);
        assertNotNull(url);
        assertEquals("getURLForSoundResource", url.method()[0]);
        assertTrue(url.cancellable());

        Inject pitch = SoundManagerMixin.class
            .getDeclaredMethod(
                "dynamicsurroundings$getNormalizedPitch",
                ISound.class,
                SoundPoolEntry.class,
                CallbackInfoReturnable.class)
            .getAnnotation(Inject.class);
        assertNotNull(pitch);
        assertEquals("getNormalizedPitch", pitch.method()[0]);
        assertTrue(pitch.cancellable());

        Inject volume = SoundManagerMixin.class
            .getDeclaredMethod(
                "dynamicsurroundings$getNormalizedVolume",
                ISound.class,
                SoundPoolEntry.class,
                SoundCategory.class,
                CallbackInfoReturnable.class)
            .getAnnotation(Inject.class);
        assertNotNull(volume);
        assertEquals("getNormalizedVolume", volume.method()[0]);
        assertTrue(volume.cancellable());

        Method playTime = SoundManagerMixin.class.getDeclaredMethod("dynamicsurroundings$zeroPlayTime", int.class);
        ModifyConstant modifyConstant = playTime.getAnnotation(ModifyConstant.class);
        assertNotNull(modifyConstant);
        assertEquals("playSound", modifyConstant.method()[0]);
        Constant[] constants = modifyConstant.constant();
        assertEquals(1, constants.length);
        assertEquals(20, constants[0].intValue());

        Redirect flush = SoundManagerMixin.class
            .getDeclaredMethod("dynamicsurroundings$playAndFlush", paulscode.sound.SoundSystem.class, String.class)
            .getAnnotation(Redirect.class);
        assertNotNull(flush);
        assertEquals("playSound", flush.method()[0]);
        assertFalse(
            flush.at()
                .remap());
        assertTrue(
            flush.at()
                .target()
                .contains("SoundManager$SoundSystemStarterThread;play"));
    }

    @Test
    public void paulscodeMixinsRedirectTheCrashingCalls() throws Exception {
        Redirect cleanup = LibraryMixin.class
            .getDeclaredMethod("dynamicsurroundings$cleanupSource", paulscode.sound.Source.class)
            .getAnnotation(Redirect.class);
        assertNotNull(cleanup);
        assertEquals("removeSource", cleanup.method()[0]);
        assertTrue(
            cleanup.at()
                .target()
                .contains("Source;cleanup()V"));

        Redirect next = StreamThreadMixin.class
            .getDeclaredMethod("dynamicsurroundings$removeSource", java.util.ListIterator.class)
            .getAnnotation(Redirect.class);
        assertNotNull(next);
        assertEquals("run", next.method()[0]);
        assertTrue(
            next.at()
                .target()
                .contains("ListIterator;next()"));
    }

    private static void assertAt(String value, At at) {
        assertEquals(value, at.value());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Accessor declarations (these replaced the access transformer)
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void accessorsNameTheVanillaFields() throws Exception {
        assertAccessor(EntityAccessor.class, "dynamicsurroundings$getNextStepDistance", "nextStepDistance");
        assertAccessor(EntityAccessor.class, "dynamicsurroundings$setNextStepDistance", "nextStepDistance");
        assertAccessor(EntityLivingBaseAccessor.class, "dynamicsurroundings$isJumping", "isJumping");
        assertAccessor(
            EntityRendererAccessor.class,
            "dynamicsurroundings$getRendererUpdateCount",
            "rendererUpdateCount");
        assertAccessor(EntityRendererAccessor.class, "dynamicsurroundings$getRainSoundCounter", "rainSoundCounter");
        assertAccessor(EntityRendererAccessor.class, "dynamicsurroundings$setRainSoundCounter", "rainSoundCounter");
        assertAccessor(EntityFlameFXAccessor.class, "dynamicsurroundings$getFlameScale", "flameScale");
        assertAccessor(EntityFlameFXAccessor.class, "dynamicsurroundings$setFlameScale", "flameScale");
        assertAccessor(SoundHandlerAccessor.class, "dynamicsurroundings$getSndManager", "sndManager");
        assertAccessor(SoundHandlerAccessor.class, "dynamicsurroundings$getSndRegistry", "sndRegistry");
        assertAccessor(SoundManagerAccessor.class, "dynamicsurroundings$getPlayingSounds", "playingSounds");
        assertAccessor(SoundManagerAccessor.class, "dynamicsurroundings$getDelayedSounds", "delayedSounds");
    }

    private static void assertAccessor(Class<?> accessor, String method, String expectedField) throws Exception {
        for (Method candidate : accessor.getDeclaredMethods()) {
            if (!candidate.getName()
                .equals(method)) {
                continue;
            }
            Accessor annotation = candidate.getAnnotation(Accessor.class);
            assertNotNull("no @Accessor on " + accessor.getSimpleName() + "." + method, annotation);
            assertEquals(expectedField, annotation.value());
            return;
        }
        throw new AssertionError("no method " + method + " on " + accessor.getName());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Vanilla signatures the mixins and accessors depend on
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void vanillaWeatherSignaturesAreStillPresent() throws Exception {
        assertTrue(methodExists(EntityRenderer.class, "addRainParticles"));
        assertTrue(methodExists(EntityRenderer.class, "renderRainSnow", float.class));
        assertTrue(methodExists(World.class, "updateWeatherBody"));
        assertTrue(methodExists(WorldServer.class, "resetRainAndThunder"));
    }

    @Test
    public void vanillaSoundSignaturesAreStillPresent() throws Exception {
        assertTrue(methodExists(SoundManager.class, "getURLForSoundResource", ResourceLocation.class));
        assertTrue(methodExists(SoundManager.class, "getNormalizedPitch", ISound.class, SoundPoolEntry.class));
        assertTrue(
            methodExists(
                SoundManager.class,
                "getNormalizedVolume",
                ISound.class,
                SoundPoolEntry.class,
                SoundCategory.class));
        assertTrue(methodExists(SoundManager.class, "playSound", ISound.class));
        assertTrue(methodExists(Library.class, "removeSource", String.class));
        assertTrue(methodExists(StreamThread.class, "run"));
    }

    @Test
    public void vanillaFieldsStillHaveTheTypesTheAccessorsDeclare() throws Exception {
        assertFieldType(Entity.class, "nextStepDistance", int.class);
        assertFieldType(EntityLivingBase.class, "isJumping", boolean.class);
        assertFieldType(EntityRenderer.class, "rendererUpdateCount", int.class);
        assertFieldType(EntityRenderer.class, "rainSoundCounter", int.class);
        assertFieldType(EntityFlameFX.class, "flameScale", float.class);
        assertFieldType(SoundHandler.class, "sndManager", SoundManager.class);
        assertFieldType(SoundHandler.class, "sndRegistry", net.minecraft.client.audio.SoundRegistry.class);
        assertFieldType(SoundManager.class, "playingSounds", Map.class);
        assertFieldType(SoundManager.class, "delayedSounds", Map.class);
    }

    private static boolean methodExists(Class<?> owner, String name, Class<?>... args) {
        try {
            owner.getDeclaredMethod(name, args);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static void assertFieldType(Class<?> owner, String name, Class<?> expected) throws Exception {
        Field field = owner.getDeclaredField(name);
        assertSame(name, expected, field.getType());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Catalogue / shipped config
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void catalogueKeepsTheFourFeatureGroups() {
        assertEquals(4, Mixins.values().length);
        assertNotNull(Mixins.valueOf("ACCESSORS"));
        assertNotNull(Mixins.valueOf("WEATHER"));
        assertNotNull(Mixins.valueOf("SOUND"));
        assertNotNull(Mixins.valueOf("SOUND_CRASH_FIX"));
        for (Mixins entry : Mixins.values()) {
            assertNotNull(entry.name(), entry.getBuilder());
        }
    }

    @Test
    public void earlyMixinConfigIsShipped() {
        InputStream stream = getClass().getResourceAsStream("/mixins.dynamicsurroundings.early.json");
        assertNotNull("the early mixin config must be in the jar", stream);
        String json;
        try (Scanner scanner = new Scanner(stream).useDelimiter("\\A")) {
            json = scanner.hasNext() ? scanner.next() : "";
        }
        assertTrue(json, json.contains("\"package\": \"com.mrfuzzihead.dynamicsurroundings.mixins.early\""));
        assertTrue(json, json.contains("\"refmap\": \"mixins.dynamicsurroundings.refmap.json\""));
    }
}
