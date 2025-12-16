package com.murilloskills.gui.renderer;

import com.murilloskills.gui.ColorPalette;
import net.minecraft.client.font.TextRenderer;

/**
 * Immutable context object containing all necessary rendering state.
 * Passed to TabRenderer implementations to provide access to layout,
 * scroll position, and styling information.
 * 
 * Uses Java 21 Record pattern for immutability and clean API.
 */
public record RenderContext(
        int contentX,
        int contentY,
        int contentWidth,
        int contentHeight,
        int scrollOffset,
        int textMaxWidth,
        int sectionPadding,
        ColorPalette palette,
        TextRenderer textRenderer) {
    /**
     * Calculates the Y position for rendering, accounting for scroll offset.
     */
    public int scrolledY(int baseY) {
        return baseY - scrollOffset;
    }

    /**
     * Calculates the left padding position for content.
     */
    public int paddedX() {
        return contentX + sectionPadding;
    }

    /**
     * Calculates the top padding position for content.
     */
    public int paddedY() {
        return contentY + sectionPadding;
    }
}
