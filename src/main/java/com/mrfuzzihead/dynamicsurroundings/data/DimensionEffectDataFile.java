package com.mrfuzzihead.dynamicsurroundings.data;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import gnu.trove.map.hash.TIntObjectHashMap;

public final class DimensionEffectDataFile extends WorldSavedData {

    private static final String IDENTIFIER = "dynamicsurroundings";
    private static final Map<World, DimensionEffectDataFile> WORLD_CACHE = Collections
        .synchronizedMap(new WeakHashMap<>());
    private final TIntObjectHashMap<DimensionEffectData> dataList = new TIntObjectHashMap();

    public DimensionEffectDataFile() {
        this("dynamicsurroundings");
    }

    public DimensionEffectDataFile(String id) {
        super(id);
    }

    private static DimensionEffectDataFile getFile(World world) {
        DimensionEffectDataFile data = WORLD_CACHE.get(world);
        if (data != null) {
            return data;
        }

        data = (DimensionEffectDataFile) world.loadItemData(DimensionEffectDataFile.class, "dynamicsurroundings");
        if (data == null) {
            data = new DimensionEffectDataFile();
            world.setItemData("dynamicsurroundings", data);
        }

        WORLD_CACHE.put(world, data);
        return data;
    }

    public static void unload(World world) {
        WORLD_CACHE.remove(world);
    }

    private DimensionEffectData getData(int dimensionId) {
        DimensionEffectData data = (DimensionEffectData) this.dataList.get(dimensionId);
        if (data != null) {
            return data;
        } else {
            data = new DimensionEffectData(dimensionId);
            data.setDirtyCallback(this::markDirty);
            this.dataList.put(dimensionId, data);
            data.markDirty();
            return data;
        }
    }

    public static DimensionEffectData get(World world) {
        return getFile(world).getData(world.provider.dimensionId);
    }

    public void readFromNBT(NBTTagCompound nbt) {
        NBTTagList list = nbt.getTagList("e", 10);

        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            DimensionEffectData data = new DimensionEffectData();
            data.setDirtyCallback(this::markDirty);
            data.readFromNBT(tag);
            this.dataList.put(data.getDimensionId(), data);
        }
    }

    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();

        for (DimensionEffectData data : this.dataList.valueCollection()) {
            NBTTagCompound tag = new NBTTagCompound();
            data.writeToNBT(tag);
            list.appendTag(tag);
        }

        nbt.setTag("e", list);
    }

}
