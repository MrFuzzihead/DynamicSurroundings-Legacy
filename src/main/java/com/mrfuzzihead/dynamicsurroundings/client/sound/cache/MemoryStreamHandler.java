package org.blockartistry.mod.DynSurround.client.sound.cache;

import java.net.URL;
import java.net.URLConnection;

import javax.annotation.Nonnull;

import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MemoryStreamHandler extends SoundStreamHandler {

    protected final byte[] buffer;

    public MemoryStreamHandler(@Nonnull ResourceLocation resource, @Nonnull byte[] buffer) {
        super(resource);
        this.buffer = buffer;
    }

    @Override
    protected URLConnection createConnection(@Nonnull URL url) {
        return new MemoryURLConnection(url, this.buffer);
    }
}
