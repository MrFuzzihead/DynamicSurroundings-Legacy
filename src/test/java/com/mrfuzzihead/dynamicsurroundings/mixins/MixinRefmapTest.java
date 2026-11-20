package com.mrfuzzihead.dynamicsurroundings.mixins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The refmap is what makes the mixins work against an obfuscated production jar; in a dev launch the names are already
 * MCP and a broken refmap goes unnoticed until a release is installed. The annotation processor writes it during
 * compilation, so this test just pins that the expected entries are present and that the Forge-added method is kept
 * out of it (it must stay literal).
 */
public class MixinRefmapTest {

    @Test
    public void refmapMapsEveryMappedMixinMember() throws Exception {
        JsonObject mappings = mappings();
        assertEquals(
            "Lnet/minecraft/client/renderer/EntityRenderer;func_78484_h()V",
            member(mappings, "EntityRendererMixin", "addRainParticles"));
        assertEquals(
            "Lnet/minecraft/client/renderer/EntityRenderer;func_78474_d(F)V",
            member(mappings, "EntityRendererMixin", "renderRainSnow"));
        assertEquals(
            "Lnet/minecraft/client/audio/SoundManager;func_148612_a(Lnet/minecraft/util/ResourceLocation;)Ljava/net/URL;",
            member(mappings, "SoundManagerMixin", "getURLForSoundResource"));
        assertEquals(
            "Lnet/minecraft/client/audio/SoundManager;func_148606_a(Lnet/minecraft/client/audio/ISound;Lnet/minecraft/client/audio/SoundPoolEntry;)F",
            member(mappings, "SoundManagerMixin", "getNormalizedPitch"));
        assertEquals(
            "Lnet/minecraft/client/audio/SoundManager;func_148594_a(Lnet/minecraft/client/audio/ISound;Lnet/minecraft/client/audio/SoundPoolEntry;Lnet/minecraft/client/audio/SoundCategory;)F",
            member(mappings, "SoundManagerMixin", "getNormalizedVolume"));
        assertEquals(
            "Lnet/minecraft/client/audio/SoundManager;func_148611_c(Lnet/minecraft/client/audio/ISound;)V",
            member(mappings, "SoundManagerMixin", "playSound"));
        assertEquals(
            "Lnet/minecraft/world/WorldServer;func_73051_P()V",
            member(mappings, "WorldServerMixin", "resetRainAndThunder"));
    }

    @Test
    public void refmapMapsEveryAccessorField() throws Exception {
        JsonObject mappings = mappings();
        assertEquals("field_70150_b:I", accessor(mappings, "EntityAccessor", "nextStepDistance"));
        assertEquals("field_70703_bu:Z", accessor(mappings, "EntityLivingBaseAccessor", "isJumping"));
        assertEquals("field_78529_t:I", accessor(mappings, "EntityRendererAccessor", "rendererUpdateCount"));
        assertEquals("field_78534_ac:I", accessor(mappings, "EntityRendererAccessor", "rainSoundCounter"));
        assertEquals("field_70562_a:F", accessor(mappings, "EntityFlameFXAccessor", "flameScale"));
        assertEquals(
            "field_147694_f:Lnet/minecraft/client/audio/SoundManager;",
            accessor(mappings, "SoundHandlerAccessor", "sndManager"));
        assertEquals(
            "field_147697_e:Lnet/minecraft/client/audio/SoundRegistry;",
            accessor(mappings, "SoundHandlerAccessor", "sndRegistry"));
        assertEquals("field_148629_h:Ljava/util/Map;", accessor(mappings, "SoundManagerAccessor", "playingSounds"));
        assertEquals("field_148626_m:Ljava/util/Map;", accessor(mappings, "SoundManagerAccessor", "delayedSounds"));
    }

    @Test
    public void forgeAddedMethodIsNotRemapped() {
        // World.updateWeatherBody is added by Forge and has no srg name; the mixin sets remap = false for it.
        assertFalse(
            mappings().toString(),
            mappings().toString()
                .contains("updateWeatherBody"));
    }

    private static JsonObject mappings() {
        InputStream stream = MixinRefmapTest.class.getResourceAsStream("/mixins.dynamicsurroundings.refmap.json");
        assertNotNull("the mixin refmap must be on the classpath", stream);
        JsonObject root = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8))
            .getAsJsonObject();
        JsonObject mappings = root.getAsJsonObject("mappings");
        assertNotNull(mappings);
        return mappings;
    }

    private static String member(JsonObject mappings, String mixin, String member) {
        JsonObject entry = mappings.getAsJsonObject("com/mrfuzzihead/dynamicsurroundings/mixins/early/" + mixin);
        assertNotNull("no refmap entry for " + mixin, entry);
        assertTrue(mixin + "." + member, entry.has(member));
        return entry.get(member)
            .getAsString();
    }

    private static String accessor(JsonObject mappings, String mixin, String field) {
        return member(mappings, "accessors/" + mixin, field);
    }
}
