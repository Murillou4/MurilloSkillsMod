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

    /**
     * Renders a styled progress bar with gradient fill and optional shine effect.
     * 
     * @param context    The draw context
     * @param x          X position
     * @param y          Y position
     * @param width      Bar width
     * @param height     Bar height
     * @param progress   Progress value (0.0 to 1.0)
     * @param emptyColor Background color for empty portion
     * @param fillColor  Color for filled portion
     * @param shineColor Color for shine highlight (set alpha to 0 to disable)
     */
    public static void renderProgressBar(DrawContext context, int x, int y, int width, int height,
            float progress, int emptyColor, int fillColor, int shineColor) {
        // Clamp progress
        progress = Math.max(0f, Math.min(1f, progress));

        // Background
        context.fill(x, y, x + width, y + height, emptyColor);

        // Border
        drawPanelBorder(context, x, y, width, height, 0x40FFFFFF);

        // Filled portion
        int fillWidth = (int) (width * progress);
        if (fillWidth > 0) {
            context.fill(x + 1, y + 1, x + fillWidth - 1, y + height - 1, fillColor);

            // Shine effect on top edge of fill
            if ((shineColor & 0xFF000000) != 0) {
                context.fill(x + 1, y + 1, x + fillWidth - 1, y + 2, shineColor);
            }

            // Gradient effect (lighter at top, darker at bottom)
            int lighterFill = blendColors(fillColor, 0xFFFFFFFF, 0.15f);
            context.fill(x + 1, y + 1, x + fillWidth - 1, y + height / 3, lighterFill);
        }
    }

    /**
     * Renders a glowing border effect around a card.
     * The glow pulses based on the provided animation tick.
     * 
     * @param context       The draw context
     * @param x             X position
     * @param y             Y position
     * @param width         Card width
     * @param height        Card height
     * @param glowColor     Base glow color
     * @param animationTick World time tick for animation
     */
    public static void renderGlowingBorder(DrawContext context, int x, int y, int width, int height,
            int glowColor, long animationTick) {
        // Calculate pulse intensity (0.5 to 1.0)
        float pulse = 0.5f + 0.5f * (float) Math.sin(animationTick * 0.1);
        int alpha = (int) (((glowColor >> 24) & 0xFF) * pulse);
        int pulsedColor = (alpha << 24) | (glowColor & 0x00FFFFFF);

        // Outer glow (3 layers)
        for (int i = 3; i > 0; i--) {
            int layerAlpha = alpha / (i + 1);
            int layerColor = (layerAlpha << 24) | (glowColor & 0x00FFFFFF);
            context.fill(x - i, y - i, x + width + i, y + height + i, layerColor);
        }

        // Inner bright border
        drawPanelBorder(context, x, y, width, height, pulsedColor);
    }

    /**
     * Renders an icon badge - a circular background with an item or text centered.
     * 
     * @param context   The draw context
     * @param x         Center X position
     * @param y         Center Y position
     * @param radius    Badge radius
     * @param bgColor   Background color
     * @param hasGlow   Whether to add glow effect
     * @param glowColor Glow color (if hasGlow is true)
     */
    public static void renderIconBadge(DrawContext context, int x, int y, int radius,
            int bgColor, boolean hasGlow, int glowColor) {
        // Simulate circle with multiple rectangles (Minecraft doesn't have native
        // circles)
        if (hasGlow) {
            // Outer glow
            context.fill(x - radius - 2, y - radius - 2, x + radius + 2, y + radius + 2,
                    (0x20 << 24) | (glowColor & 0x00FFFFFF));
        }

        // Main badge (rounded corners approximation)
        context.fill(x - radius, y - radius + 1, x + radius, y + radius - 1, bgColor);
        context.fill(x - radius + 1, y - radius, x + radius - 1, y + radius, bgColor);

        // Highlight
        context.fill(x - radius + 1, y - radius + 1, x + radius - 1, y - radius + 3, 0x20FFFFFF);
    }

    /**
     * Renders a star rating display for prestige levels.
     * 
     * @param context      The draw context
     * @param textRenderer Text renderer for drawing stars
     * @param x            X position
     * @param y            Y position
     * @param filledStars  Number of filled stars
     * @param maxStars     Maximum number of stars
     * @param filledColor  Color for filled stars
     * @param emptyColor   Color for empty stars
     */
    public static void renderStarRating(DrawContext context, TextRenderer textRenderer,
            int x, int y, int filledStars, int maxStars,
            int filledColor, int emptyColor) {
        StringBuilder starText = new StringBuilder();
        for (int i = 0; i < maxStars; i++) {
            starText.append(i < filledStars ? "★" : "☆");
        }

        // Draw stars with color based on whether they're filled
        int currentX = x;
        for (int i = 0; i < maxStars; i++) {
            String star = i < filledStars ? "★" : "☆";
            int color = i < filledStars ? filledColor : emptyColor;
            context.drawText(textRenderer, Text.literal(star), currentX, y, color, false);
            currentX += textRenderer.getWidth(star);
        }
    }

    /**
     * Renders a milestone marker for perk roadmaps.
     * 
     * @param context  The draw context
     * @param x        X position
     * @param y        Y position
     * @param size     Marker size
     * @param unlocked Whether the milestone is unlocked
     * @param isMaster Whether this is the master perk
     * @param palette  Color palette
     */
    public static void renderMilestoneMarker(DrawContext context, int x, int y, int size,
            boolean unlocked, boolean isMaster, ColorPalette palette) {
        int bgColor = unlocked
                ? (isMaster ? palette.accentGold() : palette.accentGreen())
                : palette.textMuted();
        int borderColor = isMaster ? 0xFFFFD700 : 0x80FFFFFF;

        // Marker background
        context.fill(x, y, x + size, y + size, bgColor);

        // Border
        drawPanelBorder(context, x, y, size, size, borderColor);

        // Checkmark or lock inside
        if (unlocked && size >= 6) {
            context.fill(x + size / 4, y + size / 2, x + size / 2, y + size - 2, 0xFFFFFFFF);
            context.fill(x + size / 2, y + 2, x + size - 2, y + size / 2 + 1, 0xFFFFFFFF);
        }
    }

    /**
     * Renders corner accents on a panel for a premium look.
     * 
     * @param context    The draw context
     * @param x          Panel X
     * @param y          Panel Y
     * @param width      Panel width
     * @param height     Panel height
     * @param cornerSize Size of corner accents
     * @param color      Accent color
     */
    public static void renderCornerAccents(DrawContext context, int x, int y, int width, int height,
            int cornerSize, int color) {
        // Top-left corner
        context.fill(x, y, x + cornerSize, y + 1, color);
        context.fill(x, y, x + 1, y + cornerSize, color);
        // Top-right corner
        context.fill(x + width - cornerSize, y, x + width, y + 1, color);
        context.fill(x + width - 1, y, x + width, y + cornerSize, color);
        // Bottom-left corner
        context.fill(x, y + height - 1, x + cornerSize, y + height, color);
        context.fill(x, y + height - cornerSize, x + 1, y + height, color);
        // Bottom-right corner
        context.fill(x + width - cornerSize, y + height - 1, x + width, y + height, color);
        context.fill(x + width - 1, y + height - cornerSize, x + width, y + height, color);
    }

    /**
     * Blends two colors together.
     * 
     * @param color1 First color
     * @param color2 Second color
     * @param ratio  Blend ratio (0.0 = all color1, 1.0 = all color2)
     * @return Blended color
     */
    public static int blendColors(int color1, int color2, float ratio) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
