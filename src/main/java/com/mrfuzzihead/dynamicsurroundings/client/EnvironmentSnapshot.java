package com.mrfuzzihead.dynamicsurroundings.client;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Immutable view of the client environment for one tick.
 *
 * <p>
 * The old {@code EnvironState} exposed a set of mutable static fields that handlers updated and read in registration
 * order. A handler could therefore observe a half-updated tick, and tests could not construct a stable environment.
 * This value object is built once in {@link EnvironStateHandler.EnvironState#tick} and published atomically.
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class EnvironmentSnapshot {

    public static final EnvironmentSnapshot EMPTY = builder().build();

    private final EntityPlayer player;
    private final World world;
    private final BiomeGenBase playerBiome;
    private final String biomeName;
    private final DimensionRegistry dimensionInfo;
    private final int dimensionId;
    private final String dimensionName;
    private final String conditions;
    private final boolean freezing;
    private final boolean foggy;
    private final boolean humid;
    private final boolean dry;
    private final boolean inside;
    private final String temperatureCategory;
    private final int tickCounter;
    private final boolean playerOnGround;
    private final boolean playerMoving;

    private EnvironmentSnapshot(Builder builder) {
        this.player = builder.player;
        this.world = builder.world;
        this.playerBiome = builder.playerBiome;
        this.biomeName = builder.biomeName;
        this.dimensionInfo = builder.dimensionInfo;
        this.dimensionId = builder.dimensionId;
        this.dimensionName = builder.dimensionName;
        this.conditions = builder.conditions;
        this.freezing = builder.freezing;
        this.foggy = builder.foggy;
        this.humid = builder.humid;
        this.dry = builder.dry;
        this.inside = builder.inside;
        this.temperatureCategory = builder.temperatureCategory;
        this.tickCounter = builder.tickCounter;
        this.playerOnGround = builder.playerOnGround;
        this.playerMoving = builder.playerMoving;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().player(this.player)
            .world(this.world)
            .playerBiome(this.playerBiome)
            .biomeName(this.biomeName)
            .dimensionInfo(this.dimensionInfo)
            .dimensionId(this.dimensionId)
            .dimensionName(this.dimensionName)
            .conditions(this.conditions)
            .freezing(this.freezing)
            .foggy(this.foggy)
            .humid(this.humid)
            .dry(this.dry)
            .inside(this.inside)
            .temperatureCategory(this.temperatureCategory)
            .tickCounter(this.tickCounter)
            .playerOnGround(this.playerOnGround)
            .playerMoving(this.playerMoving);
    }

    public EntityPlayer getPlayer() {
        return this.player;
    }

    public World getWorld() {
        return this.world;
    }

    public BiomeGenBase getPlayerBiome() {
        return this.playerBiome;
    }

    public String getBiomeName() {
        return this.biomeName;
    }

    public DimensionRegistry getDimensionInfo() {
        return this.dimensionInfo;
    }

    public int getDimensionId() {
        return this.dimensionId;
    }

    public String getDimensionName() {
        return this.dimensionName;
    }

    public String getConditions() {
        return this.conditions;
    }

    public boolean isFreezing() {
        return this.freezing;
    }

    public boolean isFoggy() {
        return this.foggy;
    }

    public boolean isHumid() {
        return this.humid;
    }

    public boolean isDry() {
        return this.dry;
    }

    public boolean isInside() {
        return this.inside;
    }

    public String getTemperatureCategory() {
        return this.temperatureCategory;
    }

    public int getTickCounter() {
        return this.tickCounter;
    }

    public boolean isPlayerOnGround() {
        return this.playerOnGround;
    }

    public boolean isPlayerMoving() {
        return this.playerMoving;
    }

    public static final class Builder {

        private EntityPlayer player;
        private World world;
        private BiomeGenBase playerBiome;
        private String biomeName = "";
        private DimensionRegistry dimensionInfo;
        private int dimensionId;
        private String dimensionName = "";
        private String conditions = "";
        private boolean freezing;
        private boolean foggy;
        private boolean humid;
        private boolean dry;
        private boolean inside;
        private String temperatureCategory = "";
        private int tickCounter;
        private boolean playerOnGround;
        private boolean playerMoving;

        private Builder() {}

        public Builder player(EntityPlayer player) {
            this.player = player;
            return this;
        }

        public Builder world(World world) {
            this.world = world;
            return this;
        }

        public Builder playerBiome(BiomeGenBase playerBiome) {
            this.playerBiome = playerBiome;
            return this;
        }

        public Builder biomeName(String biomeName) {
            this.biomeName = biomeName;
            return this;
        }

        public Builder dimensionInfo(DimensionRegistry dimensionInfo) {
            this.dimensionInfo = dimensionInfo;
            return this;
        }

        public Builder dimensionId(int dimensionId) {
            this.dimensionId = dimensionId;
            return this;
        }

        public Builder dimensionName(String dimensionName) {
            this.dimensionName = dimensionName;
            return this;
        }

        public Builder conditions(String conditions) {
            this.conditions = conditions;
            return this;
        }

        public Builder freezing(boolean freezing) {
            this.freezing = freezing;
            return this;
        }

        public Builder foggy(boolean foggy) {
            this.foggy = foggy;
            return this;
        }

        public Builder humid(boolean humid) {
            this.humid = humid;
            return this;
        }

        public Builder dry(boolean dry) {
            this.dry = dry;
            return this;
        }

        public Builder inside(boolean inside) {
            this.inside = inside;
            return this;
        }

        public Builder temperatureCategory(String temperatureCategory) {
            this.temperatureCategory = temperatureCategory;
            return this;
        }

        public Builder tickCounter(int tickCounter) {
            this.tickCounter = tickCounter;
            return this;
        }

        public Builder playerOnGround(boolean playerOnGround) {
            this.playerOnGround = playerOnGround;
            return this;
        }

        public Builder playerMoving(boolean playerMoving) {
            this.playerMoving = playerMoving;
            return this;
        }

        public EnvironmentSnapshot build() {
            return new EnvironmentSnapshot(this);
        }
    }
}
