package com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.implem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import org.apache.commons.lang3.StringUtils;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces.IBlockMap;
import com.mrfuzzihead.dynamicsurroundings.compat.MCHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BasicBlockMap implements IBlockMap {

    private static final Pattern pattern = Pattern.compile("([^:]+:[^^+]+)\\^?(\\d+)?\\+?(\\w+)?");
    private final BlockInfo.BlockInfoMutable mutable = new BlockInfo.BlockInfoMutable();
    private final Map<BlockInfo, String> metaMap = new HashMap<>();
    private final Map<Substrate, Map<BlockInfo, String>> substrateMap = new EnumMap<>(Substrate.class);
    private static final Map<String, List<BasicBlockMap.MacroEntry>> macros = new LinkedHashMap<>();

    public BasicBlockMap() {
        this.put(Blocks.air, -1, null, "NOT_EMITTER");
    }

    @Override
    public String getBlockMap(Block block, int meta) {
        this.mutable.setBlock(block)
            .setMeta(meta);
        String acoustic = this.metaMap.get(this.mutable);
        if (acoustic == null) {
            acoustic = this.metaMap.get(this.mutable.asGeneric());
        }

        return acoustic;
    }

    @Override
    public String getBlockMapSubstrate(Block block, int meta, Substrate substrate) {
        Map<BlockInfo, String> sub = this.substrateMap.get(substrate);
        if (sub != null) {
            this.mutable.setBlock(block)
                .setMeta(meta);
            String result = sub.get(this.mutable);
            if (result == null) {
                result = sub.get(this.mutable.asGeneric());
            }

            return result;
        } else {
            return null;
        }
    }

    private void put(Block block, int meta, String substrate, String value) {
        BlockInfo info = new BlockInfo(block, meta);
        if (StringUtils.isEmpty(substrate)) {
            this.metaMap.put(info, value);
        } else {
            Substrate s = Substrate.get(substrate);
            Map<BlockInfo, String> sub = this.substrateMap.get(s);
            if (sub == null) {
                this.substrateMap.put(s, sub = new HashMap<>());
            }

            sub.put(info, value);
        }
    }

    private void expand(Block block, String value) {
        List<BasicBlockMap.MacroEntry> macro = macros.get(value);
        if (macro != null) {
            for (BasicBlockMap.MacroEntry entry : macro) {
                this.put(block, entry.meta, entry.substrate, entry.value);
            }
        } else {
            ModLog.debug("Unknown macro '%s'", value);
        }
    }

    @Override
    public void register(String key, String value) {
        Matcher matcher = pattern.matcher(key);
        if (matcher.matches()) {
            String blockName = matcher.group(1);
            Block block = MCHelper.getBlockNameRaw(blockName);
            if (block != null && block != Blocks.air) {
                int meta = matcher.group(2) == null ? -1 : Integer.parseInt(matcher.group(2));
                String substrate = matcher.group(3);
                if (value.startsWith("#")) {
                    this.expand(block, value);
                } else {
                    this.put(block, meta, substrate, value);
                }
            } else {
                ModLog.debug("Unable to locate block for blockmap '%s'", blockName);
            }
        } else {
            ModLog.debug("Malformed key in blockmap '%s'", key);
        }
    }

    @Override
    public void collectData(Block block, int meta, List<String> data) {}

    static {
        List<BasicBlockMap.MacroEntry> entries = new ArrayList<>();
        entries.add(new BasicBlockMap.MacroEntry(null, "NOT_EMITTER"));
        entries.add(new BasicBlockMap.MacroEntry("messy", "MESSY_GROUND"));
        entries.add(new BasicBlockMap.MacroEntry("foliage", "straw"));
        macros.put("#sapling", entries);
        macros.put("#reed", entries);
        entries = new ArrayList<>();
        entries.add(new BasicBlockMap.MacroEntry(null, "NOT_EMITTER"));
        entries.add(new BasicBlockMap.MacroEntry("messy", "MESSY_GROUND"));
        entries.add(new BasicBlockMap.MacroEntry(0, "foliage", "NOT_EMITTER"));
        entries.add(new BasicBlockMap.MacroEntry(1, "foliage", "NOT_EMITTER"));
        entries.add(new BasicBlockMap.MacroEntry(2, "foliage", "brush"));
        entries.add(new BasicBlockMap.MacroEntry(3, "foliage", "brush"));
        entries.add(new BasicBlockMap.MacroEntry(4, "foliage", "brush_straw_transition"));
        entries.add(new BasicBlockMap.MacroEntry(5, "foliage", "brush_straw_transition"));
        entries.add(new BasicBlockMap.MacroEntry(6, "foliage", "straw"));
        entries.add(new BasicBlockMap.MacroEntry(7, "foliage", "straw"));
        macros.put("#wheat", entries);
        List<BasicBlockMap.MacroEntry> var2 = new ArrayList();
        var2.add(new BasicBlockMap.MacroEntry(null, "NOT_EMITTER"));
        var2.add(new BasicBlockMap.MacroEntry("messy", "MESSY_GROUND"));
        var2.add(new BasicBlockMap.MacroEntry(0, "foliage", "NOT_EMITTER"));
        var2.add(new BasicBlockMap.MacroEntry(1, "foliage", "NOT_EMITTER"));
        var2.add(new BasicBlockMap.MacroEntry(2, "foliage", "NOT_EMITTER"));
        var2.add(new BasicBlockMap.MacroEntry(3, "foliage", "NOT_EMITTER"));
        var2.add(new BasicBlockMap.MacroEntry(4, "foliage", "brush"));
        var2.add(new BasicBlockMap.MacroEntry(5, "foliage", "brush"));
        var2.add(new BasicBlockMap.MacroEntry(6, "foliage", "brush"));
        var2.add(new BasicBlockMap.MacroEntry(7, "foliage", "brush"));
        macros.put("#crop", var2);
        List<BasicBlockMap.MacroEntry> var3 = new ArrayList();
        var3.add(new BasicBlockMap.MacroEntry("bigger", "bluntwood"));
        macros.put("#fence", var3);
    }

    private static class MacroEntry {

        public final int meta;
        public final String substrate;
        public final String value;

        public MacroEntry(String substrate, String value) {
            this(-1, substrate, value);
        }

        public MacroEntry(int meta, String substrate, String value) {
            this.meta = meta;
            this.substrate = substrate;
            this.value = value;
        }
    }
}
