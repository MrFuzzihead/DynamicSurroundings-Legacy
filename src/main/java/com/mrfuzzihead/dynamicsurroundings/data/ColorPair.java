package com.mrfuzzihead.dynamicsurroundings.data;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.MathHelper;

import com.mrfuzzihead.dynamicsurroundings.util.RgbColor;
import com.mrfuzzihead.dynamicsurroundings.util.random.XorShiftRandom;

public final class ColorPair {

    public final RgbColor baseColor;
    public final RgbColor fadeColor;
    private static final List<ColorPair> PAIRS = new ArrayList<>();

    private ColorPair(RgbColor base, RgbColor fade) {
        this.baseColor = base;
        this.fadeColor = fade;
    }

    public static ColorPair get(int id) {
        return PAIRS.get(MathHelper.clamp_int(id, 0, PAIRS.size() - 1));
    }

    public static int randomId() {
        return XorShiftRandom.current()
            .nextInt(PAIRS.size());
    }

    public static int testId() {
        return PAIRS.size() - 1;
    }

    static {
        PAIRS.add(new ColorPair(new RgbColor(0, 255, 153), new RgbColor(51, 255, 0)));
        PAIRS.add(new ColorPair(RgbColor.BLUE, RgbColor.GREEN));
        PAIRS.add(new ColorPair(RgbColor.TURQOISE, RgbColor.LGREEN));
        PAIRS.add(new ColorPair(RgbColor.YELLOW, RgbColor.RED));
        PAIRS.add(new ColorPair(RgbColor.NAVY, RgbColor.INDIGO));
        PAIRS.add(new ColorPair(RgbColor.GREEN, RgbColor.YELLOW));
        PAIRS.add(new ColorPair(RgbColor.MAGENTA, RgbColor.GREEN));
        PAIRS.add(new ColorPair(RgbColor.INDIGO, RgbColor.GREEN));
        PAIRS.add(new ColorPair(RgbColor.CYAN, RgbColor.MAGENTA));
    }
}
