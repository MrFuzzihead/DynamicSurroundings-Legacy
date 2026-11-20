package com.mrfuzzihead.dynamicsurroundings.data;

import java.io.File;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import org.apache.commons.lang3.StringUtils;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.Module;
import com.mrfuzzihead.dynamicsurroundings.client.fx.BlockEffect;
import com.mrfuzzihead.dynamicsurroundings.client.fx.JetEffect;
import com.mrfuzzihead.dynamicsurroundings.client.sound.SoundEffect;
import com.mrfuzzihead.dynamicsurroundings.data.config.BlockConfig;
import com.mrfuzzihead.dynamicsurroundings.data.config.SoundConfig;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameData;

public final class BlockRegistry {

    private static final Map<Block, BlockRegistry.Entry> registry = new IdentityHashMap<>();

    public static void initialize() {
        registry.clear();
        processConfig();
        if (ModOptions.enableDebugLogging) {
            ModLog.info("*** BLOCK REGISTRY ***");

            for (BlockRegistry.Entry entry : registry.values()) {
                ModLog.info(entry.toString());
            }
        }
    }

    public static List<BlockEffect> getEffects(Block block) {
        BlockRegistry.Entry entry = registry.get(block);
        return entry != null ? entry.effects : null;
    }

    private static SoundEffect getRandomSound(List<SoundEffect> list, Random random, String conditions) {
        int totalWeight = 0;
        List<SoundEffect> candidates = new ArrayList<>();

        for (SoundEffect s : list) {
            if (s.matches(conditions)) {
                candidates.add(s);
                totalWeight += s.weight;
            }
        }

        if (totalWeight <= 0) {
            return null;
        } else if (candidates.size() == 1) {
            return candidates.get(0);
        } else {
            int targetWeight = random.nextInt(totalWeight);
            int i = 0;
            i = candidates.size();

            while ((targetWeight -= candidates.get(i - 1).weight) >= 0) {
                i--;
            }

            return candidates.get(i - 1);
        }
    }

    public static SoundEffect getSound(Block block, Random random, String conditions) {
        BlockRegistry.Entry entry = registry.get(block);
        return entry != null && !entry.sounds.isEmpty() && random.nextInt(entry.chance) == 0
            ? getRandomSound(entry.sounds, random, conditions)
            : null;
    }

    public static SoundEffect getStepSound(Block block, Random random, String conditions) {
        BlockRegistry.Entry entry = registry.get(block);
        return entry != null && !entry.stepSounds.isEmpty() && random.nextInt(entry.stepChance) == 0
            ? getRandomSound(entry.stepSounds, random, conditions)
            : null;
    }

    private static void processConfig() {
        try {
            process(BlockConfig.load("blocks"));
        } catch (Exception var9) {
            var9.printStackTrace();
        }

        for (ModContainer mod : Loader.instance()
            .getActiveModList()) {
            try {
                process(BlockConfig.load(mod.getModId() + "_blocks"));
            } catch (Exception var8) {
                var8.printStackTrace();
            }
        }

        String[] configFiles = ModOptions.blockConfigFiles;

        for (String file : configFiles) {
            File theFile = new File(Module.dataDirectory(), file);
            if (theFile.exists()) {
                try {
                    BlockConfig config = BlockConfig.load(theFile);
                    if (config != null) {
                        process(config);
                    } else {
                        ModLog.warn("Unable to process block config file " + file);
                    }
                } catch (Exception var7) {
                    ModLog.error("Unable to process block config file " + file, var7);
                }
            } else {
                ModLog.warn("Could not locate block config file [%s]", file);
            }
        }
    }

    private static void process(BlockConfig config) {
        for (BlockConfig.Entry entry : config.entries) {
            if (!entry.blocks.isEmpty()) {
                for (String blockName : entry.blocks) {
                    Block block = (Block) GameData.getBlockRegistry()
                        .getObject(blockName);
                    if (block != null && block != Blocks.air) {
                        BlockRegistry.Entry blockData = registry.get(block);
                        if (blockData == null) {
                            blockData = new BlockRegistry.Entry(block);
                            registry.put(block, blockData);
                        }

                        if (entry.soundReset != null && entry.soundReset) {
                            blockData.sounds.clear();
                        }

                        if (entry.stepSoundReset != null && entry.stepSoundReset) {
                            blockData.stepSounds.clear();
                        }

                        if (entry.effectReset != null && entry.effectReset) {
                            blockData.effects.clear();
                        }

                        if (entry.chance != null) {
                            blockData.chance = entry.chance;
                        }

                        if (entry.stepChance != null) {
                            blockData.stepChance = entry.stepChance;
                        }

                        for (SoundConfig sr : entry.sounds) {
                            if (sr.sound != null && !SoundRegistry.isSoundBlocked(sr.sound)) {
                                SoundEffect eff = new SoundEffect(sr);
                                if (eff.type == SoundEffect.SoundType.STEP) {
                                    blockData.stepSounds.add(eff);
                                } else {
                                    blockData.sounds.add(eff);
                                }
                            }
                        }

                        for (BlockConfig.Effect e : entry.effects) {
                            if (!StringUtils.isEmpty(e.effect)) {
                                BlockEffect blockEffect = null;
                                int chance = e.chance != null ? e.chance : 100;
                                if (StringUtils.equalsIgnoreCase("steam", e.effect)) {
                                    blockEffect = new JetEffect.Steam(chance);
                                } else if (StringUtils.equalsIgnoreCase("fire", e.effect)) {
                                    blockEffect = new JetEffect.Fire(chance);
                                } else if (StringUtils.equalsIgnoreCase("bubble", e.effect)) {
                                    blockEffect = new JetEffect.Bubble(chance);
                                } else if (StringUtils.equalsIgnoreCase("dust", e.effect)) {
                                    blockEffect = new JetEffect.Dust(chance);
                                } else {
                                    if (!StringUtils.equalsIgnoreCase("fountain", e.effect)) {
                                        ModLog.warn("Unknown effect type in config: '%s'", e.effect);
                                        continue;
                                    }

                                    blockEffect = new JetEffect.Fountain(chance);
                                }

                                blockData.effects.add(blockEffect);
                            }
                        }
                    } else {
                        ModLog.warn("Unknown block [%s] in block config file", blockName);
                    }
                }
            }
        }
    }

    private static final class Entry {

        public final Block block;
        public int chance = 100;
        public int stepChance = 100;
        public final List<SoundEffect> sounds = new ArrayList<>();
        public final List<SoundEffect> stepSounds = new ArrayList<>();
        public final List<BlockEffect> effects = new ArrayList<>();

        public Entry(Block block) {
            this.block = block;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("Block [%s]:", this.block.getUnlocalizedName()));
            if (!this.sounds.isEmpty()) {
                builder.append(" chance:")
                    .append(this.chance);
                builder.append("; sounds [");

                for (SoundEffect sound : this.sounds) {
                    builder.append(sound.toString())
                        .append(',');
                }

                builder.append(']');
            }

            if (!this.stepSounds.isEmpty()) {
                builder.append(" chance:")
                    .append(this.stepChance);
                builder.append("; step sounds [");

                for (SoundEffect sound : this.stepSounds) {
                    builder.append(sound.toString())
                        .append(',');
                }

                builder.append(']');
            }

            if (!this.effects.isEmpty()) {
                builder.append("; effects [");

                for (BlockEffect effect : this.effects) {
                    builder.append(effect.toString())
                        .append(',');
                }

                builder.append(']');
            }

            return builder.toString();
        }
    }
}
