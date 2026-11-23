package com.mrfuzzihead.dynamicsurroundings.data;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.util.INBTSerialization;
import com.mrfuzzihead.dynamicsurroundings.util.random.XorShiftRandom;

public final class DimensionEffectData implements INBTSerialization {

    private static final XorShiftRandom random = new XorShiftRandom();
    private int dimensionId = 0;
    private float intensity = 0.0F;
    private float minIntensity = ModOptions.defaultMinRainStrength;
    private float maxIntensity = ModOptions.defaultMaxRainStrength;
    private final Set<AuroraData> auroras = new HashSet<>();
    private Runnable dirtyCallback = () -> {};

    public DimensionEffectData() {}

    public DimensionEffectData(int dimensionId) {
        this.dimensionId = dimensionId;
    }

    public int getDimensionId() {
        return this.dimensionId;
    }

    public float getRainIntensity() {
        return this.intensity;
    }

    public void setRainIntensity(float intensity) {
        this.intensity = MathHelper.clamp_float(intensity, 0.0F, 1.0F);
        this.markDirty();
    }

    public void setDirtyCallback(Runnable callback) {
        this.dirtyCallback = callback == null ? () -> {} : callback;
    }

    public void markDirty() {
        this.dirtyCallback.run();
    }

    public float getMinRainIntensity() {
        return this.minIntensity;
    }

    public void setMinRainIntensity(float intensity) {
        this.minIntensity = MathHelper.clamp_float(intensity, 0.0F, this.maxIntensity);
        this.markDirty();
    }

    public float getMaxRainIntensity() {
        return this.maxIntensity;
    }

    public void setMaxRainIntensity(float intensity) {
        this.maxIntensity = MathHelper.clamp_float(intensity, this.minIntensity, 1.0F);
        this.markDirty();
    }

    public Set<AuroraData> getAuroraList() {
        return this.auroras;
    }

    public void randomizeRain() {
        float result = 0.0F;
        float delta = this.maxIntensity - this.minIntensity;
        if (delta <= 0.0F) {
            result = this.minIntensity;
        } else {
            result = this.minIntensity + random.nextFloat() * delta;
        }

        this.setRainIntensity(MathHelper.clamp_float(result, 0.01F, 1.0F));
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.dimensionId = nbt.getInteger("d");
        this.intensity = MathHelper.clamp_float(nbt.getFloat("s"), 0.0F, 1.0F);
        if (nbt.hasKey("min")) {
            this.minIntensity = MathHelper.clamp_float(nbt.getFloat("min"), 0.0F, 1.0F);
        }

        if (nbt.hasKey("max")) {
            this.maxIntensity = MathHelper.clamp_float(nbt.getFloat("max"), this.minIntensity, 1.0F);
        }

        NBTTagList list = nbt.getTagList("al", 10);

        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            AuroraData data = new AuroraData();
            data.readFromNBT(tag);
            this.auroras.add(data);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("d", this.dimensionId);
        nbt.setFloat("s", this.intensity);
        nbt.setFloat("min", this.minIntensity);
        nbt.setFloat("max", this.maxIntensity);
        NBTTagList list = new NBTTagList();

        for (AuroraData data : this.auroras) {
            NBTTagCompound tag = new NBTTagCompound();
            data.writeToNBT(tag);
            list.appendTag(tag);
        }

        nbt.setTag("al", list);
    }

    public static DimensionEffectData get(World world) {
        return DimensionEffectDataFile.get(world);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("dim ")
            .append(this.dimensionId)
            .append(": ");
        builder.append("intensity: ")
            .append(String.format(Locale.ENGLISH, "%.0f", this.intensity * 100.0F));
        builder.append(" [")
            .append(String.format(Locale.ENGLISH, "%.0f", this.minIntensity * 100.0F));
        builder.append(",")
            .append(String.format(Locale.ENGLISH, "%.0f", this.maxIntensity * 100.0F));
        builder.append("]");
        builder.append(", auroras: ")
            .append(this.auroras.size());
        return builder.toString();
    }

}
