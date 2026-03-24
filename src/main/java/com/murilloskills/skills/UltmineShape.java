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

    /**
     * Returns the number of variants available for the given shape.
     * Variant 0 is always the default behavior.
     */
    public static int getVariantCount(UltmineShape shape) {
        return switch (shape) {
            case STAIRS -> 2;            // 0=UP, 1=DOWN
            case SQUARE_20x20_D1 -> 3;   // 0=Horizontal, 1=Vertical N/S, 2=Vertical E/W
            case R_2x1 -> 2;             // 0=default (wide), 1=rotated (tall)
            default -> 1;
        };
    }

    /**
     * Returns the translation key for a specific variant of a shape.
     */
    public static String getVariantTranslationKey(UltmineShape shape, int variant) {
        return switch (shape) {
            case STAIRS -> variant == 0
                    ? "murilloskills.ultmine.variant.stairs.up"
                    : "murilloskills.ultmine.variant.stairs.down";
            case SQUARE_20x20_D1 -> switch (variant) {
                case 1 -> "murilloskills.ultmine.variant.square.vertical_ns";
                case 2 -> "murilloskills.ultmine.variant.square.vertical_ew";
                default -> "murilloskills.ultmine.variant.square.horizontal";
            };
            case R_2x1 -> variant == 0
                    ? "murilloskills.ultmine.variant.2x1.wide"
                    : "murilloskills.ultmine.variant.2x1.tall";
            default -> "murilloskills.ultmine.variant.default";
        };
    }
}
