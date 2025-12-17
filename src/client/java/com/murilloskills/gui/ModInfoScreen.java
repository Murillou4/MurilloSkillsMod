package com.murilloskills.gui;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.gui.renderer.RenderingHelper;
import com.murilloskills.gui.data.SkillUiData;
import com.murilloskills.data.ClientSkillData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Information screen that displays all mod information including:
 * - Player's current skill status
 * - Active synergies
 * - Prestige system bonuses
 * - Complete perk guide for all skills
 */
public class ModInfoScreen extends Screen {

        // === COLOR PALETTE (Java 21 Record) ===
        private static final ColorPalette PALETTE = ColorPalette.premium();

        // Tab system
        private enum Tab {
                STATUS, SYNERGIES, PRESTIGE, PERKS
        }

        private Tab currentTab = Tab.STATUS;
        private final ScrollController scrollController = new ScrollController();
        private static final int LINE_HEIGHT = 12;
        private static final int SECTION_PADDING = 12;

        // Layout
        private int contentX, contentY, contentWidth, contentHeight;
        private int textMaxWidth;
        private int headerHeight = 48;
        private final Screen parent;

        // Animation
        private float tabTransition = 0f;
        private Tab lastTab = Tab.STATUS;

        // === SYNERGY & PERK DEFINITIONS ===
        // Removed duplicated definitions - migrated to SkillUiData

        public ModInfoScreen(Screen parent) {
                super(Text.translatable("murilloskills.info.title"));
                this.parent = parent;
        }

        @Override
        protected void init() {
                super.init();
                this.clearChildren();

                // Layout calculation
                int margin = 15;
                contentX = margin;
                contentY = headerHeight + 8;
                contentWidth = this.width - (margin * 2);
                contentHeight = this.height - contentY - 35;
                textMaxWidth = contentWidth - 40;

                // Custom styled tab buttons
                int tabWidth = 75;
                int tabHeight = 22;
                int tabY = headerHeight - 26;
                int totalTabWidth = tabWidth * 4 + 9;
                int tabStartX = (this.width - totalTabWidth) / 2;

                addTabButton(tabStartX, tabY, tabWidth, tabHeight, Tab.STATUS, "murilloskills.info.tab.status");
                addTabButton(tabStartX + tabWidth + 3, tabY, tabWidth, tabHeight, Tab.SYNERGIES,
                                "murilloskills.info.tab.synergies");
                addTabButton(tabStartX + (tabWidth + 3) * 2, tabY, tabWidth, tabHeight, Tab.PRESTIGE,
                                "murilloskills.info.tab.prestige");
                addTabButton(tabStartX + (tabWidth + 3) * 3, tabY, tabWidth, tabHeight, Tab.PERKS,
                                "murilloskills.info.tab.perks");

                // Styled back button
                this.addDrawableChild(ButtonWidget.builder(
                                Text.translatable("murilloskills.info.back"),
                                btn -> this.close())
                                .dimensions(10, this.height - 28, 80, 20)
                                .build());

                calculateMaxScroll();
        }

        private void addTabButton(int x, int y, int width, int height, Tab tab, String translationKey) {
                this.addDrawableChild(ButtonWidget.builder(
                                Text.translatable(translationKey),
                                btn -> {
                                        if (currentTab != tab) {
                                                lastTab = currentTab;
                                                currentTab = tab;
                                                scrollController.reset();
                                                tabTransition = 0f;
                                                calculateMaxScroll();
                                        }
                                })
                                .dimensions(x, y, width, height)
                                .build());
        }

        @Override
        public void close() {
                MinecraftClient.getInstance().setScreen(parent);
        }

        private void calculateMaxScroll() {
                int totalContentHeight = switch (currentTab) {
                        case STATUS -> calculateStatusHeight();
                        case SYNERGIES -> calculateSynergiesHeight();
                        case PRESTIGE -> calculatePrestigeHeight();
                        case PERKS -> calculatePerksHeight();
                };
                scrollController.updateMaxScroll(totalContentHeight, contentHeight);
        }

        private int calculateStatusHeight() {
                int height = 40;
                List<MurilloSkillsList> selectedSkills = ClientSkillData.getSelectedSkills();
                if (selectedSkills.isEmpty()) {
                        height += 30;
                } else {
                        for (MurilloSkillsList skill : selectedSkills) {
                                var stats = ClientSkillData.get(skill);
                                height += 56; // 50px card + 6px spacing
                                if (stats.prestige > 0)
                                        height += 10;
                        }
                }
                height += 50;
                for (SkillUiData.SynergyInfo synergy : SkillUiData.SYNERGIES) {
                        if (selectedSkills.contains(synergy.skill1()) && selectedSkills.contains(synergy.skill2())) {
                                height += 25;
                        }
                }
                return height + 30;
        }

        private int calculateSynergiesHeight() {
                return 80 + SkillUiData.SYNERGIES.size() * 78; // Updated for taller cards
        }

        private int calculatePrestigeHeight() {
                return 350;
        }

        private int calculatePerksHeight() {
                int height = 40;
                for (MurilloSkillsList skill : MurilloSkillsList.values()) {
                        height += 30;
                        List<SkillUiData.PerkInfo> perks = SkillUiData.SKILL_PERKS.get(skill);
                        if (perks != null)
                                height += perks.size() * 22;
                        height += 15;
                }
                return height;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
                // Update animation
                if (tabTransition < 1f) {
                        tabTransition = Math.min(1f, tabTransition + delta * 0.15f);
                }

                // Gradient background
                renderGradientBackground(context);

                // Decorative header
                renderHeader(context);

                // Main content panel with modern styling
                renderContentPanel(context);

                // Enable scissor for content clipping
                context.enableScissor(contentX + 2, contentY + 2, contentX + contentWidth - 2,
                                contentY + contentHeight - 2);

                // Tab content with styling
                switch (currentTab) {
                        case STATUS -> renderStatusTab(context);
                        case SYNERGIES -> renderSynergiesTab(context);
                        case PRESTIGE -> renderPrestigeTab(context);
                        case PERKS -> renderPerksTab(context);
                }

                context.disableScissor();

                // Scrollbar
                if (scrollController.getMaxScrollOffset() > 0) {
                        scrollController.renderScrollbar(context, contentX, contentY, contentWidth, contentHeight,
                                        PALETTE);
                }

                // Widgets on top
                super.render(context, mouseX, mouseY, delta);
        }

        private void renderGradientBackground(DrawContext context) {
                // Vertical gradient
                for (int y = 0; y < this.height; y++) {
                        float ratio = (float) y / this.height;
                        int r = (int) (8 + ratio * 4);
                        int g = (int) (8 + ratio * 4);
                        int b = (int) (16 + ratio * 8);
                        int color = 0xF0000000 | (r << 16) | (g << 8) | b;
                        context.fill(0, y, this.width, y + 1, color);
                }

                // Subtle vignette effect
                renderVignette(context);
        }

        private void renderVignette(DrawContext context) {
                int size = Math.min(this.width, this.height) / 3;
                // Corner darkening
                for (int i = 0; i < 6; i++) {
                        int alpha = (int) (0x12 * (1 - (float) i / 6));
                        if (alpha <= 0)
                                continue;
                        int color = alpha << 24;
                        int offset = size * i / 6;
                        // Top-left
                        context.fill(0, 0, size - offset, size - offset, color);
                        // Top-right
                        context.fill(this.width - size + offset, 0, this.width, size - offset, color);
                        // Bottom-left
                        context.fill(0, this.height - size + offset, size - offset, this.height, color);
                        // Bottom-right
                        context.fill(this.width - size + offset, this.height - size + offset, this.width, this.height,
                                        color);
                }
        }

        private void renderHeader(DrawContext context) {
                // Header gradient background
                for (int y = 0; y < headerHeight; y++) {
                        float ratio = (float) y / headerHeight;
                        int alpha = (int) (0xF0 * (1 - ratio * 0.3f));
                        int color = (alpha << 24) | 0x101020;
                        context.fill(0, y, this.width, y + 1, color);
                }

                // Decorative lines
                context.fill(0, headerHeight - 2, this.width, headerHeight - 1, 0x25FFFFFF);

                // Golden accent line centered
                int accentWidth = this.width / 2;
                int accentStart = (this.width - accentWidth) / 2;
                context.fill(accentStart, headerHeight - 1, accentStart + accentWidth, headerHeight,
                                PALETTE.accentGold());

                // Title with shadow effect
                String titleText = Text.translatable("murilloskills.info.title").getString();
                int titleX = (this.width - textRenderer.getWidth(titleText)) / 2;
                int titleY = 8;

                // Shadow
                context.drawText(textRenderer, titleText, titleX + 1, titleY + 1, 0x40000000, false);
                // Main text
                context.drawTextWithShadow(textRenderer,
                                Text.literal(titleText).formatted(Formatting.GOLD, Formatting.BOLD),
                                titleX, titleY, PALETTE.textGold());
        }

        private void renderContentPanel(DrawContext context) {
                // Outer shadow
                context.fill(contentX - 1, contentY - 1, contentX + contentWidth + 1, contentY + contentHeight + 1,
                                PALETTE.panelShadow());

                // Main panel background
                context.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight, PALETTE.panelBg());

                // Inner highlight (top edge)
                context.fill(contentX + 1, contentY + 1, contentX + contentWidth - 1, contentY + 2,
                                PALETTE.panelHighlight());

                // Border
                drawPanelBorder(context, contentX, contentY, contentWidth, contentHeight, PALETTE.sectionBorder());

                // Corner accents
                int cornerSize = 6;
                // Top-left
                context.fill(contentX, contentY, contentX + cornerSize, contentY + 1, PALETTE.accentGold());
                context.fill(contentX, contentY, contentX + 1, contentY + cornerSize, PALETTE.accentGold());
                // Top-right
                context.fill(contentX + contentWidth - cornerSize, contentY, contentX + contentWidth, contentY + 1,
                                PALETTE.accentGold());
                context.fill(contentX + contentWidth - 1, contentY, contentX + contentWidth, contentY + cornerSize,
                                PALETTE.accentGold());
                // Bottom-left
                context.fill(contentX, contentY + contentHeight - 1, contentX + cornerSize, contentY + contentHeight,
                                PALETTE.accentGold());
                context.fill(contentX, contentY + contentHeight - cornerSize, contentX + 1, contentY + contentHeight,
                                PALETTE.accentGold());
                // Bottom-right
                context.fill(contentX + contentWidth - cornerSize, contentY + contentHeight - 1,
                                contentX + contentWidth, contentY + contentHeight, PALETTE.accentGold());
                context.fill(contentX + contentWidth - 1, contentY + contentHeight - cornerSize,
                                contentX + contentWidth, contentY + contentHeight, PALETTE.accentGold());
        }

        private void drawPanelBorder(DrawContext context, int x, int y, int w, int h, int color) {
                context.fill(x, y, x + w, y + 1, color); // Top
                context.fill(x, y + h - 1, x + w, y + h, color); // Bottom
                context.fill(x, y, x + 1, y + h, color); // Left
                context.fill(x + w - 1, y, x + w, y + h, color); // Right
        }

        // === TAB CONTENT RENDERING ===

        private void renderStatusTab(DrawContext context) {
                int y = contentY + SECTION_PADDING - scrollController.getScrollOffset();
                int x = contentX + SECTION_PADDING;

                // Section title with decorative line
                renderSectionTitle(context, x, y, Text.translatable("murilloskills.info.status.title").getString());
                y += 28;

                // Selected Skills section
                renderSubsectionHeader(context, x, y,
                                Text.translatable("murilloskills.info.status.selected").getString());
                y += 18;

                List<MurilloSkillsList> selectedSkills = ClientSkillData.getSelectedSkills();
                if (selectedSkills.isEmpty()) {
                        context.drawText(textRenderer,
                                        Text.translatable("murilloskills.info.status.none_selected")
                                                        .formatted(Formatting.ITALIC),
                                        x + 12, y, PALETTE.textMuted(), false);
                        y += 20;
                } else {
                        for (MurilloSkillsList skill : selectedSkills) {
                                var stats = ClientSkillData.get(skill);
                                boolean isParagon = skill == ClientSkillData.getParagonSkill();

                                // Skill card
                                renderSkillMiniCard(context, x + 8, y, skill, stats.level, stats.prestige, isParagon);
                                y += 46;
                        }
                }
                y += 12;

                // Synergies section
                renderSubsectionHeader(context, x, y,
                                Text.translatable("murilloskills.info.status.synergies").getString());
                y += 18;

                boolean hasSynergy = false;
                for (SkillUiData.SynergyInfo synergy : SkillUiData.SYNERGIES) {
                        if (selectedSkills.contains(synergy.skill1()) && selectedSkills.contains(synergy.skill2())) {
                                renderActiveSynergyBadge(context, x + 8, y, synergy);
                                y += 22;
                                hasSynergy = true;
                        }
                }
                if (!hasSynergy) {
                        context.drawText(textRenderer,
                                        Text.translatable("murilloskills.synergy.none").formatted(Formatting.ITALIC),
                                        x + 12, y, PALETTE.textMuted(), false);
                }
        }

        private void renderSynergiesTab(DrawContext context) {
                int y = contentY + SECTION_PADDING - scrollController.getScrollOffset();
                int x = contentX + SECTION_PADDING;

                renderSectionTitle(context, x, y, Text.translatable("murilloskills.info.synergies.title").getString());
                y += 28;

                // Description in a nice box
                int descBoxWidth = contentWidth - SECTION_PADDING * 2 - 16;
                renderInfoBox(context, x, y, descBoxWidth, 24, PALETTE.accentBlue());
                context.drawText(textRenderer,
                                Text.translatable("murilloskills.info.synergies.desc").formatted(Formatting.ITALIC),
                                x + 8, y + 7, PALETTE.textLight(), false);
                y += 36;

                // All synergies as improved cards
                List<MurilloSkillsList> selectedSkills = ClientSkillData.getSelectedSkills();
                for (SkillUiData.SynergyInfo synergy : SkillUiData.SYNERGIES) {
                        boolean isActive = selectedSkills.contains(synergy.skill1())
                                        && selectedSkills.contains(synergy.skill2());
                        renderImprovedSynergyCard(context, x, y, synergy, isActive);
                        y += 78; // More spacing between cards
                }
        }

        private void renderPrestigeTab(DrawContext context) {
                int y = contentY + SECTION_PADDING - scrollController.getScrollOffset();
                int x = contentX + SECTION_PADDING;

                renderSectionTitle(context, x, y, Text.translatable("murilloskills.info.prestige.title").getString());
                y += 28;

                // Description in a styled box
                int descBoxWidth = contentWidth - SECTION_PADDING * 2 - 16;
                renderInfoBox(context, x, y, descBoxWidth, 58, PALETTE.textPurple());

                List<String> descLines = List.of(
                                Text.translatable("murilloskills.info.prestige.desc1").getString(),
                                Text.translatable("murilloskills.info.prestige.desc2").getString(),
                                Text.translatable("murilloskills.info.prestige.desc3").getString());
                int descY = y + 6;
                for (String line : descLines) {
                        List<String> wrapped = wrapText(line, descBoxWidth - 16);
                        for (String wrappedLine : wrapped) {
                                context.drawText(textRenderer, Text.literal(wrappedLine), x + 8, descY,
                                                PALETTE.textLight(),
                                                false);
                                descY += 12;
                        }
                }
                y += 68;

                // Bonus table
                renderSubsectionHeader(context, x, y,
                                Text.translatable("murilloskills.info.prestige.table_header").getString());
                y += 20;

                // Table header
                int col1 = x + 10;
                int col2 = x + 80;
                int col3 = x + 170;
                context.drawText(textRenderer, Text.translatable("murilloskills.info.prestige.level"), col1, y,
                                PALETTE.textAqua(), false);
                context.drawText(textRenderer, Text.translatable("murilloskills.info.prestige.xp_bonus"), col2, y,
                                PALETTE.textAqua(), false);
                context.drawText(textRenderer, Text.translatable("murilloskills.info.prestige.passive_bonus"), col3, y,
                                PALETTE.textAqua(), false);
                y += 14;

                // Divider
                renderDivider(context, x + 5, y, contentWidth - SECTION_PADDING * 2 - 20);
                y += 8;

                // Table rows
                int[] levels = { 1, 2, 3, 5, 10, 25, 50, 100 };
                for (int level : levels) {
                        int xpBonus = level * 5;
                        int passiveBonus = level * 2;

                        int rowColor = (level == 100) ? PALETTE.textGold() : PALETTE.textLight();
                        context.drawText(textRenderer, Text.literal("P" + level), col1, y, rowColor, false);
                        context.drawText(textRenderer, Text.literal("+" + xpBonus + "%"), col2, y, PALETTE.textGreen(),
                                        false);
                        context.drawText(textRenderer, Text.literal("+" + passiveBonus + "%"), col3, y,
                                        PALETTE.textAqua(),
                                        false);
                        y += 14;
                }
                y += 12;

                // Requirements
                renderSubsectionHeader(context, x, y,
                                Text.translatable("murilloskills.info.prestige.requirements").getString());
                y += 18;

                String[] reqs = { "req1", "req2", "req3" };
                for (String req : reqs) {
                        context.drawText(textRenderer,
                                        Text.literal("  > " + Text.translatable("murilloskills.info.prestige." + req)
                                                        .getString()),
                                        x + 8, y, PALETTE.textMuted(), false);
                        y += 14;
                }
        }

        private void renderPerksTab(DrawContext context) {
                int y = contentY + SECTION_PADDING - scrollController.getScrollOffset();
                int x = contentX + SECTION_PADDING;

                renderSectionTitle(context, x, y, Text.translatable("murilloskills.info.perks.title").getString());
                y += 28;

                // All skills with perks
                for (MurilloSkillsList skill : MurilloSkillsList.values()) {
                        // Skill header card
                        renderSkillPerkHeader(context, x, y, skill);
                        y += 26;

                        // Perks list
                        List<SkillUiData.PerkInfo> perks = SkillUiData.SKILL_PERKS.get(skill);
                        if (perks != null) {
                                var stats = ClientSkillData.get(skill);
                                int playerLevel = stats != null ? stats.level : 0;

                                for (SkillUiData.PerkInfo perk : perks) {
                                        boolean unlocked = playerLevel >= perk.level();
                                        renderPerkItem(context, x + 12, y, perk, unlocked);
                                        y += 20;
                                }
                        }
                        y += 12;
                }
        }

        // === STYLED COMPONENT HELPERS ===

        private void renderSectionTitle(DrawContext context, int x, int y, String title) {
                // Decorative line before
                renderDivider(context, x, y + 5, 20);

                // Title text
                context.drawTextWithShadow(textRenderer,
                                Text.literal(title).formatted(Formatting.GOLD, Formatting.BOLD),
                                x + 25, y, PALETTE.textGold());

                // Decorative line after
                int titleWidth = textRenderer.getWidth(title);
                renderDivider(context, x + 30 + titleWidth, y + 5,
                                contentWidth - SECTION_PADDING * 2 - titleWidth - 50);
        }

        private void renderSubsectionHeader(DrawContext context, int x, int y, String title) {
                context.drawText(textRenderer, Text.literal("> " + title).formatted(Formatting.YELLOW),
                                x, y, PALETTE.textYellow(), false);
        }

        private void renderDivider(DrawContext context, int x, int y, int width) {
                if (width > 0) {
                        context.fill(x, y, x + width, y + 1, PALETTE.dividerColor());
                }
        }

        private void renderInfoBox(DrawContext context, int x, int y, int width, int height, int accentColor) {
                // Background
                context.fill(x, y, x + width, y + height, 0xC0101018);
                // Border
                drawPanelBorder(context, x, y, width, height, 0x60FFFFFF);
                // Left accent
                context.fill(x, y + 2, x + 3, y + height - 2, accentColor);
        }

        private void renderSkillMiniCard(DrawContext context, int x, int y, MurilloSkillsList skill, int level,
                        int prestige, boolean isParagon) {
                int cardWidth = contentWidth - SECTION_PADDING * 2 - 24;
                int cardHeight = 50; // Taller to fit XP bar

                var stats = ClientSkillData.get(skill);

                // Card background with glow for paragon
                int bgColor = isParagon ? 0xE0201810 : 0xD0181825;
                if (isParagon) {
                        // Subtle paragon glow
                        context.fill(x - 1, y - 1, x + cardWidth + 1, y + cardHeight + 1, PALETTE.cardGlowParagon());
                }
                context.fill(x, y, x + cardWidth, y + cardHeight, bgColor);

                // Border with accent
                int borderColor = isParagon ? PALETTE.accentGold() : PALETTE.sectionBorder();
                drawPanelBorder(context, x, y, cardWidth, cardHeight, borderColor);

                // Corner accents for paragon
                if (isParagon) {
                        RenderingHelper.renderCornerAccents(context, x, y, cardWidth, cardHeight, 4,
                                        PALETTE.accentGold());
                }

                // Skill icon (item)
                context.drawItem(SkillUiData.getSkillIcon(skill), x + 4, y + 8);

                // Skill name with skill color
                String name = Text.translatable("murilloskills.skill.name." + skill.name().toLowerCase()).getString();
                int nameColor = isParagon ? PALETTE.textGold() : PALETTE.getSkillColor(skill);
                context.drawTextWithShadow(textRenderer, Text.literal(name).formatted(Formatting.BOLD),
                                x + 26, y + 5, nameColor);

                // Level badge + Prestige
                String levelStr = "Lv " + level;
                int levelColor = level >= 100 ? PALETTE.textGold() : PALETTE.textLight();
                context.drawText(textRenderer, Text.literal(levelStr), x + 26, y + 17, levelColor, false);

                if (prestige > 0) {
                        // Star rating for prestige
                        int starsX = x + 26 + textRenderer.getWidth(levelStr) + 5;
                        RenderingHelper.renderStarRating(context, textRenderer, starsX, y + 17,
                                        Math.min(prestige, 5), 5, PALETTE.textGold(), PALETTE.textMuted());
                }

                // XP Progress bar
                int barX = x + 26;
                int barY = y + 30;
                int barWidth = cardWidth - 75;
                int barHeight = 6;

                double xpNeeded = 60 + (level * 15) + (2 * level * level);
                float progress = level >= 100 ? 1.0f : (float) (stats.xp / xpNeeded);
                int fillColor = level >= 100 ? PALETTE.accentGold() : PALETTE.progressBarFill();

                RenderingHelper.renderProgressBar(context, barX, barY, barWidth, barHeight, progress,
                                PALETTE.progressBarEmpty(), fillColor, PALETTE.progressBarShine());

                // XP text or MAX
                if (level >= 100) {
                        context.drawText(textRenderer, Text.literal("MAX").formatted(Formatting.GOLD),
                                        barX + barWidth + 4, barY, PALETTE.textGold(), false);
                } else {
                        int percent = (int) (progress * 100);
                        context.drawText(textRenderer, Text.literal(percent + "%"),
                                        barX + barWidth + 4, barY, PALETTE.textMuted(), false);
                }

                // Paragon crown with glow
                if (isParagon) {
                        context.drawTextWithShadow(textRenderer, Text.literal("👑").formatted(Formatting.GOLD),
                                        x + cardWidth - 18, y + 5, PALETTE.accentGold());
                }

                // Prestige bonus summary (compact)
                if (prestige > 0) {
                        int xpBonus = prestige * 5;
                        String bonusStr = "+" + xpBonus + "% XP";
                        int bonusX = x + cardWidth - textRenderer.getWidth(bonusStr) - 8;
                        context.drawText(textRenderer, Text.literal(bonusStr), bonusX, y + 38, PALETTE.textAqua(),
                                        false);
                }
        }

        private void renderActiveSynergyBadge(DrawContext context, int x, int y, SkillUiData.SynergyInfo synergy) {
                String name = Text.translatable("murilloskills.synergy." + synergy.id()).getString();
                String bonus = "+" + synergy.bonus() + "% " + Text.translatable(synergy.typeKey()).getString();

                // Badge background
                int width = Math.max(textRenderer.getWidth(name), textRenderer.getWidth(bonus)) + 20;
                context.fill(x, y, x + width, y + 18, 0xC0103010);
                drawPanelBorder(context, x, y, width, 18, PALETTE.accentGreen());

                // Check mark and text
                context.drawText(textRenderer, Text.literal("[+] " + name), x + 4, y + 2, PALETTE.textGreen(), false);
                context.drawText(textRenderer, Text.literal("    " + bonus), x + 4, y + 10, PALETTE.textAqua(), false);
        }

        private void renderImprovedSynergyCard(DrawContext context, int x, int y, SkillUiData.SynergyInfo synergy,
                        boolean isActive) {
                int cardWidth = contentWidth - SECTION_PADDING * 2 - 16;
                int cardHeight = 68; // Taller card

                // Card background with gradient effect
                int bgColor = isActive ? 0xD8102820 : 0xD0181822;
                context.fill(x, y, x + cardWidth, y + cardHeight, bgColor);

                // Glow effect for active cards
                if (isActive) {
                        context.fill(x + 1, y + 1, x + cardWidth - 1, y + 3, 0x30AAFFAA);
                }

                // Border with accent color
                int borderColor = isActive ? PALETTE.accentGreen() : PALETTE.sectionBorder();
                drawPanelBorder(context, x, y, cardWidth, cardHeight, borderColor);

                // Left accent bar (thicker for active)
                int accentWidth = isActive ? 4 : 3;
                context.fill(x, y + 2, x + accentWidth, y + cardHeight - 2,
                                isActive ? PALETTE.accentGreen() : PALETTE.textMuted());

                // === CONTENT SECTION ===
                int iconAreaX = x + 10;
                int textAreaX = x + 58;

                // Render skill icons side by side with more spacing
                context.drawItem(SkillUiData.getSkillIcon(synergy.skill1()), iconAreaX, y + 8);
                context.drawItem(SkillUiData.getSkillIcon(synergy.skill2()), iconAreaX + 28, y + 8);

                // Plus sign between icons (centered)
                context.drawText(textRenderer, Text.literal("+").formatted(Formatting.WHITE),
                                iconAreaX + 19, y + 13, PALETTE.textWhite(), false);

                // Synergy name with status indicator
                String statusIcon = isActive ? "[+]" : "[ ]";
                String name = Text.translatable("murilloskills.synergy." + synergy.id()).getString();
                int nameColor = isActive ? PALETTE.textGreen() : PALETTE.textGray();
                context.drawTextWithShadow(textRenderer, Text.literal(statusIcon + " " + name),
                                textAreaX, y + 8, nameColor);

                // Skills required (smaller text below)
                String skill1Name = Text
                                .translatable("murilloskills.skill.name." + synergy.skill1().name().toLowerCase())
                                .getString();
                String skill2Name = Text
                                .translatable("murilloskills.skill.name." + synergy.skill2().name().toLowerCase())
                                .getString();
                String skillsText = skill1Name + " + " + skill2Name;
                context.drawText(textRenderer, Text.literal(skillsText).formatted(Formatting.GRAY),
                                textAreaX, y + 22, PALETTE.textMuted(), false);

                // === BONUS BAR SECTION ===
                int bonusBarY = y + 40;
                int bonusBarWidth = cardWidth - 24;
                int bonusBarHeight = 18;

                // Bonus bar background
                context.fill(x + 12, bonusBarY, x + 12 + bonusBarWidth, bonusBarY + bonusBarHeight, 0x80101018);

                // Bonus bar border
                int barBorderColor = isActive ? 0xFF225533 : 0xFF333344;
                context.fill(x + 12, bonusBarY, x + 12 + bonusBarWidth, bonusBarY + 1, barBorderColor);
                context.fill(x + 12, bonusBarY + bonusBarHeight - 1, x + 12 + bonusBarWidth, bonusBarY + bonusBarHeight,
                                barBorderColor);
                context.fill(x + 12, bonusBarY, x + 13, bonusBarY + bonusBarHeight, barBorderColor);
                context.fill(x + 11 + bonusBarWidth, bonusBarY, x + 12 + bonusBarWidth, bonusBarY + bonusBarHeight,
                                barBorderColor);

                // Bonus text centered in bar
                String bonusType = Text.translatable(synergy.typeKey()).getString();
                String bonusText = "+" + synergy.bonus() + "% " + bonusType;
                int textWidth = textRenderer.getWidth(bonusText);
                int textX = x + 12 + (bonusBarWidth - textWidth) / 2;
                int bonusColor = isActive ? PALETTE.textAqua() : PALETTE.textMuted();
                context.drawText(textRenderer, Text.literal(bonusText), textX, bonusBarY + 5, bonusColor, false);

                // Active indicator on the right
                if (isActive) {
                        String activeText = "ACTIVE";
                        int activeX = x + cardWidth - textRenderer.getWidth(activeText) - 12;
                        context.drawTextWithShadow(textRenderer,
                                        Text.literal(activeText).formatted(Formatting.GREEN, Formatting.BOLD),
                                        activeX, y + 8, PALETTE.textGreen());
                }
        }

        private void renderSkillPerkHeader(DrawContext context, int x, int y, MurilloSkillsList skill) {
                int width = contentWidth - SECTION_PADDING * 2 - 16;

                // Header background
                context.fill(x, y, x + width, y + 20, PALETTE.sectionBg());
                drawPanelBorder(context, x, y, width, 20, PALETTE.sectionBorder());

                // Skill name with color
                String skillName = Text.translatable("murilloskills.skill.name." + skill.name().toLowerCase())
                                .getString();
                int skillColor = getSkillColor(skill);
                context.drawTextWithShadow(textRenderer, Text.literal("  " + skillName).formatted(Formatting.BOLD),
                                x + 4, y + 6, skillColor);
        }

        private void renderPerkItem(DrawContext context, int x, int y, SkillUiData.PerkInfo perk, boolean unlocked) {
                String prefix = unlocked ? "[+]" : "[ ]";
                int prefixColor = unlocked ? PALETTE.textGreen() : PALETTE.textMuted();

                context.drawText(textRenderer, Text.literal(prefix), x, y, prefixColor, false);

                String levelStr = "Lv" + perk.level() + ": ";
                context.drawText(textRenderer, Text.literal(levelStr), x + 18, y,
                                unlocked ? PALETTE.textYellow() : PALETTE.textMuted(),
                                false);

                String perkName = Text.translatable(perk.nameKey()).getString();
                int nameWidth = textRenderer.getWidth(levelStr) + 18;
                int maxNameWidth = textMaxWidth - nameWidth - 20;

                if (textRenderer.getWidth(perkName) > maxNameWidth) {
                        while (textRenderer.getWidth(perkName + "...") > maxNameWidth && perkName.length() > 5) {
                                perkName = perkName.substring(0, perkName.length() - 1);
                        }
                        perkName += "...";
                }

                context.drawText(textRenderer, Text.literal(perkName), x + nameWidth, y,
                                unlocked ? PALETTE.textWhite() : PALETTE.textMuted(), false);
        }

        private int getSkillColor(MurilloSkillsList skill) {
                return switch (skill) {
                        case MINER -> 0xFF88CCFF;
                        case WARRIOR -> 0xFFFF6666;
                        case FARMER -> 0xFF88FF88;
                        case ARCHER -> 0xFFFFCC66;
                        case FISHER -> 0xFF66CCFF;
                        case BUILDER -> 0xFFCC9966;
                        case BLACKSMITH -> 0xFFCCCCCC;
                        case EXPLORER -> 0xFF66FF99;
                };
        }

        private List<String> wrapText(String text, int maxWidth) {
                List<String> lines = new ArrayList<>();
                if (textRenderer.getWidth(text) <= maxWidth) {
                        lines.add(text);
                        return lines;
                }

                StringBuilder currentLine = new StringBuilder();
                String[] words = text.split(" ");

                for (String word : words) {
                        String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                        if (textRenderer.getWidth(testLine) <= maxWidth) {
                                if (!currentLine.isEmpty())
                                        currentLine.append(" ");
                                currentLine.append(word);
                        } else {
                                if (!currentLine.isEmpty()) {
                                        lines.add(currentLine.toString());
                                        currentLine = new StringBuilder(word);
                                } else {
                                        lines.add(word);
                                }
                        }
                }
                if (!currentLine.isEmpty())
                        lines.add(currentLine.toString());
                return lines;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
                return scrollController.handleMouseScroll(mouseX, mouseY, verticalAmount,
                                contentX, contentY, contentWidth, contentHeight) ||
                                super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        // SynergyInfo and PerkInfo records removed (migrated to SkillUiData)
}
