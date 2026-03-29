package com.murilloskills.render;

import com.murilloskills.gui.ColorPalette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * HUD indicator for Auto-Torch mode (Miner Level 25+).
 * Displays a styled mini-panel when auto-torch is enabled.
 */
public class AutoTorchHud {

    private static final ColorPalette PALETTE = ColorPalette.premium();
    private static final int PADDING_H = 8;
    private static final int PADDING_V = 4;
    private static final int MARGIN = 10;
    private static final int HOTBAR_OFFSET = 54;

    private static boolean enabled = false;

    public static void setEnabled(boolean state) {
        enabled = state;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!enabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        Text label = Text.translatable("murilloskills.hud.auto_torch");
        Text icon = Text.literal("\uD83D\uDD26 ").formatted(Formatting.YELLOW);
        Text fullText = icon.copy().append(label.copy().formatted(Formatting.YELLOW, Formatting.BOLD));

        int textWidth = client.textRenderer.getWidth(fullText);
        int textHeight = client.textRenderer.fontHeight;

        int panelW = textWidth + PADDING_H * 2;
        int panelH = textHeight + PADDING_V * 2;
        int x = MARGIN;
        // Stack below other HUD indicators
        int yOffset = 0;
        if (AreaPlantingHud.isEnabled()) yOffset += panelH + 4;
        if (PathfinderHud.isActive()) yOffset += panelH + 4;
        int y = context.getScaledWindowHeight() - HOTBAR_OFFSET - panelH - yOffset;

        // Panel background
        context.fill(x, y, x + panelW, y + panelH, PALETTE.hudIndicatorBg());

        // Border (orange/gold for torch)
        context.fill(x, y, x + panelW, y + 1, 0xFFFF8800);
        context.fill(x, y + panelH - 1, x + panelW, y + panelH, 0xFFFF8800);
        context.fill(x, y, x + 1, y + panelH, 0xFFFF8800);
        context.fill(x + panelW - 1, y, x + panelW, y + panelH, 0xFFFF8800);

        // Left accent bar (orange)
        context.fill(x + 1, y + 1, x + 3, y + panelH - 1, 0xFFFFAA00);

        // Text
        context.drawTextWithShadow(client.textRenderer, fullText, x + PADDING_H, y + PADDING_V, PALETTE.textWhite());
    }
}
