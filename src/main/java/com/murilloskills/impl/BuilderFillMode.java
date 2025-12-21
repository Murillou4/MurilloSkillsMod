package com.murilloskills.impl;

/**
 * Fill modes for the Builder's Creative Brush master ability.
 * Each mode creates a different geometric shape when filling an area.
 */
public enum BuilderFillMode {
    /**
     * Standard rectangular box (cuboid) - default mode
     */
    CUBOID("murilloskills.builder.mode_cuboid"),

    /**
     * Spherical or ellipsoid shape - perfect for domes and organic structures
     */
    SPHERE("murilloskills.builder.mode_sphere"),

    /**
     * Cylindrical shape - great for towers, arenas, and pillars
     * Can be vertical or horizontal based on player preference
     */
    CYLINDER("murilloskills.builder.mode_cylinder"),

    /**
     * Pyramid shape - ideal for roofs and temples
     * Tapers from base to tip
     */
    PYRAMID("murilloskills.builder.mode_pyramid"),

    /**
     * Walls only - fills only the vertical sides
     * Does not fill floor or ceiling, unlike hollow mode
     */
    WALL("murilloskills.builder.mode_wall");

    private final String translationKey;

    BuilderFillMode(String translationKey) {
        this.translationKey = translationKey;
    }

    /**
     * Get the translation key for this mode's display name
     */
    public String getTranslationKey() {
        return translationKey;
    }

    /**
     * Get the next mode in the cycle (for toggling)
     */
    public BuilderFillMode next() {
        BuilderFillMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    /**
     * Get the previous mode in the cycle
     */
    public BuilderFillMode previous() {
        BuilderFillMode[] values = values();
        int prevIndex = (this.ordinal() - 1 + values.length) % values.length;
        return values[prevIndex];
    }
}
