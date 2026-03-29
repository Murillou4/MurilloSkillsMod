package com.murilloskills.render;

import com.murilloskills.gui.ColorPalette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * HUD indicator for Pathfinder speed boost (Explorer Level 45+).
 * Displays a styled mini-panel when the speed boost is active.
 */
public class PathfinderHud {

    private static final ColorPalette PALETTE = ColorPalette.premium();
    private static final int PADDING_H = 8;
    private static final int PADDING_V = 4;
    private static final int MARGIN = 10;
    private static final int HOTBAR_OFFSET = 54;

    private static boolean active = false;

    /**
     * Set the active state from server sync
     */
    public static void setActive(boolean state) {
        active = state;
    }

    /**
     * Check if pathfinder speed is active
     */
    public static boolean isActive() {
        return active;
    }

    /**
     * Render the HUD indicator (called by HudRenderCallback)
     */
    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!active) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        Text label = Text.translatable("murilloskills.hud.pathfinder");
        Text icon = Text.literal("\u26A1 ").formatted(Formatting.AQUA);
        Text fullText = icon.copy().append(label.copy().formatted(Formatting.AQUA, Formatting.BOLD));

        int textWidth = client.textRenderer.getWidth(fullText);
        int textHeight = client.textRenderer.fontHeight;

        int panelW = textWidth + PADDING_H * 2;
        int panelH = textHeight + PADDING_V * 2;
        int x = MARGIN;
        // Stack below AreaPlantingHud if both are active
        int yOffset = AreaPlantingHud.isEnabled() ? (panelH + 4) : 0;
        int y = context.getScaledWindowHeight() - HOTBAR_OFFSET - panelH - yOffset;

        // Panel background (darker blue tint)
        context.fill(x, y, x + panelW, y + panelH, PALETTE.hudIndicatorBg());

        // Border
        context.fill(x, y, x + panelW, y + 1, 0xFF00BBFF);
        context.fill(x, y + panelH - 1, x + panelW, y + panelH, 0xFF00BBFF);
        context.fill(x, y, x + 1, y + panelH, 0xFF00BBFF);
        context.fill(x + panelW - 1, y, x + panelW, y + panelH, 0xFF00BBFF);

        // Left accent bar (cyan)
        context.fill(x + 1, y + 1, x + 3, y + panelH - 1, 0xFF00DDFF);

        // Text
        context.drawTextWithShadow(client.textRenderer, fullText, x + PADDING_H, y + PADDING_V, PALETTE.textWhite());
    }
}
