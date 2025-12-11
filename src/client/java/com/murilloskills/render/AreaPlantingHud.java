package com.murilloskills.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * HUD indicator for Area Planting (3x3) mode.
 * Displays a small icon in the corner when enabled.
 */
public class AreaPlantingHud {

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

        // Position: bottom-left corner, above hotbar
        int x = 10;
        int y = context.getScaledWindowHeight() - 60;

        // Draw indicator text
        Text text = Text.translatable("murilloskills.hud.area_planting").formatted(Formatting.GREEN, Formatting.BOLD);
        context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFFFFF);
    }
}
