package com.murilloskills.gui.renderer;

import com.murilloskills.gui.ColorPalette;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Utility class providing common rendering operations used across multiple
 * tab renderers. Eliminates code duplication for drawing panels, borders,
 * dividers, and other UI elements.
 * 
 * All methods are static and stateless - this is a pure utility class.
 */
public final class RenderingHelper {

    private RenderingHelper() {
        // Prevent instantiation - utility class
    }

    /**
     * Draws a panel border around the specified area.
     */
    public static void drawPanelBorder(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color); // Top
        context.fill(x, y + h - 1, x + w, y + h, color); // Bottom
        context.fill(x, y, x + 1, y + h, color); // Left
        context.fill(x + w - 1, y, x + w, y + h, color); // Right
    }

    /**
     * Renders a horizontal divider line.
     */
    public static void renderDivider(DrawContext context, int x, int y, int width, int color) {
        if (width > 0) {
            context.fill(x, y, x + width, y + 1, color);
        }
    }

    /**
     * Renders a section title with decorative lines on both sides.
     */
    public static void renderSectionTitle(DrawContext context, TextRenderer textRenderer,
            int x, int y, String title,
            int contentWidth, int sectionPadding,
            ColorPalette palette) {
        // Decorative line before
        renderDivider(context, x, y + 5, 20, palette.dividerColor());

        // Title text
        context.drawTextWithShadow(textRenderer,
                Text.literal(title).formatted(Formatting.GOLD, Formatting.BOLD),
                x + 25, y, palette.textGold());

        // Decorative line after
        int titleWidth = textRenderer.getWidth(title);
        renderDivider(context, x + 30 + titleWidth, y + 5,
                contentWidth - sectionPadding * 2 - titleWidth - 50, palette.dividerColor());
    }

    /**
     * Renders a subsection header with a simple prefix.
     */
    public static void renderSubsectionHeader(DrawContext context, TextRenderer textRenderer,
            int x, int y, String title, ColorPalette palette) {
        context.drawText(textRenderer,
                Text.literal("> " + title).formatted(Formatting.YELLOW),
                x, y, palette.textYellow(), false);
    }

    /**
     * Renders an info box with background, border, and left accent.
     */
    public static void renderInfoBox(DrawContext context, int x, int y, int width, int height,
            int accentColor) {
        // Background
        context.fill(x, y, x + width, y + height, 0xC0101018);
        // Border
        drawPanelBorder(context, x, y, width, height, 0x60FFFFFF);
        // Left accent
        context.fill(x, y + 2, x + 3, y + height - 2, accentColor);
    }
}
