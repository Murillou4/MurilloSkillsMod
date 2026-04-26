package com.murilloskills.skills;

/**
 * Immutable UltPlace configuration shared by client preview and server execution.
 */
public record UltPlaceSelection(
        UltPlaceShape shape,
        int size,
        int length,
        int height,
        int variant,
        UltPlaceAnchorMode anchorMode,
        UltPlaceRotationMode rotationMode,
        int spacing) {

    public UltPlaceSelection(UltPlaceShape shape, int size, int length, int variant,
            UltPlaceAnchorMode anchorMode, UltPlaceRotationMode rotationMode) {
        this(shape, size, length, 1, variant, anchorMode, rotationMode, 1);
    }

    public UltPlaceSelection(UltPlaceShape shape, int size, int length, int variant,
            UltPlaceAnchorMode anchorMode, UltPlaceRotationMode rotationMode, int spacing) {
        this(shape, size, length, 1, variant, anchorMode, rotationMode, spacing);
    }
}
