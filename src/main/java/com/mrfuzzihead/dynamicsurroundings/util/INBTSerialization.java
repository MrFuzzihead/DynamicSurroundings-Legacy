package com.mrfuzzihead.dynamicsurroundings.util;

import net.minecraft.nbt.NBTTagCompound;

public interface INBTSerialization {

    void readFromNBT(NBTTagCompound var1);

    void writeToNBT(NBTTagCompound var1);
}
