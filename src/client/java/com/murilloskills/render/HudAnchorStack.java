package com.murilloskills.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Shared HUD slot allocator for small mode indicators.
 */
public final class HudAnchorStack {
    private static int bottomLeftOffset;
    private static int bottomRightOffset;

    private HudAnchorStack() {
    }

    public static void reset(DrawContext context, RenderTickCounter tickCounter) {
        bottomLeftOffset = 0;
        bottomRightOffset = 0;
    }

    public static int claimBottomLeft(DrawContext context, int panelHeight, int hotbarOffset, int gap) {
        int y = context.getScaledWindowHeight() - hotbarOffset - panelHeight - bottomLeftOffset;
        bottomLeftOffset += panelHeight + gap;
        return Math.max(gap, y);
    }

    public static int claimBottomRight(DrawContext context, int panelHeight, int marginY, int gap) {
        int y = context.getScaledWindowHeight() - marginY - panelHeight - bottomRightOffset;
        bottomRightOffset += panelHeight + gap;
        return Math.max(gap, y);
    }
}
