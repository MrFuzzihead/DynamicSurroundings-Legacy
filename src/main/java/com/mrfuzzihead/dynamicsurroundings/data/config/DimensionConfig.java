package com.mrfuzzihead.dynamicsurroundings.data.config;

import java.io.File;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.SerializedName;
import com.mrfuzzihead.dynamicsurroundings.util.JsonUtils;

public final class DimensionConfig {

    @SerializedName("entries")
    public List<DimensionConfig.Entry> entries = ImmutableList.of();

    public static DimensionConfig load(File file) throws Exception {
        return JsonUtils.load(file, DimensionConfig.class);
    }

    public static DimensionConfig load(String modId) throws Exception {
        return JsonUtils.load(modId, DimensionConfig.class);
    }

    public static final class Entry {

        @SerializedName("dimId")
        public Integer dimensionId = null;
        @SerializedName("name")
        public String name = null;
        @SerializedName("seaLevel")
        public Integer seaLevel = null;
        @SerializedName("skyHeight")
        public Integer skyHeight = null;
        @SerializedName("cloudHeight")
        public Integer cloudHeight = null;
        @SerializedName("haze")
        public Boolean hasHaze = null;
        @SerializedName("aurora")
        public Boolean hasAurora = null;
        @SerializedName("weather")
        public Boolean hasWeather = null;
    }
}
