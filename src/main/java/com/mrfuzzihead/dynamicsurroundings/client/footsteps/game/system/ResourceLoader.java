package com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import net.minecraft.client.resources.IResourcePack;

import com.mrfuzzihead.dynamicsurroundings.ModLog;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Shared stream lifecycle for the footstep resource packs. */
@SideOnly(Side.CLIENT)
public final class ResourceLoader {

    public interface Opener {

        InputStream open(IResourcePack pack) throws IOException;
    }

    public interface Consumer {

        void accept(InputStream stream) throws Exception;
    }

    private ResourceLoader() {}

    public static void forEachPack(List<IResourcePack> packs, String type, Opener opener, Consumer consumer) {
        for (IResourcePack pack : packs) {
            try (InputStream stream = opener.open(pack)) {
                if (stream != null) {
                    consumer.accept(stream);
                }
            } catch (Exception e) {
                ModLog.debug("Unable to load %s data from pack %s", type, pack.getPackName());
            }
        }
    }
}
