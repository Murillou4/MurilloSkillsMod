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

        int panelWidth = 250;
        int entryHeight = 42;
        int headerHeight = 30;
        int bonusHeight = ClientSkillData.areAllChallengesComplete() ? 22 : 0;
        int panelHeight = headerHeight + (challenges.size() * entryHeight) + bonusHeight + 6;
        int panelX = 8;
        int panelY = height - panelHeight - 8;

        // Panel background with shadow
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

        // Header completion indicator (small dots)
        int dotX = panelX + panelWidth - 8 - (challenges.size() * 8);
        for (int i = 0; i < challenges.size(); i++) {
            boolean done = i < completed;
            int dotColor = done ? palette.accentGreen() : 0x40FFFFFF;
            context.fill(dotX + (i * 8), panelY + 10, dotX + (i * 8) + 5, panelY + 15, dotColor);
        }

        // Divider
        RenderingHelper.renderDivider(context, panelX + 8, panelY + 22, panelWidth - 16, palette.dividerColor());

        // Render each challenge
        int y = panelY + headerHeight;
        int contentPadding = 10;

        for (var challenge : challenges) {
            int entryLeft = panelX + 4;
            int entryRight = panelX + panelWidth - 4;
            int entryTop = y;
            int entryBottom = y + entryHeight - 4;

            // Entry background
            int entryBg = challenge.completed() ? palette.successBg() : 0x18FFFFFF;
            context.fill(entryLeft, entryTop, entryRight, entryBottom, entryBg);

            // Left accent bar
            int accentColor = challenge.completed() ? palette.accentGreen() : palette.accentGold();
            context.fill(entryLeft, entryTop + 2, entryLeft + 3, entryBottom - 2, accentColor);

            // Challenge name (row 1) - truncated to fit with status icon
            Text typeText = Text.translatable("murilloskills.challenge." + challenge.type().toLowerCase());
            String nameStr = typeText.getString();
            int statusWidth = challenge.completed() ? 14 : textRenderer.getWidth("+" + challenge.xpReward() + "xp") + 4;
            int maxNameWidth = (entryRight - entryLeft) - contentPadding - statusWidth - 8;

            if (textRenderer.getWidth(nameStr) > maxNameWidth) {
                while (nameStr.length() > 1 && textRenderer.getWidth(nameStr + "..") > maxNameWidth) {
                    nameStr = nameStr.substring(0, nameStr.length() - 1);
                }
                nameStr += "..";
            }

            int typeColor = challenge.completed() ? palette.textGreen() : palette.textLight();
            context.drawTextWithShadow(textRenderer, Text.literal(nameStr),
                    entryLeft + contentPadding, entryTop + 4, typeColor);

            // Status indicator (right side of name row)
            if (challenge.completed()) {
                context.drawTextWithShadow(textRenderer, Text.literal("✓"),
                        entryRight - 14, entryTop + 4, palette.textGreen());
            } else {
                String xpStr = "+" + challenge.xpReward() + "xp";
                int xpWidth = textRenderer.getWidth(xpStr);
                context.drawText(textRenderer, Text.literal(xpStr),
                        entryRight - xpWidth - 6, entryTop + 4, palette.textMuted(), false);
            }

            // Progress bar (row 2) - width calculated to leave room for progress text
            String progressStr = challenge.progress() + "/" + challenge.target();
            int progressTextWidth = textRenderer.getWidth(progressStr);
            int barLeft = entryLeft + contentPadding;
            int barWidth = (entryRight - barLeft) - progressTextWidth - 12;
            barWidth = Math.max(barWidth, 40); // Minimum bar width

            float progress = challenge.getProgressPercentage();
            int fillColor = challenge.completed() ? palette.accentGreen() : palette.progressBarFill();
            RenderingHelper.renderProgressBar(context, barLeft, entryTop + 18, barWidth, 8, progress,
                    palette.progressBarEmpty(), fillColor, palette.progressBarShine());

            // Progress text (right-aligned after bar)
            int progressTextColor = challenge.completed() ? palette.textGreen() : palette.textMuted();
            context.drawText(textRenderer, Text.literal(progressStr),
                    barLeft + barWidth + 4, entryTop + 18, progressTextColor, false);

            // Percentage text below bar (subtle)
            if (!challenge.completed()) {
                int percent = (int) (progress * 100);
                String percentStr = percent + "%";
                int percentWidth = textRenderer.getWidth(percentStr);
                context.drawText(textRenderer, Text.literal(percentStr),
                        barLeft + (barWidth - percentWidth) / 2, entryTop + 28,
                        0x40FFFFFF, false);
            }

            y += entryHeight;
        }

        // Bonus indicator when all complete
        if (ClientSkillData.areAllChallengesComplete()) {
            int bonusY = y + 2;
            context.fill(panelX + 4, bonusY, panelX + panelWidth - 4, bonusY + 18, palette.cardGlowParagon());
            RenderingHelper.drawPanelBorder(context, panelX + 4, bonusY, panelWidth - 8, 18, palette.accentGold());
            Text bonus = Text.translatable("murilloskills.gui.challenges.all_complete");
            int bonusWidth = textRenderer.getWidth(bonus);
            context.drawTextWithShadow(textRenderer, bonus,
                    panelX + (panelWidth - bonusWidth) / 2, bonusY + 5, palette.textGold());
        }
    }
}
