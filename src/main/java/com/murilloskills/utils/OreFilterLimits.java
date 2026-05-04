package com.murilloskills.utils;

public final class OreFilterLimits {
    public static final int MIN_ORES = 5;
    public static final int MAX_ORES = 500;
    public static final int DEFAULT_ORES = 500;

    private OreFilterLimits() {
    }

    public static int clampMaxOres(int value) {
        return Math.max(MIN_ORES, Math.min(MAX_ORES, value));
    }
}
