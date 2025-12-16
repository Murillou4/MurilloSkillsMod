package com.murilloskills.gui;

/**
 * Immutable color palette configuration for the ModInfoScreen GUI.
 * Uses Java 21 Record to encapsulate all UI color constants in a type-safe,
 * immutable structure.
 * 
 * Benefits:
 * - Eliminates magic numbers scattered throughout the codebase
 * - Provides a single source of truth for UI theming
 * - Makes it easy to swap color schemes (e.g., for different themes)
 */
public record ColorPalette(
        // Background & Overlay
        int bgOverlay,
        int bgGradientTop,
        int bgGradientBottom,

        // Panel Colors
        int panelBg,
        int panelBgHeader,
        int panelHighlight,
        int panelShadow,

        // Card/Section Colors
        int sectionBg,
        int sectionBgActive,
        int sectionBorder,
        int sectionBorderActive,

        // Text Colors
        int textGold,
        int textWhite,
        int textLight,
        int textGray,
        int textMuted,
        int textGreen,
        int textAqua,
        int textYellow,
        int textPurple,

        // Accent Colors
        int accentGold,
        int accentGreen,
        int accentBlue,

        // UI Elements
        int scrollbarBg,
        int scrollbarFg,
        int dividerColor) {
    /**
     * Factory method for the premium dark theme.
     * This is the default color scheme used throughout the mod.
     */
    public static ColorPalette premium() {
        return new ColorPalette(
                // Background & Overlay
                0xF0080810,
                0xFF101018,
                0xFF080810,

                // Panel Colors
                0xE8141420,
                0xF0181828,
                0x20FFFFFF,
                0x40000000,

                // Card/Section Colors
                0xD0181825,
                0xE0202030,
                0xFF2A2A3A,
                0xFF3A4A5A,

                // Text Colors
                0xFFFFD700,
                0xFFFFFFFF,
                0xFFDDDDEE,
                0xFFBBBBCC,
                0xFF666680,
                0xFF32CD32,
                0xFF00DDDD,
                0xFFFFEE44,
                0xFFDD88FF,

                // Accent Colors
                0xFFDDA520,
                0xFF22AA44,
                0xFF4488DD,

                // UI Elements
                0x30FFFFFF,
                0x80FFFFFF,
                0x40FFFFFF);
    }
}
