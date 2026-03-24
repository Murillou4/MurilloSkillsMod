package com.murilloskills.gui.renderer;

import com.murilloskills.data.ClientSkillData;
import com.murilloskills.gui.ColorPalette;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Renderer for the Daily Challenges panel in the SkillsScreen.
 */
public class DailyChallengeRenderer {

    public static void renderPanel(DrawContext context, TextRenderer textRenderer, int width, int height,
            int mouseX, int mouseY, ColorPalette palette) {
        var challenges = ClientSkillData.getDailyChallenges();
        if (challenges.isEmpty())
            return;

        int panelWidth = 220;
        int panelHeight = 35 + (challenges.size() * 38);
        int panelX = 8; // Left side
        int panelY = height - panelHeight - 8; // Bottom aligned

        // Panel background
        context.fill(panelX - 2, panelY - 2, panelX + panelWidth + 2, panelY + panelHeight + 2, palette.panelShadow());
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, palette.panelBg());

        // Border and corner accents
        RenderingHelper.drawPanelBorder(context, panelX, panelY, panelWidth, panelHeight, palette.sectionBorder());
        RenderingHelper.renderCornerAccents(context, panelX, panelY, panelWidth, panelHeight, 4, palette.accentGold());

        // Inner highlight
        context.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + 2, palette.panelHighlight());

        // Header
        int completed = ClientSkillData.getCompletedChallengeCount();
        Text header = Text.translatable("murilloskills.gui.challenges.title", completed, challenges.size());
        context.drawTextWithShadow(textRenderer, header, panelX + 8, panelY + 8, palette.textGold());

        // Divider
        RenderingHelper.renderDivider(context, panelX + 8, panelY + 22, panelWidth - 16, palette.dividerColor());

        // Render each challenge
        int y = panelY + 28;
        for (var challenge : challenges) {
            if (challenge.completed()) {
                context.fill(panelX + 4, y - 2, panelX + panelWidth - 4, y + 30, palette.successBg());
            }

            Text typeText = Text.translatable("murilloskills.challenge." + challenge.type().toLowerCase());
            int typeColor = challenge.completed() ? palette.textGreen() : palette.textLight();
            context.drawTextWithShadow(textRenderer, typeText, panelX + 8, y + 2, typeColor);

            if (challenge.completed()) {
                context.drawText(textRenderer, Text.literal("✓"), panelX + panelWidth - 16, y + 2, palette.textGreen(), false);
            }

            // Progress bar
            float progress = challenge.getProgressPercentage();
            int fillColor = challenge.completed() ? palette.accentGreen() : palette.progressBarFill();
            RenderingHelper.renderProgressBar(context, panelX + 8, y + 14, panelWidth - 50, 8, progress,
                    palette.progressBarEmpty(), fillColor, palette.progressBarShine());

            // Progress text
            String progressStr = challenge.progress() + "/" + challenge.target();
            int progressWidth = textRenderer.getWidth(progressStr);
            context.drawText(textRenderer, Text.literal(progressStr),
                    panelX + panelWidth - progressWidth - 8, y + 14,
                    challenge.completed() ? palette.textGreen() : palette.textMuted(), false);

            y += 38;
        }

        // Bonus indicator
        if (ClientSkillData.areAllChallengesComplete()) {
            int bonusY = y + 4;
            context.fill(panelX + 4, bonusY - 2, panelX + panelWidth - 4, bonusY + 14, palette.cardGlowParagon());
            Text bonus = Text.translatable("murilloskills.gui.challenges.all_complete");
            int bonusWidth = textRenderer.getWidth(bonus);
            context.drawTextWithShadow(textRenderer, bonus, panelX + (panelWidth - bonusWidth) / 2, bonusY + 2, palette.textGold());
        }
    }
}
