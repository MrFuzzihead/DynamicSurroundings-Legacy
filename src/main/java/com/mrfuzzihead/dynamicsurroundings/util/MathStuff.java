package com.mrfuzzihead.dynamicsurroundings.util;

public class MathStuff {

    private static final int SIN_BITS = 12;
    private static final int SIN_MASK = 4095;
    private static final int SIN_COUNT = 4096;
    private static final float RAD_FULL = (float) (Math.PI * 2);
    private static final float[] SIN_TABLE = new float[4096];
    private static final int ATAN2_BITS = 7;
    private static final int ATAN2_BITS2 = 14;
    private static final int ATAN2_MASK = 16383;
    private static final int ATAN2_COUNT = 16384;
    private static final int ATAN2_DIM = (int) Math.sqrt(16384.0);
    private static final float ATAN2_DIM_MINUS_1 = ATAN2_DIM - 1;
    private static final float[] ATAN2_TABLE = new float[16384];
    public static final float PI_F = (float) Math.PI;
    public static final float E_F = (float) Math.E;

    public static final float sin(float rad) {
        return SIN_TABLE[(int) (rad * 651.8986F) & 4095];
    }

    public static final float cos(float rad) {
        return SIN_TABLE[(int) ((rad + (float) (Math.PI / 2)) * 651.8986F) & 4095];
    }

    public static final float tan(float rad) {
        return sin(rad) / cos(rad);
    }

    public static final double sin(double rad) {
        return Math.sin(rad);
    }

    public static final double cos(double rad) {
        return Math.cos(rad);
    }

    public static final double tan(double rad) {
        return Math.tan(rad);
    }

    public static final float atan2(float y, float x) {
        float add;
        float mul;
        if (x < 0.0F) {
            if (y < 0.0F) {
                x = -x;
                y = -y;
                mul = 1.0F;
            } else {
                x = -x;
                mul = -1.0F;
            }

            add = (float) -Math.PI;
        } else {
            if (y < 0.0F) {
                y = -y;
                mul = -1.0F;
            } else {
                mul = 1.0F;
            }

            add = 0.0F;
        }

        float invDiv = ATAN2_DIM_MINUS_1 / (x < y ? y : x);
        int xi = (int) (x * invDiv);
        int yi = (int) (y * invDiv);
        return (ATAN2_TABLE[yi * ATAN2_DIM + xi] + add) * mul;
    }

    public static final double atan2(double y, double x) {
        float tmp1 = (float) y;
        float tmp2 = (float) x;
        return atan2(tmp1, tmp2);
    }

    public static final float toRadians(float degrees) {
        return degrees * (float) (Math.PI / 180.0);
    }

    public static final float toDegrees(float radians) {
        return radians * (float) (180.0 / Math.PI);
    }

    public static float wrapDegrees(float value) {
        value %= 360.0F;
        if (value >= 180.0F) {
            value -= 360.0F;
        }

        if (value < -180.0F) {
            value += 360.0F;
        }

        return value;
    }

    public static double wrapDegrees(double value) {
        value %= 360.0;
        if (value >= 180.0) {
            value -= 360.0;
        }

        if (value < -180.0) {
            value += 360.0;
        }

        return value;
    }

    public static final float abs(float val) {
        return val < 0.0F ? -val : val;
    }

    public static final double abs(double val) {
        return val < 0.0 ? -val : val;
    }

    public static final long abs(long val) {
        return val < 0L ? -val : val;
    }

    public static final int abs(int val) {
        return val < 0 ? -val : val;
    }

    public static float sqrt(float value) {
        return (float) Math.sqrt(value);
    }

    public static double sqrt(double value) {
        return Math.sqrt(value);
    }

    public static int floor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    public static int floor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    public static double log(double value) {
        return Math.log(value);
    }

    public static float clamp(float num, float min, float max) {
        return num < min ? min : (num > max ? max : num);
    }

    public static double clamp(double num, double min, double max) {
        return num < min ? min : (num > max ? max : num);
    }

    public static int clamp(int num, int min, int max) {
        return num < min ? min : (num > max ? max : num);
    }

    static {
        for (int i = 0; i < 4096; i++) {
            SIN_TABLE[i] = (float) Math.sin((i + 0.5F) / 4096.0F * (float) (Math.PI * 2));
        }

        for (int i = 0; i < 360; i += 90) {
            SIN_TABLE[(int) (i * 11.377778F) & 4095] = (float) Math.sin(i * Math.PI / 180.0);
        }

        for (int i = 0; i < ATAN2_DIM; i++) {
            for (int j = 0; j < ATAN2_DIM; j++) {
                float x0 = (float) i / ATAN2_DIM;
                float y0 = (float) j / ATAN2_DIM;
                ATAN2_TABLE[j * ATAN2_DIM + i] = (float) Math.atan2(y0, x0);
            }
        }
    }
}
