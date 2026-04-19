package com.murilloskills.skills;

/**
 * Controls how shape direction is resolved from the player's click.
 */
public enum UltPlaceRotationMode {
    AUTO("murilloskills.ultplace.rotation.auto"),
    FACE_LOCKED("murilloskills.ultplace.rotation.face_locked"),
    PLAYER_FACING("murilloskills.ultplace.rotation.player_facing");

    private final String translationKey;

    UltPlaceRotationMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public static UltPlaceRotationMode normalize(UltPlaceShape shape, UltPlaceRotationMode mode) {
        if (shape == null || !shape.supportsRotationMode()) {
            return AUTO;
        }
        return mode == null ? AUTO : mode;
    }
}
