package com.murilloskills.render;

import com.murilloskills.gui.ColorPalette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * HUD indicator for Area Planting (3x3) mode.
 * Displays a styled mini-panel in the bottom-left corner when enabled.
 */
public class AreaPlantingHud {

    private static final ColorPalette PALETTE = ColorPalette.premium();
    private static final int PADDING_H = 8;
    private static final int PADDING_V = 4;
    private static final int MARGIN = 10;
    private static final int HOTBAR_OFFSET = 54;

    private static boolean enabled = false;

    /**
     * Set the enabled state from server sync
     */
    public static void setEnabled(boolean state) {
        enabled = state;
    }

    /**
     * Check if area planting is enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Render the HUD indicator (called by HudRenderCallback)
     */
    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!enabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        Text label = Text.translatable("murilloskills.hud.area_planting");
        Text icon = Text.literal("🌾 ").formatted(Formatting.GREEN);
        Text fullText = icon.copy().append(label.copy().formatted(Formatting.GREEN, Formatting.BOLD));

        int textWidth = client.textRenderer.getWidth(fullText);
        int textHeight = client.textRenderer.fontHeight;

        int panelW = textWidth + PADDING_H * 2;
        int panelH = textHeight + PADDING_V * 2;
        int x = MARGIN;
        int y = context.getScaledWindowHeight() - HOTBAR_OFFSET - panelH;

        // Panel background
        context.fill(x, y, x + panelW, y + panelH, PALETTE.hudIndicatorBg());

        // Border
        context.fill(x, y, x + panelW, y + 1, PALETTE.hudIndicatorBorder());
        context.fill(x, y + panelH - 1, x + panelW, y + panelH, PALETTE.hudIndicatorBorder());
        context.fill(x, y, x + 1, y + panelH, PALETTE.hudIndicatorBorder());
        context.fill(x + panelW - 1, y, x + panelW, y + panelH, PALETTE.hudIndicatorBorder());

        // Left accent bar
        context.fill(x + 1, y + 1, x + 3, y + panelH - 1, PALETTE.hudIndicatorAccent());

        // Text
        context.drawTextWithShadow(client.textRenderer, fullText, x + PADDING_H, y + PADDING_V, PALETTE.textWhite());
    }
}
