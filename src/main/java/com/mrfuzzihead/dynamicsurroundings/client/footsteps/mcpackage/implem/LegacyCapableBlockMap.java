package com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.implem;

import net.minecraft.block.Block;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.compat.MCHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LegacyCapableBlockMap extends BasicBlockMap {

    static int findLegacySeparator(String key) {
        int colon = key.indexOf(':');
        if (colon >= 0) {
            return colon;
        }

        int caret = key.indexOf('^');
        if (caret >= 0) {
            return caret;
        }

        int dot = key.indexOf('.');
        return dot >= 0 ? dot : key.length();
    }

    @Override
    public void register(String key, String value) {
        try {
            int endOfNumber = findLegacySeparator(key);

            String number = key.substring(0, endOfNumber);
            int id = Integer.parseInt(number);
            Object o = Block.blockRegistry.getObjectById(id);
            if (o != null && o instanceof Block) {
                String fullKeyRebuild = MCHelper.nameOf((Block) o)
                    + (endOfNumber == key.length() ? "" : key.substring(endOfNumber));
                super.register(fullKeyRebuild, value);
                ModLog.debug("Adding legacy key: " + fullKeyRebuild + " for " + key);
            } else {
                super.register(key, value);
            }
        } catch (NumberFormatException var8) {
            super.register(key, value);
        }
    }
}
