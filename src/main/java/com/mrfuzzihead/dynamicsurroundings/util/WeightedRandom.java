package com.mrfuzzihead.dynamicsurroundings.util;

import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;

/**
 * Guarded weighted selection and chance rolls.
 *
 * <p>
 * The original code repeated the reverse-cumulative pick in three places and called {@code nextInt(chance)}
 * unconditionally. A configured chance of zero threw {@link IllegalArgumentException}, and a negative weight could
 * walk the loop past index zero. This helper treats non-positive weights as absent and defines a non-positive chance
 * as "never".
 * </p>
 */
public final class WeightedRandom {

    private WeightedRandom() {}

    /** @return true only when {@code chance > 0} and the roll is zero */
    public static boolean roll(Random random, int chance) {
        return chance > 0 && random.nextInt(chance) == 0;
    }

    /**
     * Picks an index from non-negative weights. Returns {@code -1} when every weight is zero/negative.
     */
    public static int pickIndex(int[] weights, Random random) {
        int total = 0;
        for (int weight : weights) {
            if (weight > 0) {
                total += weight;
            }
        }

        if (total <= 0) {
            return -1;
        }

        int target = random.nextInt(total);
        int index = weights.length;
        while ((target -= Math.max(0, weights[index - 1])) >= 0) {
            index--;
        }

        return index - 1;
    }

    /**
     * Picks an item using a non-negative weight function. Returns {@code null} when no candidate has a positive
     * weight.
     */
    public static <T> T pick(List<T> candidates, ToIntFunction<T> weight, Random random) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        int total = 0;
        for (T candidate : candidates) {
            int value = weight.applyAsInt(candidate);
            if (value > 0) {
                total += value;
            }
        }

        if (total <= 0) {
            return null;
        }

        int target = random.nextInt(total);
        int index = candidates.size();
        while ((target -= Math.max(0, weight.applyAsInt(candidates.get(index - 1)))) >= 0) {
            index--;
        }

        return candidates.get(index - 1);
    }
}
