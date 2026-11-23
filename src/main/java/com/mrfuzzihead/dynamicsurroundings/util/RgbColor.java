package com.mrfuzzihead.dynamicsurroundings.util;

import javax.annotation.Nonnull;

import net.minecraft.util.Vec3;

public class RgbColor {

    public static final RgbColor RED = new RgbColor.ImmutableRgbColor(255, 0, 0);
    public static final RgbColor ORANGE = new RgbColor.ImmutableRgbColor(255, 127, 0);
    public static final RgbColor YELLOW = new RgbColor.ImmutableRgbColor(255, 255, 0);
    public static final RgbColor LGREEN = new RgbColor.ImmutableRgbColor(127, 255, 0);
    public static final RgbColor GREEN = new RgbColor.ImmutableRgbColor(0, 255, 0);
    public static final RgbColor TURQOISE = new RgbColor.ImmutableRgbColor(0, 255, 127);
    public static final RgbColor CYAN = new RgbColor.ImmutableRgbColor(0, 255, 255);
    public static final RgbColor AUQUAMARINE = new RgbColor.ImmutableRgbColor(0, 127, 255);
    public static final RgbColor BLUE = new RgbColor.ImmutableRgbColor(0, 0, 255);
    public static final RgbColor VIOLET = new RgbColor.ImmutableRgbColor(127, 0, 255);
    public static final RgbColor MAGENTA = new RgbColor.ImmutableRgbColor(255, 0, 255);
    public static final RgbColor RASPBERRY = new RgbColor.ImmutableRgbColor(255, 0, 127);
    public static final RgbColor BLACK = new RgbColor.ImmutableRgbColor(0, 0, 0);
    public static final RgbColor WHITE = new RgbColor.ImmutableRgbColor(255, 255, 255);
    public static final RgbColor PURPLE = new RgbColor.ImmutableRgbColor(80, 0, 80);
    public static final RgbColor INDIGO = new RgbColor.ImmutableRgbColor(75, 0, 130);
    public static final RgbColor NAVY = new RgbColor.ImmutableRgbColor(0, 0, 128);
    public static final RgbColor TAN = new RgbColor.ImmutableRgbColor(210, 180, 140);
    public static final RgbColor GOLD = new RgbColor.ImmutableRgbColor(255, 215, 0);
    public static final RgbColor GRAY = new RgbColor.ImmutableRgbColor(128, 128, 128);
    public static final RgbColor LGRAY = new RgbColor.ImmutableRgbColor(192, 192, 192);
    public static final RgbColor SLATEGRAY = new RgbColor.ImmutableRgbColor(112, 128, 144);
    public static final RgbColor DARKSLATEGRAY = new RgbColor.ImmutableRgbColor(47, 79, 79);
    public static final RgbColor MC_DARKBLUE = new RgbColor.ImmutableRgbColor(0, 0, 170);
    public static final RgbColor MC_DARKGREEN = new RgbColor.ImmutableRgbColor(0, 170, 0);
    public static final RgbColor MC_DARKAQUA = new RgbColor.ImmutableRgbColor(0, 170, 170);
    public static final RgbColor MC_DARKRED = new RgbColor.ImmutableRgbColor(170, 0, 0);
    public static final RgbColor MC_DARKPURPLE = new RgbColor.ImmutableRgbColor(170, 0, 170);
    public static final RgbColor MC_GOLD = new RgbColor.ImmutableRgbColor(255, 170, 0);
    public static final RgbColor MC_GRAY = new RgbColor.ImmutableRgbColor(170, 170, 170);
    public static final RgbColor MC_DARKGRAY = new RgbColor.ImmutableRgbColor(85, 85, 85);
    public static final RgbColor MC_BLUE = new RgbColor.ImmutableRgbColor(85, 85, 255);
    public static final RgbColor MC_GREEN = new RgbColor.ImmutableRgbColor(85, 255, 85);
    public static final RgbColor MC_AQUA = new RgbColor.ImmutableRgbColor(85, 255, 255);
    public static final RgbColor MC_RED = new RgbColor.ImmutableRgbColor(255, 85, 85);
    public static final RgbColor MC_LIGHTPURPLE = new RgbColor.ImmutableRgbColor(255, 85, 255);
    public static final RgbColor MC_YELLOW = new RgbColor.ImmutableRgbColor(255, 255, 85);
    public static final RgbColor AURORA_RED = new RgbColor.ImmutableRgbColor(1.0F, 0.0F, 0.0F);
    public static final RgbColor AURORA_GREEN = new RgbColor.ImmutableRgbColor(0.5F, 1.0F, 0.0F);
    public static final RgbColor AURORA_BLUE = new RgbColor.ImmutableRgbColor(0.0F, 0.8F, 1.0F);
    public float red;
    public float green;
    public float blue;

    public RgbColor(@Nonnull RgbColor color) {
        this(color.red, color.green, color.blue);
    }

    public RgbColor(int red, int green, int blue) {
        this(red / 255.0F, green / 255.0F, blue / 255.0F);
    }

    public RgbColor(@Nonnull Vec3 vec) {
        this((float) vec.xCoord, (float) vec.yCoord, (float) vec.zCoord);
    }

    public RgbColor(int rgb) {
        this(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF);
    }

    public RgbColor(float red, float green, float blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public RgbColor(double red, double green, double blue) {
        this((float) red, (float) green, (float) blue);
    }

    @Nonnull
    public Vec3 toVec3d() {
        return Vec3.createVectorHelper(this.red, this.green, this.blue);
    }

    @Nonnull
    public Vec3 transitionTo(@Nonnull RgbColor target, int iterations) {
        double deltaRed = (target.red - this.red) / iterations;
        double deltaGreen = (target.green - this.green) / iterations;
        double deltaBlue = (target.blue - this.blue) / iterations;
        return Vec3.createVectorHelper(deltaRed, deltaGreen, deltaBlue);
    }

    @Nonnull
    public RgbColor scale(float scaleFactor) {
        return this.scale(scaleFactor, scaleFactor, scaleFactor);
    }

    @Nonnull
    public RgbColor scale(float scaleRed, float scaleGreen, float scaleBlue) {
        this.red *= scaleRed;
        this.green *= scaleGreen;
        this.blue *= scaleBlue;
        return this;
    }

    @Nonnull
    public static RgbColor scale(@Nonnull RgbColor color, float scaleFactor) {
        return new RgbColor(color).scale(scaleFactor);
    }

    @Nonnull
    public RgbColor add(@Nonnull RgbColor color) {
        this.red = this.red + color.red;
        this.green = this.green + color.green;
        this.blue = this.blue + color.blue;
        return this;
    }

    @Nonnull
    public RgbColor add(float red, float green, float blue) {
        this.red += red;
        this.green += green;
        this.blue += blue;
        return this;
    }

    private static float blend(float c1, float c2, float factor) {
        return (float) Math.sqrt((1.0F - factor) * c1 * c1 + factor * c2 * c2);
    }

    @Nonnull
    public RgbColor blend(@Nonnull RgbColor color, float factor) {
        this.red = blend(this.red, color.red, factor);
        this.green = blend(this.green, color.green, factor);
        this.blue = blend(this.blue, color.blue, factor);
        return this;
    }

    @Nonnull
    public RgbColor mix(@Nonnull RgbColor color) {
        return this.mix(color.red, color.green, color.blue);
    }

    @Nonnull
    public RgbColor mix(float red, float green, float blue) {
        this.red = (this.red + red) / 2.0F;
        this.green = (this.green + green) / 2.0F;
        this.blue = (this.blue + blue) / 2.0F;
        return this;
    }

    @Nonnull
    public RgbColor adjust(@Nonnull Vec3 adjust, @Nonnull RgbColor target) {
        this.red = (float) (this.red + adjust.xCoord);
        if (adjust.xCoord < 0.0 && this.red < target.red || adjust.xCoord > 0.0 && this.red > target.red) {
            this.red = target.red;
        }

        this.green = (float) (this.green + adjust.yCoord);
        if (adjust.yCoord < 0.0 && this.green < target.green || adjust.yCoord > 0.0 && this.green > target.green) {
            this.green = target.green;
        }

        this.blue = (float) (this.blue + adjust.zCoord);
        if (adjust.zCoord < 0.0 && this.blue < target.blue || adjust.zCoord > 0.0 && this.blue > target.blue) {
            this.blue = target.blue;
        }

        return this;
    }

    @Nonnull
    public RgbColor luminance(float percent) {
        float r = Math.min(Math.max(0.0F, this.red + this.red * percent), 1.0F);
        float g = Math.min(Math.max(0.0F, this.green + this.green * percent), 1.0F);
        float b = Math.min(Math.max(0.0F, this.blue + this.blue * percent), 1.0F);
        return new RgbColor(r, g, b);
    }

    public int rgb() {
        int iRed = (int) (this.red * 255.0F);
        int iGreen = (int) (this.green * 255.0F);
        int iBlue = (int) (this.blue * 255.0F);
        return iRed << 16 | iGreen << 8 | iBlue;
    }

    public int rgbWithAlpha(float alpha) {
        int iAlpha = (int) (alpha * 255.0F);
        return this.rgb() | iAlpha << 24;
    }

    @Override
    public boolean equals(Object anObject) {
        if (anObject != null && anObject instanceof RgbColor) {
            RgbColor color = (RgbColor) anObject;
            return this.red == color.red && this.green == color.green && this.blue == color.blue;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(this.red);
        result = 31 * result + Float.floatToIntBits(this.green);
        result = 31 * result + Float.floatToIntBits(this.blue);
        return result;
    }

    @Nonnull
    public RgbColor asImmutable() {
        return new RgbColor.ImmutableRgbColor(this);
    }

    @Nonnull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[r:")
            .append((int) (this.red * 255.0F));
        builder.append(",g:")
            .append((int) (this.green * 255.0F));
        builder.append(",b:")
            .append((int) (this.blue * 255.0F));
        builder.append(']');
        return builder.toString();
    }

    public static final class ImmutableRgbColor extends RgbColor {

        ImmutableRgbColor(@Nonnull RgbColor color) {
            super(color);
        }

        ImmutableRgbColor(int red, int green, int blue) {
            super(red, green, blue);
        }

        ImmutableRgbColor(float red, float green, float blue) {
            super(red, green, blue);
        }

        @Override
        public RgbColor scale(float scaleFactor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RgbColor scale(float scaleRed, float scaleGreen, float scaleBlue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RgbColor add(@Nonnull RgbColor color) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RgbColor add(float red, float green, float blue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RgbColor blend(@Nonnull RgbColor color, float factor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RgbColor mix(@Nonnull RgbColor color) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RgbColor mix(float red, float green, float blue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RgbColor adjust(@Nonnull Vec3 adjust, @Nonnull RgbColor target) {
            throw new UnsupportedOperationException();
        }
    }
}
