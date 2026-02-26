package com.murilloskills.skills;

/**
 * Supported area mining patterns for Ultmine.
 */
public enum UltmineShape {
    S_3x3("murilloskills.ultmine.shape.3x3", 3, 3, 1),
    R_2x1("murilloskills.ultmine.shape.2x1", 2, 1, 1),
    LINE("murilloskills.ultmine.shape.line", 1, 1, 1),
    STAIRS("murilloskills.ultmine.shape.stairs", 1, 1, 5),
    SQUARE_20x20_D1("murilloskills.ultmine.shape.square_20x20_d1", 20, 20, 1),
    LEGACY("murilloskills.ultmine.shape.legacy", 1, 1, 1);

    private final String translationKey;
    private final int width;
    private final int height;
    private final int defaultDepth;

    UltmineShape(String translationKey, int width, int height, int defaultDepth) {
        this.translationKey = translationKey;
        this.width = width;
        this.height = height;
        this.defaultDepth = defaultDepth;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDefaultDepth() {
        return defaultDepth;
    }

    public UltmineShape next() {
        UltmineShape[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
