package com.mrfuzzihead.dynamicsurroundings.data.compat;

/**
 * Duck interface implemented by the Biomes O' Plenty mixin. Living outside the mixin package is deliberate: main
 * code may reference this interface unconditionally, while the mixin class that implements it is only loaded when
 * BOP is present.
 */
public interface IBOPBiome {

    int dynamicsurroundings$getFogColor();

    float dynamicsurroundings$getFogDensity();
}
