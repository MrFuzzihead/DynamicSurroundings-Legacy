package org.blockartistry.mod.DynSurround.util;

import javax.annotation.Nonnull;

import net.minecraft.util.Vec3;

public class Color {

    public static final Color RED = new Color.ImmutableColor(255, 0, 0);
    public static final Color ORANGE = new Color.ImmutableColor(255, 127, 0);
    public static final Color YELLOW = new Color.ImmutableColor(255, 255, 0);
    public static final Color LGREEN = new Color.ImmutableColor(127, 255, 0);
    public static final Color GREEN = new Color.ImmutableColor(0, 255, 0);
    public static final Color TURQOISE = new Color.ImmutableColor(0, 255, 127);
    public static final Color CYAN = new Color.ImmutableColor(0, 255, 255);
    public static final Color AUQUAMARINE = new Color.ImmutableColor(0, 127, 255);
    public static final Color BLUE = new Color.ImmutableColor(0, 0, 255);
    public static final Color VIOLET = new Color.ImmutableColor(127, 0, 255);
    public static final Color MAGENTA = new Color.ImmutableColor(255, 0, 255);
    public static final Color RASPBERRY = new Color.ImmutableColor(255, 0, 127);
    public static final Color BLACK = new Color.ImmutableColor(0, 0, 0);
    public static final Color WHITE = new Color.ImmutableColor(255, 255, 255);
    public static final Color PURPLE = new Color.ImmutableColor(80, 0, 80);
    public static final Color INDIGO = new Color.ImmutableColor(75, 0, 130);
    public static final Color NAVY = new Color.ImmutableColor(0, 0, 128);
    public static final Color TAN = new Color.ImmutableColor(210, 180, 140);
    public static final Color GOLD = new Color.ImmutableColor(255, 215, 0);
    public static final Color GRAY = new Color.ImmutableColor(128, 128, 128);
    public static final Color LGRAY = new Color.ImmutableColor(192, 192, 192);
    public static final Color SLATEGRAY = new Color.ImmutableColor(112, 128, 144);
    public static final Color DARKSLATEGRAY = new Color.ImmutableColor(47, 79, 79);
    public static final Color MC_BLACK = new Color.ImmutableColor(0, 0, 0);
    public static final Color MC_DARKBLUE = new Color.ImmutableColor(0, 0, 170);
    public static final Color MC_DARKGREEN = new Color.ImmutableColor(0, 170, 0);
    public static final Color MC_DARKAQUA = new Color.ImmutableColor(0, 170, 170);
    public static final Color MC_DARKRED = new Color.ImmutableColor(170, 0, 0);
    public static final Color MC_DARKPURPLE = new Color.ImmutableColor(170, 0, 170);
    public static final Color MC_GOLD = new Color.ImmutableColor(255, 170, 0);
    public static final Color MC_GRAY = new Color.ImmutableColor(170, 170, 170);
    public static final Color MC_DARKGRAY = new Color.ImmutableColor(85, 85, 85);
    public static final Color MC_BLUE = new Color.ImmutableColor(85, 85, 255);
    public static final Color MC_GREEN = new Color.ImmutableColor(85, 255, 85);
    public static final Color MC_AQUA = new Color.ImmutableColor(85, 255, 255);
    public static final Color MC_RED = new Color.ImmutableColor(255, 85, 85);
    public static final Color MC_LIGHTPURPLE = new Color.ImmutableColor(255, 85, 255);
    public static final Color MC_YELLOW = new Color.ImmutableColor(255, 255, 85);
    public static final Color MC_WHITE = new Color.ImmutableColor(255, 255, 255);
    public static final Color AURORA_RED = new Color.ImmutableColor(1.0F, 0.0F, 0.0F);
    public static final Color AURORA_GREEN = new Color.ImmutableColor(0.5F, 1.0F, 0.0F);
    public static final Color AURORA_BLUE = new Color.ImmutableColor(0.0F, 0.8F, 1.0F);
    public float red;
    public float green;
    public float blue;

    public Color(@Nonnull Color color) {
        this(color.red, color.green, color.blue);
    }

    public Color(int red, int green, int blue) {
        this(red / 255.0F, green / 255.0F, blue / 255.0F);
    }

    public Color(@Nonnull Vec3 vec) {
        this((float) vec.xCoord, (float) vec.yCoord, (float) vec.zCoord);
    }

    public Color(int rgb) {
        this(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF);
    }

    public Color(float red, float green, float blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public Color(double red, double green, double blue) {
        this((float) red, (float) green, (float) blue);
    }

    @Nonnull
    public Vec3 toVec3d() {
        return Vec3.createVectorHelper(this.red, this.green, this.blue);
    }

    @Nonnull
    public Vec3 transitionTo(@Nonnull Color target, int iterations) {
        double deltaRed = (target.red - this.red) / iterations;
        double deltaGreen = (target.green - this.green) / iterations;
        double deltaBlue = (target.blue - this.blue) / iterations;
        return Vec3.createVectorHelper(deltaRed, deltaGreen, deltaBlue);
    }

    @Nonnull
    public Color scale(float scaleFactor) {
        return this.scale(scaleFactor, scaleFactor, scaleFactor);
    }

    @Nonnull
    public Color scale(float scaleRed, float scaleGreen, float scaleBlue) {
        this.red *= scaleRed;
        this.green *= scaleGreen;
        this.blue *= scaleBlue;
        return this;
    }

    @Nonnull
    public static Color scale(@Nonnull Color color, float scaleFactor) {
        return new Color(color).scale(scaleFactor);
    }

    @Nonnull
    public Color add(@Nonnull Color color) {
        this.red = this.red + color.red;
        this.green = this.green + color.green;
        this.blue = this.blue + color.blue;
        return this;
    }

    @Nonnull
    public Color add(float red, float green, float blue) {
        this.red += red;
        this.green += green;
        this.blue += blue;
        return this;
    }

    private static float blend(float c1, float c2, float factor) {
        return (float) Math.sqrt((1.0F - factor) * c1 * c1 + factor * c2 * c2);
    }

    @Nonnull
    public Color blend(@Nonnull Color color, float factor) {
        this.red = blend(this.red, color.red, factor);
        this.green = blend(this.green, color.green, factor);
        this.blue = blend(this.blue, color.blue, factor);
        return this;
    }

    @Nonnull
    public Color mix(@Nonnull Color color) {
        return this.mix(color.red, color.green, color.blue);
    }

    @Nonnull
    public Color mix(float red, float green, float blue) {
        this.red = (this.red + red) / 2.0F;
        this.green = (this.green + green) / 2.0F;
        this.blue = (this.blue + blue) / 2.0F;
        return this;
    }

    @Nonnull
    public Color adjust(@Nonnull Vec3 adjust, @Nonnull Color target) {
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
    public Color luminance(float percent) {
        float r = Math.min(Math.max(0.0F, this.red + this.red * percent), 1.0F);
        float g = Math.min(Math.max(0.0F, this.green + this.green * percent), 1.0F);
        float b = Math.min(Math.max(0.0F, this.blue + this.blue * percent), 1.0F);
        return new Color(r, g, b);
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
        if (anObject != null && anObject instanceof Color) {
            Color color = (Color) anObject;
            return this.red == color.red && this.green == color.green && this.blue == color.blue;
        } else {
            return false;
        }
    }

    @Nonnull
    public Color asImmutable() {
        return new Color.ImmutableColor(this);
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

    public static final class ImmutableColor extends Color {

        ImmutableColor(@Nonnull Color color) {
            super(color);
        }

        ImmutableColor(int red, int green, int blue) {
            super(red, green, blue);
        }

        ImmutableColor(float red, float green, float blue) {
            super(red, green, blue);
        }

        @Override
        public Color scale(float scaleFactor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Color mix(float red, float green, float blue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Color adjust(@Nonnull Vec3 adjust, @Nonnull Color target) {
            throw new UnsupportedOperationException();
        }
    }
}
