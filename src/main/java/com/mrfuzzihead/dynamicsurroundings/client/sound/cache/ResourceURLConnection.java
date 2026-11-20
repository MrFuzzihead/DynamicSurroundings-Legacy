package com.mrfuzzihead.dynamicsurroundings.client.sound.cache;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ResourceURLConnection extends URLConnection {

    protected final ResourceLocation resource;

    protected ResourceURLConnection(@Nonnull URL url, @Nonnull ResourceLocation resource) {
        super(url);
        this.resource = resource;
    }

    @Override
    public void connect() throws IOException {}

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream stream = Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(this.resource)
            .getInputStream();
        return new SoundInputStream(stream);
    }
}
