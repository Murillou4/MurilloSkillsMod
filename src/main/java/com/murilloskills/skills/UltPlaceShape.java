package com.murilloskills.skills;

/**
 * Supported mass placement patterns for Builder UltPlace.
 */
public enum UltPlaceShape {
    PLANE_NXN("murilloskills.ultplace.shape.plane"),
    HORIZONTAL_BOX("murilloskills.ultplace.shape.horizontal_box"),
    LINE("murilloskills.ultplace.shape.line"),
    WALL("murilloskills.ultplace.shape.wall"),
    STAIRS("murilloskills.ultplace.shape.stairs"),
    COLUMN("murilloskills.ultplace.shape.column"),
    TUNNEL_3X3("murilloskills.ultplace.shape.tunnel_3x3"),
    CIRCLE("murilloskills.ultplace.shape.circle"),
    SPHERE_SHELL("murilloskills.ultplace.shape.sphere_shell"),
    SINGLE("murilloskills.ultplace.shape.single");

    private final String translationKey;

    UltPlaceShape(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public boolean supportsAnchorMode() {
        return switch (this) {
            case PLANE_NXN, HORIZONTAL_BOX, LINE, WALL, STAIRS, COLUMN, TUNNEL_3X3 -> true;
            default -> false;
        };
    }

    public boolean supportsRotationMode() {
        return switch (this) {
            case PLANE_NXN, HORIZONTAL_BOX, LINE, WALL, STAIRS, COLUMN, TUNNEL_3X3 -> true;
            default -> false;
        };
    }

    public boolean supportsHeight() {
        return this == HORIZONTAL_BOX;
    }

    public boolean supportsSpacing() {
        return switch (this) {
            case PLANE_NXN, HORIZONTAL_BOX, WALL, LINE, COLUMN -> true;
            default -> false;
        };
    }

    public static int getVariantCount(UltPlaceShape shape) {
        return switch (shape) {
            case STAIRS, COLUMN -> 2;
            default -> 1;
        };
    }

    public static String getVariantTranslationKey(UltPlaceShape shape, int variant) {
        return switch (shape) {
            case STAIRS -> variant == 1
                    ? "murilloskills.ultplace.variant.stairs.down"
                    : "murilloskills.ultplace.variant.stairs.up";
            case COLUMN -> variant == 1
                    ? "murilloskills.ultplace.variant.column.down"
                    : "murilloskills.ultplace.variant.column.up";
            default -> "murilloskills.ultplace.variant.default";
        };
    }
}
