package com.mrfuzzihead.dynamicsurroundings.data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.MinecraftForge;

import org.apache.commons.lang3.StringUtils;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.Module;
import com.mrfuzzihead.dynamicsurroundings.client.sound.SoundEffect;
import com.mrfuzzihead.dynamicsurroundings.compat.Compat;
import com.mrfuzzihead.dynamicsurroundings.data.compat.BOPFogIntegration;
import com.mrfuzzihead.dynamicsurroundings.data.compat.IBOPFog;
import com.mrfuzzihead.dynamicsurroundings.data.config.BiomeConfig;
import com.mrfuzzihead.dynamicsurroundings.data.config.SoundConfig;
import com.mrfuzzihead.dynamicsurroundings.event.RegistryReloadEvent;
import com.mrfuzzihead.dynamicsurroundings.util.MyUtils;
import com.mrfuzzihead.dynamicsurroundings.util.RgbColor;
import com.mrfuzzihead.dynamicsurroundings.util.WeightedRandom;

import gnu.trove.map.hash.TIntObjectHashMap;

public final class BiomeRegistry {

    private static volatile TIntObjectHashMap<BiomeRegistry.Entry> registry = new TIntObjectHashMap<>();
    private static volatile Map<String, String> biomeAliases = new HashMap<>();
    private static final Object INIT_LOCK = new Object();
    private static volatile boolean initialized;
    private static final boolean BOP_PRESENT = isClassPresent("biomesoplenty.BiomesOPlenty");
    public static final BiomeGenBase UNDERGROUND = new FakeBiome(-1, "Underground");
    public static final BiomeGenBase PLAYER = new FakeBiome(-2, "Player");
    public static final BiomeGenBase UNDERWATER = new FakeBiome(-3, "Underwater");
    public static final BiomeGenBase UNDEROCEAN = new FakeBiome(-4, "UnderOCN");
    public static final BiomeGenBase UNDERDEEPOCEAN = new FakeBiome(-5, "UnderDOCN");
    public static final BiomeGenBase UNDERRIVER = new FakeBiome(-6, "UnderRVR");
    public static final BiomeGenBase OUTERSPACE = new FakeBiome(-7, "OuterSpace");
    public static final BiomeGenBase CLOUDS = new FakeBiome(-8, "Clouds");
    public static final SoundEffect WATER_DRIP = new SoundEffect("dynamicsurroundings:waterdrops");
    private static final BiomeGenBase WTF = new FakeBiome(-256, "(FooBar)");

    /**
     * Optional-mod probe that also works in a headless unit test (FML's own {@code Loader} is not initialised
     * there). The class is resolved without running BOP's static initialisers, and no BOP type is referenced.
     */
    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, BiomeRegistry.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static String resolveName(BiomeGenBase biome) {
        if (biome == null) {
            return "(Bad Biome)";
        } else {
            return StringUtils.isEmpty(biome.biomeName) ? "" + '#' + biome.biomeID : biome.biomeName;
        }
    }

    public static void initialize() {
        Map<String, String> aliases = new HashMap<>();
        for (String entry : ModOptions.biomeAliases) {
            String[] parts = StringUtils.split(entry, "=");
            if (parts.length == 2) {
                aliases.put(parts[0], parts[1]);
            }
        }

        TIntObjectHashMap<BiomeRegistry.Entry> fresh = new TIntObjectHashMap<>();
        BiomeGenBase[] biomeArray = BiomeGenBase.getBiomeGenArray();
        for (BiomeGenBase biome : biomeArray) {
            if (biome != null) {
                fresh.put(biome.biomeID, new BiomeRegistry.Entry(biome));
            }
        }

        fresh.put(UNDERGROUND.biomeID, new BiomeRegistry.Entry(UNDERGROUND));
        fresh.put(UNDERWATER.biomeID, new BiomeRegistry.Entry(UNDERWATER));
        fresh.put(UNDEROCEAN.biomeID, new BiomeRegistry.Entry(UNDEROCEAN));
        fresh.put(UNDERDEEPOCEAN.biomeID, new BiomeRegistry.Entry(UNDERDEEPOCEAN));
        fresh.put(UNDERRIVER.biomeID, new BiomeRegistry.Entry(UNDERRIVER));
        fresh.put(OUTERSPACE.biomeID, new BiomeRegistry.Entry(OUTERSPACE));
        fresh.put(CLOUDS.biomeID, new BiomeRegistry.Entry(CLOUDS));
        fresh.put(PLAYER.biomeID, new BiomeRegistry.Entry(PLAYER));
        fresh.put(WTF.biomeID, new BiomeRegistry.Entry(WTF));
        biomeAliases = aliases;
        processConfig(fresh);

        synchronized (INIT_LOCK) {
            registry = fresh;
            initialized = true;
        }

        if (ModOptions.enableDebugLogging) {
            ModLog.info("*** BIOME REGISTRY ***");
            for (BiomeRegistry.Entry entryx : fresh.valueCollection()) {
                ModLog.info(entryx.toString());
            }
        }

        MinecraftForge.EVENT_BUS.post(new RegistryReloadEvent.Biome());
    }

    private static BiomeRegistry.Entry get(BiomeGenBase biome) {
        int id = biome == null ? WTF.biomeID : biome.biomeID;
        BiomeRegistry.Entry entry = registry.get(id);
        if (entry != null) {
            return entry;
        }

        synchronized (INIT_LOCK) {
            entry = registry.get(id);
            if (entry == null) {
                if (!initialized) {
                    initialize();
                    entry = registry.get(biome.biomeID);
                }

                if (entry == null) {
                    ModLog.warn(
                        "Biome [%s] was not detected during initial scan! Explicitly adding defaults",
                        resolveName(biome));
                    entry = new BiomeRegistry.Entry(biome);
                    registry.put(biome.biomeID, entry);
                }
            }
        }

        return entry;
    }

    public static boolean hasDust(BiomeGenBase biome) {
        return get(biome).hasDust;
    }

    public static boolean hasPrecipitation(BiomeGenBase biome) {
        return get(biome).hasPrecipitation;
    }

    public static boolean hasAurora(BiomeGenBase biome) {
        return get(biome).hasAurora;
    }

    public static boolean hasFog(BiomeGenBase biome) {
        return get(biome).hasFog;
    }

    public static RgbColor getDustColor(BiomeGenBase biome) {
        return get(biome).dustColor;
    }

    public static RgbColor getFogColor(BiomeGenBase biome) {
        return get(biome).fogColor;
    }

    public static float getFogDensity(BiomeGenBase biome) {
        return get(biome).fogDensity;
    }

    public static SoundEffect getSound(BiomeGenBase biome, String conditions) {
        return get(biome).findSoundMatch(conditions);
    }

    public static List<SoundEffect> getSounds(BiomeGenBase biome, String conditions) {
        return get(biome).findSoundMatches(conditions);
    }

    public static SoundEffect getSpotSound(BiomeGenBase biome, String conditions, Random random) {
        BiomeRegistry.Entry e = get(biome);
        if (e == null || e.spotSounds.isEmpty() || !WeightedRandom.roll(random, e.spotSoundChance)) {
            return null;
        }

        return WeightedRandom.pick(e.spotSounds, s -> s.matches(conditions) ? s.weight : 0, random);
    }

    private static void processConfig(TIntObjectHashMap<BiomeRegistry.Entry> map) {
        try {
            process(BiomeConfig.load("dynamicsurroundings"), map);
        } catch (Exception var8) {
            ModLog.error("Unable to load default biome config", var8);
        }

        String[] configFiles = ModOptions.biomeConfigFiles;

        for (String file : configFiles) {
            File theFile = new File(Module.dataDirectory(), file);
            if (theFile.exists()) {
                try {
                    BiomeConfig config = BiomeConfig.load(theFile);
                    if (config != null) {
                        process(config, map);
                    } else {
                        ModLog.warn("Unable to process biome config file " + file);
                    }
                } catch (Exception var7) {
                    ModLog.error("Unable to process biome config file " + file, var7);
                }
            } else {
                ModLog.warn("Could not locate biome config file [%s]", file);
            }
        }
    }

    static final boolean isBiomeMatch(BiomeConfig.Entry entry, String biomeName) {
        return isBiomeMatch(Pattern.compile(entry.biomeName), biomeName);
    }

    static final boolean isBiomeMatch(Pattern pattern, String biomeName) {
        if (pattern.matcher(biomeName)
            .matches()) {
            return true;
        } else {
            String alias = biomeAliases.get(biomeName);
            return alias == null ? false
                : pattern.matcher(alias)
                    .matches();
        }
    }

    private static void process(BiomeConfig config, TIntObjectHashMap<BiomeRegistry.Entry> map) {
        for (BiomeConfig.Entry entry : config.entries) {
            Pattern pattern = Pattern.compile(entry.biomeName);
            for (BiomeRegistry.Entry biomeEntry : map.valueCollection()) {
                if (isBiomeMatch(pattern, resolveName(biomeEntry.biome))) {
                    if (entry.hasPrecipitation != null) {
                        biomeEntry.hasPrecipitation = entry.hasPrecipitation;
                    }

                    if (entry.hasAurora != null) {
                        biomeEntry.hasAurora = entry.hasAurora;
                    }

                    if (entry.hasDust != null) {
                        biomeEntry.hasDust = entry.hasDust;
                    }

                    if (entry.hasFog != null) {
                        biomeEntry.hasFog = entry.hasFog;
                    }

                    if (entry.fogDensity != null) {
                        biomeEntry.fogDensity = entry.fogDensity;
                    }

                    if (entry.fogColor != null) {
                        int[] rgb = MyUtils.splitToInts(entry.fogColor, ',');
                        if (rgb.length == 3) {
                            biomeEntry.fogColor = new RgbColor(rgb[0], rgb[1], rgb[2]);
                        }
                    }

                    if (entry.dustColor != null) {
                        int[] rgb = MyUtils.splitToInts(entry.dustColor, ',');
                        if (rgb.length == 3) {
                            biomeEntry.dustColor = new RgbColor(rgb[0], rgb[1], rgb[2]);
                        }
                    }

                    if (entry.soundReset != null && entry.soundReset) {
                        biomeEntry.sounds = new ArrayList<>();
                        biomeEntry.spotSounds = new ArrayList<>();
                    }

                    if (entry.spotSoundChance != null) {
                        biomeEntry.spotSoundChance = entry.spotSoundChance;
                    }

                    for (SoundConfig sr : entry.sounds) {
                        if (!SoundRegistry.isSoundBlocked(sr.sound)) {
                            SoundEffect s = new SoundEffect(sr);
                            if (s.type == SoundEffect.SoundType.SPOT) {
                                biomeEntry.spotSounds.add(s);
                            } else {
                                biomeEntry.sounds.add(s);
                            }
                        }
                    }
                }
            }
        }
    }

    private static class Entry {

        public final BiomeGenBase biome;
        public boolean hasPrecipitation;
        public boolean hasDust;
        public boolean hasAurora;
        public boolean hasFog;
        public RgbColor dustColor;
        public RgbColor fogColor;
        public float fogDensity;
        public List<SoundEffect> sounds;
        public int spotSoundChance;
        public List<SoundEffect> spotSounds;

        public Entry(BiomeGenBase biome) {
            this.biome = biome;
            this.hasPrecipitation = biome.canSpawnLightningBolt() || biome.getEnableSnow();
            this.sounds = new ArrayList<>();
            this.spotSounds = new ArrayList<>();
            this.spotSoundChance = 1200;
            if (BOP_PRESENT && !Compat.isAngelicaBopFogActive()) {
                // Optional integration: BOP biomes expose their fog through IBiomeFog. BOPFogIntegration loads the
                // bridge only when BOP is present, so no BOP class is touched when the mod is absent. With Angelica
                // installed, its late BOP fog mixin already owns the blend; importing it here would double-apply it.
                IBOPFog bopFog = BOPFogIntegration.get();
                if (bopFog.isFogBiome(biome)) {
                    int color = bopFog.getFogColor(biome);
                    if (color > 0) {
                        this.hasFog = true;
                        this.fogColor = new RgbColor(color);
                        this.fogDensity = bopFog.getFogDensity(biome);
                    }
                }
            }
        }

        public SoundEffect findSoundMatch(String conditions) {
            for (SoundEffect sound : this.sounds) {
                if (sound.matches(conditions)) {
                    return sound;
                }
            }

            return null;
        }

        public List<SoundEffect> findSoundMatches(String conditions) {
            List<SoundEffect> results = new ArrayList<>();

            for (SoundEffect sound : this.sounds) {
                if (sound.matches(conditions)) {
                    results.add(sound);
                }
            }

            return results;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("Biome %d [%s]:", this.biome.biomeID, BiomeRegistry.resolveName(this.biome)));
            if (this.hasPrecipitation) {
                builder.append(" PRECIPITATION");
            }

            if (this.hasDust) {
                builder.append(" DUST");
            }

            if (this.hasAurora) {
                builder.append(" AURORA");
            }

            if (this.hasFog) {
                builder.append(" FOG");
            }

            if (!this.hasPrecipitation && !this.hasDust && !this.hasAurora && !this.hasFog) {
                builder.append(" NONE");
            }

            if (this.dustColor != null) {
                builder.append(" dustColor:")
                    .append(this.dustColor.toString());
            }

            if (this.fogColor != null) {
                builder.append(" fogColor:")
                    .append(this.fogColor.toString());
                builder.append(" fogDensity:")
                    .append(this.fogDensity);
            }

            if (!this.sounds.isEmpty()) {
                builder.append("; sounds [");

                for (SoundEffect sound : this.sounds) {
                    builder.append(sound.toString())
                        .append(',');
                }

                builder.append(']');
            }

            if (!this.spotSounds.isEmpty()) {
                builder.append("; spot sound chance:")
                    .append(this.spotSoundChance);
                builder.append(" spot sounds [");

                for (SoundEffect sound : this.spotSounds) {
                    builder.append(sound.toString())
                        .append(',');
                }

                builder.append(']');
            }

            return builder.toString();
        }
    }
}
