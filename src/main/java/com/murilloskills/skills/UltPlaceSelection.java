package com.murilloskills.skills;

/**
 * Immutable UltPlace configuration shared by client preview and server execution.
 */
public record UltPlaceSelection(
        UltPlaceShape shape,
        int size,
        int length,
        int variant,
        UltPlaceAnchorMode anchorMode,
        UltPlaceRotationMode rotationMode) {
}
