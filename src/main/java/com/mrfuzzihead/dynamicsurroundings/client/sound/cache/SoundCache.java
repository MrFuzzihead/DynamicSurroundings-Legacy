package com.mrfuzzihead.dynamicsurroundings.client.sound.cache;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import com.mrfuzzihead.dynamicsurroundings.ModLog;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class SoundCache {

    private static final int BUFFER_SIZE = 65536;
    private static final ThreadLocal<byte[]> BUFFER = new ThreadLocal<byte[]>() {

        @Override
        protected byte[] initialValue() {
            return new byte[BUFFER_SIZE];
        }
    };
    private static final ResourceLocation SILENCE_RESOURCE = new ResourceLocation(
        "dynamicsurroundings:sounds/ambient/silence.ogg");
    private static final Map<ResourceLocation, URL> cache = new ConcurrentHashMap<>(256);
    private static final byte[] SILENCE = getBuffer(SILENCE_RESOURCE);
    private static final URL SILENCE_URL = silenceUrl();

    private static byte[] getBuffer(@Nonnull ResourceLocation resource) {
        InputStream stream = null;

        try {
            IResourceManager manager = Minecraft.getMinecraft()
                .getResourceManager();
            stream = manager.getResource(resource)
                .getInputStream();
            if (stream == null) {
                ModLog.warn("No stream returned for [%s]", resource.toString());
                return SILENCE;
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = BUFFER.get();
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                if (output.size() + bytesRead >= BUFFER_SIZE) {
                    return null;
                }

                output.write(buffer, 0, bytesRead);
            }

            return output.size() == 0 ? null : output.toByteArray();
        } catch (Throwable var16) {
            ModLog.warn("Error reading stream [%s]", resource.toString());
            return SILENCE;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Throwable var15) {}
            }
        }
    }

    private static URL load(@Nonnull ResourceLocation key) throws Exception {
        byte[] buffer = getBuffer(key);
        SoundStreamHandler handler;
        if (buffer == null) {
            handler = new SoundStreamHandler(key);
        } else {
            handler = new MemoryStreamHandler(key, buffer);
        }

        return new URL((URL) null, handler.getSpec(), handler);
    }

    private SoundCache() {}

    public static void clearCache() {
        cache.clear();
    }

    private static URL silenceUrl() {
        try {
            SoundStreamHandler handler = new MemoryStreamHandler(SILENCE_RESOURCE, SILENCE);
            return new URL((URL) null, handler.getSpec(), handler);
        } catch (Exception e) {
            ModLog.error("Unable to build the silence sound URL", e);
            return null;
        }
    }

    public static URL getURLForSoundResource(@Nonnull ResourceLocation soundResource) {
        URL result = cache.get(soundResource);
        if (result == null) {
            try {
                result = load(soundResource);
                cache.put(soundResource, result);
            } catch (Throwable var3) {
                ModLog.error("Unable to load sound from cache: " + soundResource, var3);
                return SILENCE_URL;
            }
        }

        return result;
    }
}
