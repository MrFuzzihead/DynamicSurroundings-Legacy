package com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository.Entry;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.Module;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PFResourcePackDealer {

    private final ResourceLocation pf_pack = new ResourceLocation("footsteps", "pf_pack.json");
    private final ResourceLocation acoustics = new ResourceLocation("footsteps", "acoustics.json");
    private final ResourceLocation blockmap = new ResourceLocation("footsteps", "blockmap.json");
    private final ResourceLocation primitivemap = new ResourceLocation("footsteps", "primitivemap.json");

    public List<IResourcePack> findResourcePacks() {
        List<Entry> repo = Minecraft.getMinecraft()
            .getResourcePackRepository()
            .getRepositoryEntries();
        List<IResourcePack> foundEntries = new ArrayList<>();
        foundEntries.add(new PFResourcePackDealer.DefaultPack());

        for (ModContainer mod : Loader.instance()
            .getActiveModList()) {
            foundEntries.add(new PFResourcePackDealer.DefaultPack(mod.getModId()));
        }

        for (Entry pack : repo) {
            ModLog.debug("Resource Pack: %s", pack.getResourcePackName());
            if (this.checkCompatible(pack)) {
                ModLog.debug("Found Footsteps resource pack: %s", pack.getResourcePackName());
                foundEntries.add(pack.getResourcePack());
            }
        }

        return foundEntries;
    }

    private boolean checkCompatible(Entry pack) {
        return pack.getResourcePack()
            .resourceExists(this.pf_pack);
    }

    public InputStream openPackDescriptor(IResourcePack pack) throws IOException {
        return pack.getInputStream(this.pf_pack);
    }

    public InputStream openAcoustics(IResourcePack pack) throws IOException {
        return pack.getInputStream(this.acoustics);
    }

    public InputStream openBlockMap(IResourcePack pack) throws IOException {
        return pack.getInputStream(this.blockmap);
    }

    public InputStream openPrimitiveMap(IResourcePack pack) throws IOException {
        return pack.getInputStream(this.primitivemap);
    }

    private static class DefaultPack implements IResourcePack {

        private final String mod;

        public DefaultPack() {
            this.mod = null;
        }

        public DefaultPack(String mod) {
            this.mod = mod;
        }

        public InputStream getInputStream(ResourceLocation loc) throws IOException {
            StringBuilder builder = new StringBuilder();
            builder.append("/assets/dynamicsurroundings/data/");
            builder.append(loc.getResourceDomain());
            builder.append('/');
            if (this.mod != null) {
                builder.append(this.mod)
                    .append('_');
            }

            builder.append(loc.getResourcePath());
            return Module.class.getResourceAsStream(builder.toString());
        }

        public boolean resourceExists(ResourceLocation loc) {
            return true;
        }

        public Set getResourceDomains() {
            return null;
        }

        public IMetadataSection getPackMetadata(IMetadataSerializer loc, String data) throws IOException {
            return null;
        }

        public BufferedImage getPackImage() throws IOException {
            return null;
        }

        public String getPackName() {
            return this.mod == null ? "DEFAULT" : "DEFAULT: " + this.mod;
        }
    }
}
