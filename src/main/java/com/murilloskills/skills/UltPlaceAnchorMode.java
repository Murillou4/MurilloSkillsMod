package com.murilloskills.skills;

/**
 * Controls how a shape is anchored relative to the first placed block.
 */
public enum UltPlaceAnchorMode {
    CENTER("murilloskills.ultplace.anchor.center"),
    LEADING_EDGE("murilloskills.ultplace.anchor.leading_edge"),
    TRAILING_EDGE("murilloskills.ultplace.anchor.trailing_edge");

    private final String translationKey;

    UltPlaceAnchorMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public static UltPlaceAnchorMode normalize(UltPlaceShape shape, UltPlaceAnchorMode mode) {
        if (shape == null || !shape.supportsAnchorMode()) {
            return CENTER;
        }
        return mode == null ? CENTER : mode;
    }
}
