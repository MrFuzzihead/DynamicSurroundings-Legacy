package com.mrfuzzihead.dynamicsurroundings.client.sound.cache;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Drops memory-cached sound URLs when the resource manager reloads. */
@SideOnly(Side.CLIENT)
public final class SoundCacheReloadListener implements IResourceManagerReloadListener {

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        SoundCache.clearCache();
    }
}
