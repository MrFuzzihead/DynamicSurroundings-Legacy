package com.mrfuzzihead.dynamicsurroundings.compat.angelica;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.compat.Compat;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Guarded entry point for the Angelica debug-screen entry. */
@SideOnly(Side.CLIENT)
public final class AngelicaDebugEntries {

    private AngelicaDebugEntries() {}

    public static void register() {
        if (!Compat.isAngelicaLoaded()) {
            return;
        }

        try {
            AngelicaDebugHook.register();
        } catch (LinkageError | RuntimeException e) {
            ModLog.debug("Angelica debug entries unavailable: %s", e.toString());
        }
    }
}
