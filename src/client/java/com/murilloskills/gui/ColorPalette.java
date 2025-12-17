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
        int textRed,

        // Accent Colors
        int accentGold,
        int accentGreen,
        int accentBlue,

        // UI Elements
        int scrollbarBg,
        int scrollbarFg,
        int dividerColor,

        // Progress Bar Colors
        int progressBarEmpty,
        int progressBarFill,
        int progressBarShine,

        // Status Background Colors
        int warningBg,
        int successBg,
        int infoBg,

        // Card Glow Effects
        int cardGlowActive,
        int cardGlowParagon,
        int cardGlowHover,

        // Skill-specific Colors (for consistent visual identity)
        int skillMiner,
        int skillWarrior,
        int skillFarmer,
        int skillArcher,
        int skillFisher,
        int skillBuilder,
        int skillBlacksmith,
        int skillExplorer) {
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
                0xFFFF6666,

                // Accent Colors
                0xFFDDA520,
                0xFF22AA44,
                0xFF4488DD,

                // UI Elements
                0x30FFFFFF,
                0x80FFFFFF,
                0x40FFFFFF,

                // Progress Bar Colors
                0xFF1A1A25,
                0xFF44AA66,
                0x40FFFFFF,

                // Status Background Colors
                0xD0302010,
                0xD0103020,
                0xD0102030,

                // Card Glow Effects
                0x3044FF66,
                0x40FFD700,
                0x20FFFFFF,

                // Skill-specific Colors
                0xFF88CCFF, // Miner - Light blue
                0xFFFF6666, // Warrior - Red
                0xFF88FF88, // Farmer - Green
                0xFFFFCC66, // Archer - Orange/Gold
                0xFF66CCFF, // Fisher - Cyan
                0xFFCC9966, // Builder - Brown/Tan
                0xFFCCCCCC, // Blacksmith - Silver
                0xFF66FF99 // Explorer - Mint green
        );
    }

    /**
     * Gets the color associated with a specific skill.
     */
    public int getSkillColor(com.murilloskills.skills.MurilloSkillsList skill) {
        return switch (skill) {
            case MINER -> skillMiner;
            case WARRIOR -> skillWarrior;
            case FARMER -> skillFarmer;
            case ARCHER -> skillArcher;
            case FISHER -> skillFisher;
            case BUILDER -> skillBuilder;
            case BLACKSMITH -> skillBlacksmith;
            case EXPLORER -> skillExplorer;
        };
    }
}
