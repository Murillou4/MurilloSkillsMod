package com.murilloskills.gui;

/**
 * Immutable color palette configuration for all mod GUI elements.
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
        int scrollbarActive,
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
        int skillExplorer,

        // Ore-specific Colors (for ore filter UI and radar accents)
        int oreCoal,
        int oreCopper,
        int oreIron,
        int oreGold,
        int oreLapis,
        int oreRedstone,
        int oreDiamond,
        int oreEmerald,
        int oreAncientDebris,
        int oreNetherQuartz,
        int oreNetherGold,

        // HUD Indicator Colors
        int hudIndicatorBg,
        int hudIndicatorBorder,
        int hudIndicatorAccent,

        // Card State Colors (skill cards in SkillsScreen/ModInfoScreen)
        int cardBgPending,
        int cardBgParagon,
        int cardBgLocked,
        int cardBorderLocked,
        int cardBgSelected,

        // Status Text Colors
        int statusInactive,
        int statusReady,
        int statusCooldown,
        int statusActive,

        // Progress Bar State Colors
        int progressLockedFill,
        int progressLockedShine,
        int progressMaxShine,

        // Perk/Roadmap Colors
        int perkCloseMarker,

        // Prestige Level Colors (tiers 1-10)
        int prestigeGreen,
        int prestigeCyan,
        int prestigeYellow,
        int prestigeMagenta,
        int prestigeGold,

        // Header/Gradient Base Colors
        int headerGradientBase,
        int headerAccentLine,

        // Guide/Section Card Backgrounds
        int cardBgSubtle,
        int alternatingRowBg,

        // Synergy Card Colors
        int synergyActiveBg,
        int synergyBadgeBg,
        int synergyActiveGlow,
        int bonusBarBg,
        int bonusBarBorderActive,
        int bonusBarBorderInactive,

        // Info Box Border
        int infoBoxBorder,

        // World Highlight Colors (ARGB for treasure/outline renderers)
        int treasureHighlight) {

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
                0xC0FFFFFF,
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
                0xFF66FF99, // Explorer - Mint green

                // Ore-specific Colors
                0xFF555555, // Coal - Dark gray
                0xFFE87B35, // Copper - Orange
                0xFFD8AF93, // Iron - Tan
                0xFFFFD700, // Gold - Yellow
                0xFF2626CC, // Lapis - Deep blue
                0xFFFF3333, // Redstone - Red
                0xFF4AEDD9, // Diamond - Cyan
                0xFF00FF66, // Emerald - Green
                0xFF7B4F3A, // Ancient Debris - Brown
                0xFFE8E4D8, // Nether Quartz - Off-white
                0xFFFFAA00, // Nether Gold - Amber

                // HUD Indicator Colors
                0xC0101018, // Semi-transparent dark bg
                0xFF2A2A3A, // Subtle border
                0xFF22AA44, // Green accent

                // Card State Colors
                0xE8181830, // Pending selection - blue tint
                0xF0201810, // Paragon - warm amber tint
                0xD0101015, // Locked - very dark
                0xFF1A1A20, // Locked border - near-black
                0xE8102018, // Selected - green tint

                // Status Text Colors
                0xFFAA0000, // Inactive - dark red
                0xFF00FF00, // Ready - bright green
                0xFFFF5555, // Cooldown - soft red
                0xFF00AA00, // Active - mid green

                // Progress Bar State Colors
                0xFF663344, // Locked fill - muted red
                0x20FF4444, // Locked shine - faint red
                0x60FFFFFF, // Max level shine - bright white

                // Perk/Roadmap Colors
                0xFFAAAA44, // Close-to-unlock marker - yellow-olive

                // Prestige Level Colors
                0xFF88FF88, // Prestige 1-2 - Light green
                0xFF88FFFF, // Prestige 3-4 - Cyan
                0xFFFFFF88, // Prestige 5-6 - Yellow
                0xFFFF88FF, // Prestige 7-8 - Magenta
                0xFFFFDD00, // Prestige 9-10 - Gold

                // Header/Gradient Base Colors
                0x101018,   // Dark blue-gray header base
                0x30FFFFFF, // Subtle accent line

                // Guide/Section Card Backgrounds
                0xC0141420, // Subtle dark card bg (guide sections)
                0x15FFFFFF, // Alternating row highlight

                // Synergy Card Colors
                0xD8102820, // Active synergy - green tint
                0xC0103010, // Synergy badge bg - dark green
                0x30AAFFAA, // Active synergy glow - soft green
                0x80101018, // Bonus bar bg - semi-transparent
                0xFF225533, // Bonus bar border active - dark green
                0xFF333344, // Bonus bar border inactive - dark purple

                // Info Box Border
                0x60FFFFFF, // Semi-transparent white border

                // World Highlight Colors
                0xFF00E6E6  // Treasure - Teal/Cyan
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

    /**
     * Gets the accent color for a specific ore type.
     */
    public int getOreColor(com.murilloskills.network.MinerScanResultPayload.OreType ore) {
        return switch (ore) {
            case COAL -> oreCoal;
            case COPPER -> oreCopper;
            case IRON -> oreIron;
            case GOLD -> oreGold;
            case LAPIS -> oreLapis;
            case REDSTONE -> oreRedstone;
            case DIAMOND -> oreDiamond;
            case EMERALD -> oreEmerald;
            case ANCIENT_DEBRIS -> oreAncientDebris;
            case NETHER_QUARTZ -> oreNetherQuartz;
            case NETHER_GOLD -> oreNetherGold;
            case OTHER -> textMuted;
        };
    }

    /**
     * Gets the display color for a prestige level (1-10).
     */
    public int getPrestigeColor(int prestige) {
        return switch (prestige) {
            case 1, 2 -> prestigeGreen;
            case 3, 4 -> prestigeCyan;
            case 5, 6 -> prestigeYellow;
            case 7, 8 -> prestigeMagenta;
            case 9, 10 -> prestigeGold;
            default -> textWhite;
        };
    }

    /**
     * Extracts the float R component (0.0-1.0) from an ARGB int color.
     */
    public static float redF(int color) {
        return ((color >> 16) & 0xFF) / 255.0f;
    }

    /**
     * Extracts the float G component (0.0-1.0) from an ARGB int color.
     */
    public static float greenF(int color) {
        return ((color >> 8) & 0xFF) / 255.0f;
    }

    /**
     * Extracts the float B component (0.0-1.0) from an ARGB int color.
     */
    public static float blueF(int color) {
        return (color & 0xFF) / 255.0f;
    }
}
