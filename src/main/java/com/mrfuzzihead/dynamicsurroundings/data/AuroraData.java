package com.mrfuzzihead.dynamicsurroundings.data;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import com.mrfuzzihead.dynamicsurroundings.util.INBTSerialization;

public final class AuroraData implements INBTSerialization {

    public int dimensionId;
    public int posX;
    public int posZ;
    public long seed;
    public int colorSet;
    public int preset;

    public AuroraData() {}

    public AuroraData(EntityPlayer player, int zOffset, int colorSet, int preset) {
        this(
            player.worldObj.provider.dimensionId,
            (int) player.posX,
            (int) player.posZ + zOffset,
            player.worldObj.getWorldTime(),
            colorSet,
            preset);
    }

    public AuroraData(int dimensionId, int x, int z, long seed, int colorSet, int preset) {
        this.dimensionId = dimensionId;
        this.posX = x;
        this.posZ = z;
        this.seed = seed;
        this.colorSet = colorSet;
        this.preset = preset;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.dimensionId = nbt.getInteger("d");
        this.posX = nbt.getInteger("x");
        this.posZ = nbt.getInteger("z");
        this.seed = nbt.getLong("t");
        this.colorSet = nbt.getInteger("s");
        this.preset = nbt.getInteger("p");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("d", this.dimensionId);
        nbt.setInteger("x", this.posX);
        nbt.setInteger("z", this.posZ);
        nbt.setLong("t", this.seed);
        nbt.setInteger("s", this.colorSet);
        nbt.setInteger("p", this.preset);
    }

    @Override
    public boolean equals(Object anObj) {
        if (!(anObj instanceof AuroraData)) {
            return false;
        } else {
            AuroraData a = (AuroraData) anObj;
            return this.dimensionId == a.dimensionId && this.posX == a.posX && this.posZ == a.posZ;
        }
    }

    @Override
    public int hashCode() {
        int result = this.dimensionId;
        result = 31 * result + this.posX;
        result = 31 * result + this.posZ;
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[x: ")
            .append(this.posX)
            .append(", z: ")
            .append(this.posZ)
            .append(']');
        builder.append(" color: ")
            .append(this.colorSet);
        builder.append(" preset: ")
            .append(this.preset);
        builder.append(" seed: ")
            .append(this.seed);
        return builder.toString();
    }

    private static final class NBT {

        public static final String DIMENSION = "d";
        public static final String XCOORD = "x";
        public static final String ZCOORD = "z";
        public static final String SEED = "t";
        public static final String COLOR_SET = "s";
        public static final String PRESET = "p";
    }
}
