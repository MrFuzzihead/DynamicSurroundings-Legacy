package com.mrfuzzihead.dynamicsurroundings.util;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import org.apache.commons.lang3.StringUtils;

public final class MyUtils {

    private static final int[] EMPTY = new int[0];

    private MyUtils() {}

    public static int[] splitToInts(String str, char splitChar) {
        String[] tokens = StringUtils.split(str, splitChar);
        if (tokens != null && tokens.length != 0) {
            int[] result = new int[tokens.length];

            for (int i = 0; i < tokens.length; i++) {
                result[i] = Integer.parseInt(tokens[i]);
            }

            return result;
        } else {
            return EMPTY;
        }
    }

    public static long currentTimeMillis() {
        return System.nanoTime() / 1000000L;
    }

    public static Vec3 getCenter(AxisAlignedBB box) {
        return Vec3.createVectorHelper(
            box.minX + (box.maxX - box.minX) * 0.5,
            box.minY + (box.maxY - box.minY) * 0.5,
            box.minZ + (box.maxZ - box.minZ) * 0.5);
    }
}
