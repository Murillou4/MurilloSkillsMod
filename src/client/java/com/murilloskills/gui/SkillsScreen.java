package com.murilloskills.gui;

import com.murilloskills.data.ClientSkillData;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.gui.ColorPalette;
import com.murilloskills.gui.renderer.RenderingHelper;
import com.murilloskills.gui.data.SkillUiData;
import com.murilloskills.network.ParagonActivationC2SPayload;
import com.murilloskills.network.SkillResetC2SPayload;
import com.murilloskills.network.SkillSelectionC2SPayload;
import com.murilloskills.network.PrestigeC2SPayload;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.render.XpToastRenderer;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkillsScreen extends Screen {

    // Static perk definitions definition removed (migrated to SkillUiData)

    /**
     * Gets the translatable skill name for i18n support
     */
    private static Text getTranslatableSkillName(MurilloSkillsList skill) {
        return Text.translatable("murilloskills.skill.name." + skill.name().toLowerCase());
    }

    // === PREMIUM COLOR PALETTE (consistent with ModInfoScreen) ===
    private static final ColorPalette PALETTE = ColorPalette.premium();

    // Custom XP Bar Colors (specific to skills screen)
    private static final int XP_BAR_BG = 0xFF0A0A12;
    private static final int XP_BAR_BORDER = 0xFF1A1A25;

    // Layout responsivo - calculado dinamicamente
    private int cardWidth;
    private int cardHeight;
    private int padding;
    private int columns;
    private int startX, startY;
    private int headerHeight;

    // Selection Mode State
    private final Set<MurilloSkillsList> pendingSelection = new HashSet<>();
    private ButtonWidget confirmButton;

    // Map to store selection buttons for each skill
    private final java.util.Map<MurilloSkillsList, ButtonWidget> selectionButtons = new java.util.HashMap<>();

    // XP Toast toggle button
    private ButtonWidget toastToggleButton;

    public SkillsScreen() {
        super(Text.translatable("murilloskills.gui.title"));
    }

    private boolean isSelectionMode() {
        return !ClientSkillData.hasSelectedSkills();
    }

    private void updateSelectionButtonStates() {
        for (var entry : selectionButtons.entrySet()) {
            MurilloSkillsList skill = entry.getKey();
            ButtonWidget btn = entry.getValue();
            boolean isSelected = pendingSelection.contains(skill);
            boolean isPermanent = ClientSkillData.isSkillSelected(skill);

            if (isPermanent) {
                btn.setMessage(Text.translatable("murilloskills.gui.btn_defined"));
                btn.active = false;
            } else {
                btn.setMessage(isSelected ? Text.translatable("murilloskills.gui.btn_selected")
                        : Text.translatable("murilloskills.gui.btn_select"));
                btn.active = true;
            }
        }

        if (confirmButton != null) {
            int count = pendingSelection.size();
            boolean isComplete = count == 3;
            // Active if we have at least 1 skill, and if we have partial selection (or
            // full)
            confirmButton.active = count > 0 && count <= 3;

            if (isComplete) {
                confirmButton.setMessage(Text.translatable("murilloskills.gui.btn_confirm", count));
            } else {
                confirmButton.setMessage(Text.translatable("murilloskills.gui.btn_save_partial", count));
            }
        }
    }

    /**
     * Calcula layout responsivo baseado no tamanho da janela e escala da GUI
     */
    private void calculateResponsiveLayout() {
        // Margem mínima das bordas
        int marginX = 20;
        int marginTop = 50;
        int marginBottom = 50;

        // Espaço disponível
        int availableWidth = this.width - (marginX * 2);
        int availableHeight = this.height - marginTop - marginBottom;

        // Número de skills (8)
        int skillCount = MurilloSkillsList.values().length;

        // Determinar número de colunas baseado na largura (2, 4 ou até 8 para telas
        // muito largas)
        if (availableWidth >= 700) {
            this.columns = 4;
        } else if (availableWidth >= 400) {
            this.columns = 2;
        } else {
            this.columns = 2; // Mínimo 2 colunas
        }

        int rows = (int) Math.ceil((double) skillCount / columns);

        // Calcular padding baseado no tamanho
        this.padding = Math.max(6, Math.min(12, availableWidth / 40));

        // Calcular tamanho dos cards para caber na tela
        int totalPaddingX = (columns - 1) * padding;
        int totalPaddingY = (rows - 1) * padding;

        // Card width: Usar espaço disponível dividido pelas colunas
        this.cardWidth = Math.min(180, Math.max(120, (availableWidth - totalPaddingX) / columns));

        // Card height: Proporcional ao width (ratio ~2.5:1) mas limitado
        this.cardHeight = Math.min(70, Math.max(50, cardWidth * 45 / 100));

        // Verificar se cabe verticalmente, se não, reduzir
        int neededHeight = (rows * cardHeight) + totalPaddingY;
        if (neededHeight > availableHeight) {
            this.cardHeight = Math.max(45, (availableHeight - totalPaddingY) / rows);
        }

        // Header dinâmico
        this.headerHeight = Math.max(35, this.height / 12);

        // Posições iniciais (centralizadas)
        int totalGridWidth = (columns * cardWidth) + totalPaddingX;
        this.startX = (this.width - totalGridWidth) / 2;
        this.startY = headerHeight + 10;
    }

    @Override
    protected void init() {
        super.init();

        // Populate pending selection with already selected skills (Server
        // authoritative)
        if (isSelectionMode()) {
            pendingSelection.addAll(ClientSkillData.getSelectedSkills());
        }

        // Calcular layout responsivo baseado no tamanho da janela
        calculateResponsiveLayout();

        // Limpar widgets antigos para não duplicar se a tela for redimensionada
        this.clearChildren();
        selectionButtons.clear();

        MurilloSkillsList[] skills = MurilloSkillsList.values();
        MurilloSkillsList paragon = ClientSkillData.getParagonSkill();
        boolean selectionMode = isSelectionMode();

        if (selectionMode) {
            // SELECTION MODE: Add selection buttons for each skill card
            for (int i = 0; i < skills.length; i++) {
                MurilloSkillsList skill = skills[i];

                int col = i % columns;
                int row = i / columns;
                int x = startX + (col * (cardWidth + padding));
                int y = startY + (row * (cardHeight + padding));

                // Botões proporcionais ao tamanho do card
                int btnX = x + 18;
                int btnY = y + cardHeight - 18;
                int btnWidth = cardWidth - 36;
                int btnHeight = 14;

                boolean isSelected = pendingSelection.contains(skill);
                boolean isPermanent = ClientSkillData.isSkillSelected(skill);

                ButtonWidget selectBtn = ButtonWidget.builder(
                        isPermanent ? Text.translatable("murilloskills.gui.btn_defined")
                                : (isSelected ? Text.translatable("murilloskills.gui.btn_selected")
                                        : Text.translatable("murilloskills.gui.btn_select")),
                        (button) -> {
                            // Cannot change permanently selected skills
                            if (isPermanent)
                                return;

                            // Toggle selection
                            if (pendingSelection.contains(skill)) {
                                pendingSelection.remove(skill);
                            } else if (pendingSelection.size() < 3) {
                                pendingSelection.add(skill);
                            }
                            updateSelectionButtonStates();
                        })
                        .dimensions(btnX, btnY, btnWidth, btnHeight)
                        .build();

                // Disable button if permanent choice
                if (isPermanent) {
                    selectBtn.active = false;
                }

                selectionButtons.put(skill, selectBtn);
                this.addDrawableChild(selectBtn);

                // If permanent, also add the Reset button to allow "unlocking" it
                if (isPermanent) {
                    int resetBtnSize = Math.max(12, cardHeight / 5);
                    int resetBtnX = x + cardWidth - resetBtnSize - 4;
                    int resetBtnY = y + cardHeight - resetBtnSize - 4;
                    int resetBtnWidth = resetBtnSize;
                    int resetBtnHeight = resetBtnSize;

                    ButtonWidget resetBtn = ButtonWidget
                            .builder(Text.translatable("murilloskills.gui.icon.reset"), (button) -> {
                                MinecraftClient.getInstance().setScreen(new ConfirmationScreen(
                                        this,
                                        Text.translatable("murilloskills.confirm.reset_title"),
                                        Text.translatable("murilloskills.confirm.reset_message",
                                                getTranslatableSkillName(skill)),
                                        () -> {
                                            pendingSelection.remove(skill);
                                            ClientPlayNetworking.send(new SkillResetC2SPayload(skill));
                                        }));
                            })
                            .dimensions(resetBtnX, resetBtnY, resetBtnWidth, resetBtnHeight)
                            .build();

                    this.addDrawableChild(resetBtn);
                }
            }

            // Botão de confirmação responsivo
            int confirmBtnWidth = Math.min(300, Math.max(180, this.width / 3));
            int confirmBtnHeight = 22;
            int confirmBtnX = (this.width - confirmBtnWidth) / 2;
            int confirmBtnY = this.height - 35;

            confirmButton = ButtonWidget
                    .builder(Text.translatable("murilloskills.gui.btn_save_partial", 0), (button) -> {
                        if (pendingSelection.size() > 0 && pendingSelection.size() <= 3) {
                            List<MurilloSkillsList> selected = new ArrayList<>(pendingSelection);
                            ClientPlayNetworking.send(new SkillSelectionC2SPayload(selected));
                            this.close();
                        }
                    })
                    .dimensions(confirmBtnX, confirmBtnY, confirmBtnWidth, confirmBtnHeight)
                    .build();
            // confirmButton.active is updated in updateSelectionButtonStates
            confirmButton.active = false;
            this.addDrawableChild(confirmButton);

            // Update button states based on current pending selection
            updateSelectionButtonStates();
        } else {
            // NORMAL MODE: Add Paragon buttons for eligible skills
            for (int i = 0; i < skills.length; i++) {
                MurilloSkillsList skill = skills[i];
                var stats = ClientSkillData.get(skill);

                // Paragon button only for selected skills at level 99+ with no paragon yet
                boolean isSelected = ClientSkillData.isSkillSelected(skill);
                if (paragon == null && isSelected && stats.level >= 99) {
                    int col = i % columns;
                    int row = i / columns;
                    int x = startX + (col * (cardWidth + padding));
                    int y = startY + (row * (cardHeight + padding));

                    // Botões proporcionais ao tamanho do card
                    int btnX = x + 18;
                    int btnY = y + cardHeight - 18;
                    int btnWidth = cardWidth - 36;
                    int btnHeight = 14;

                    ButtonWidget paragonBtn = ButtonWidget
                            .builder(Text.translatable("murilloskills.gui.btn_paragon"), (button) -> {
                                ClientPlayNetworking.send(new ParagonActivationC2SPayload(skill));
                                this.close();
                            })
                            .dimensions(btnX, btnY, btnWidth, btnHeight)
                            .build();

                    this.addDrawableChild(paragonBtn);
                }

                // Prestige button for Paragon skill at level 100+ with prestige < 10
                boolean isParagon = (skill == paragon);
                if (isParagon && stats.level >= 100 && stats.prestige < 10) {
                    int col = i % columns;
                    int row = i / columns;
                    int x = startX + (col * (cardWidth + padding));
                    int y = startY + (row * (cardHeight + padding));

                    // Botão de prestígio no card
                    int btnX = x + 18;
                    int btnY = y + cardHeight - 18;
                    int btnWidth = cardWidth - 36;
                    int btnHeight = 14;

                    int nextPrestige = stats.prestige + 1;
                    int nextXpBonus = nextPrestige * 5; // +5% per prestige
                    int nextPassiveBonus = nextPrestige * 2; // +2% per prestige

                    // Build rich multiline tooltip
                    Text prestigeTooltip = Text.empty()
                            .append(Text.literal("⭐ ").formatted(Formatting.GOLD))
                            .append(Text.translatable("murilloskills.gui.prestige_tooltip.title", nextPrestige)
                                    .formatted(Formatting.GOLD, Formatting.BOLD))
                            .append(Text.literal("\n"))
                            .append(Text.literal("\n"))
                            .append(Text.literal("📈 ").formatted(Formatting.GREEN))
                            .append(Text.translatable("murilloskills.gui.prestige_tooltip.xp_bonus", nextXpBonus)
                                    .formatted(Formatting.GREEN))
                            .append(Text.literal("\n"))
                            .append(Text.literal("💪 ").formatted(Formatting.AQUA))
                            .append(Text
                                    .translatable("murilloskills.gui.prestige_tooltip.passive_bonus", nextPassiveBonus)
                                    .formatted(Formatting.AQUA))
                            .append(Text.literal("\n"))
                            .append(Text.literal("\n"))
                            .append(Text.literal("✓ ").formatted(Formatting.YELLOW))
                            .append(Text.translatable("murilloskills.gui.prestige_tooltip.keeps_master")
                                    .formatted(Formatting.YELLOW))
                            .append(Text.literal("\n"))
                            .append(Text.literal("⚠ ").formatted(Formatting.RED))
                            .append(Text.translatable("murilloskills.gui.prestige_tooltip.resets_level")
                                    .formatted(Formatting.RED));

                    ButtonWidget prestigeBtn = ButtonWidget
                            .builder(Text.translatable("murilloskills.gui.prestige_button"), (button) -> {
                                MinecraftClient.getInstance().setScreen(new ConfirmationScreen(
                                        this,
                                        Text.translatable("murilloskills.gui.prestige_button"),
                                        Text.translatable("murilloskills.gui.prestige_confirm"),
                                        () -> {
                                            ClientPlayNetworking.send(new PrestigeC2SPayload(skill));
                                        }));
                            })
                            .dimensions(btnX, btnY, btnWidth, btnHeight)
                            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(prestigeTooltip))
                            .build();

                    this.addDrawableChild(prestigeBtn);
                }
                // Reset button for all selected skills (only on selected, non-paragon skills)
                if (isSelected) {
                    int col = i % columns;
                    int row = i / columns;
                    int x = startX + (col * (cardWidth + padding));
                    int y = startY + (row * (cardHeight + padding));

                    // Botão de reset no canto inferior direito (não sobrepor o nível)
                    int resetBtnSize = Math.max(12, cardHeight / 5);
                    int resetBtnX = x + cardWidth - resetBtnSize - 4;
                    int resetBtnY = y + cardHeight - resetBtnSize - 4;
                    int resetBtnWidth = resetBtnSize;
                    int resetBtnHeight = resetBtnSize;

                    ButtonWidget resetBtn = ButtonWidget
                            .builder(Text.translatable("murilloskills.gui.icon.reset"), (button) -> {
                                MinecraftClient.getInstance().setScreen(new ConfirmationScreen(
                                        this,
                                        Text.translatable("murilloskills.confirm.reset_title"),
                                        Text.translatable("murilloskills.confirm.reset_message",
                                                getTranslatableSkillName(skill)),
                                        () -> {
                                            ClientPlayNetworking.send(new SkillResetC2SPayload(skill));
                                        }));
                            })
                            .dimensions(resetBtnX, resetBtnY, resetBtnWidth, resetBtnHeight)
                            .build();

                    this.addDrawableChild(resetBtn);
                }
            }
        }

        // XP Toast toggle button (both modes) - positioned in top-right corner
        int toastBtnWidth = 130;
        int toastBtnHeight = 16;
        int toastBtnX = this.width - toastBtnWidth - 10;
        int toastBtnY = 8;

        toastToggleButton = ButtonWidget.builder(
                getToastToggleText(),
                (button) -> {
                    XpToastRenderer.toggle();
                    button.setMessage(getToastToggleText());
                })
                .dimensions(toastBtnX, toastBtnY, toastBtnWidth, toastBtnHeight)
                .build();
        this.addDrawableChild(toastToggleButton);

        // Info button - opens ModInfoScreen with all mod details
        int infoBtnWidth = 50;
        int infoBtnHeight = 16;
        int infoBtnX = toastBtnX - infoBtnWidth - 8;
        int infoBtnY = 8;

        ButtonWidget infoButton = ButtonWidget.builder(
                Text.translatable("murilloskills.gui.info_button"),
                (button) -> {
                    MinecraftClient.getInstance().setScreen(new ModInfoScreen(this));
                })
                .dimensions(infoBtnX, infoBtnY, infoBtnWidth, infoBtnHeight)
                .build();
        this.addDrawableChild(infoButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. Background gradient with vignette (same as ModInfoScreen)
        renderGradientBackground(context);

        // Subtle vignette corners (darkening towards edges)
        int vignetteSize = Math.min(this.width, this.height) / 3;
        drawVignetteCorner(context, 0, 0, vignetteSize, true, true);
        drawVignetteCorner(context, this.width - vignetteSize, 0, vignetteSize, false, true);
        drawVignetteCorner(context, 0, this.height - vignetteSize, vignetteSize, true, false);
        drawVignetteCorner(context, this.width - vignetteSize, this.height - vignetteSize, vignetteSize, false, false);

        boolean selectionMode = isSelectionMode();

        // 2. Modern Header with gradient
        renderHeader(context, selectionMode);

        MurilloSkillsList[] skills = MurilloSkillsList.values();
        List<Text> tooltipToRender = null;
        MurilloSkillsList paragon = ClientSkillData.getParagonSkill();
        long worldTime = MinecraftClient.getInstance().world != null ? MinecraftClient.getInstance().world.getTime()
                : 0;

        // 2. Renderizar os Cartões (Fundo, Ícones, Texto)
        for (int i = 0; i < skills.length; i++) {
            MurilloSkillsList skill = skills[i];
            var stats = ClientSkillData.get(skill);

            int col = i % columns;
            int row = i / columns;
            int x = startX + (col * (cardWidth + padding));
            int y = startY + (row * (cardHeight + padding));

            boolean isHovered = mouseX >= x && mouseX <= x + cardWidth && mouseY >= y && mouseY <= y + cardHeight;
            boolean isParagon = (skill == paragon);
            boolean isSelected = ClientSkillData.isSkillSelected(skill);
            boolean isPendingSelect = pendingSelection.contains(skill);
            boolean isLocked = !selectionMode && !isSelected && ClientSkillData.hasSelectedSkills();

            // Determine card background color (using PALETTE)
            int cardBg;
            int borderColor;

            if (selectionMode) {
                // Selection mode colors
                if (isPendingSelect) {
                    cardBg = isHovered ? PALETTE.sectionBgActive() : 0xE8181830;
                    borderColor = PALETTE.accentGreen();
                } else {
                    cardBg = isHovered ? PALETTE.sectionBgActive() : PALETTE.sectionBg();
                    borderColor = isHovered ? PALETTE.accentGold() : PALETTE.sectionBorder();
                }
            } else {
                // Normal mode colors
                if (isParagon) {
                    cardBg = 0xF0201810; // Special paragon tint
                    borderColor = isHovered ? PALETTE.accentGold() : 0xFFFFD700;
                } else if (isLocked) {
                    cardBg = 0xD0101015; // Darker for locked
                    borderColor = 0xFF1A1A20;
                } else if (isSelected) {
                    cardBg = isHovered ? PALETTE.sectionBgActive() : 0xE8102018;
                    borderColor = isHovered ? PALETTE.accentGold() : PALETTE.accentGreen();
                } else {
                    cardBg = isHovered ? PALETTE.sectionBgActive() : PALETTE.sectionBg();
                    borderColor = isHovered ? PALETTE.accentGold() : PALETTE.sectionBorder();
                }
            }

            // Modern Card with depth effects
            renderModernCard(context, x, y, cardWidth, cardHeight, cardBg, borderColor, isHovered);

            // Skill Icon with subtle glow for active skills
            if (!isLocked && (isSelected || isParagon)) {
                // Subtle item glow
                context.fill(x + 3, y + 12, x + 23, y + 32, PALETTE.panelHighlight());
            }
            context.drawItem(SkillUiData.getSkillIcon(skill), x + 5, y + 14);

            // Skill Name with better typography
            int titleColor = isLocked ? PALETTE.textMuted() : PALETTE.textGold();
            context.drawTextWithShadow(this.textRenderer, getTranslatableSkillName(skill), x + 28, y + 6, titleColor);

            // Lock icon for non-selected skills (normal mode only)
            if (!selectionMode && isLocked) {
                context.drawTextWithShadow(this.textRenderer, Text.translatable("murilloskills.gui.icon.lock"),
                        x + cardWidth - 16, y + 6, PALETTE.textMuted());
            }

            // Level badge with better positioning
            String lvlStr = String.valueOf(stats.level);
            Text fullLevelText = Text.translatable("murilloskills.gui.level_prefix").append(lvlStr);
            int lvlWidth = this.textRenderer.getWidth(fullLevelText);
            int lvlColor = isLocked ? PALETTE.textMuted()
                    : (stats.level >= 100 ? PALETTE.textGold() : PALETTE.textLight());
            context.drawTextWithShadow(this.textRenderer, fullLevelText, x + cardWidth - lvlWidth - 6, y + 6,
                    lvlColor);

            // Prestige indicator (show symbol if prestige > 0)
            if (stats.prestige > 0 && !isLocked) {
                String prestigeSymbol = getPrestigeSymbol(stats.prestige);
                int prestigeColor = getPrestigeColor(stats.prestige);
                // Draw prestige symbol before the level
                int symbolWidth = this.textRenderer.getWidth(prestigeSymbol);
                context.drawTextWithShadow(this.textRenderer, Text.literal(prestigeSymbol),
                        x + cardWidth - lvlWidth - symbolWidth - 10, y + 6, prestigeColor);
            }

            // Barra de XP
            renderXpBar(context, x + 28, y + 25, stats, isLocked);

            // Status text below XP bar (buttons handle selection mode text)
            if (isLocked) {
                context.drawText(this.textRenderer, Text.translatable("murilloskills.gui.locked"), x + 28, y + 40,
                        0xFFAA0000, false);
            } else if (isParagon) {
                long cooldownTicks = getSkillCooldown(skill);
                long timeSinceUse = worldTime - stats.lastAbilityUse;

                // Se lastAbilityUse == -1, significa que nunca foi usada (pronto para usar)
                // Ou se já passou o cooldown, também está pronto
                if (stats.lastAbilityUse < 0 || timeSinceUse >= cooldownTicks) {
                    context.drawText(this.textRenderer, Text.translatable("murilloskills.gui.ready"), x + 28, y + 40,
                            0xFF00FF00, false);
                } else {
                    long secondsLeft = (cooldownTicks - timeSinceUse) / 20;
                    String cdText = formatTime(secondsLeft);
                    context.drawText(this.textRenderer, Text.translatable("murilloskills.gui.cooldown", cdText), x + 28,
                            y + 40, 0xFFFF5555, false);
                }
                context.drawTextWithShadow(this.textRenderer, Text.translatable("murilloskills.gui.icon.paragon"),
                        x + 120, y - 4, 0xFFFFAA00);
            } else if (isSelected) {
                context.drawText(this.textRenderer, Text.translatable("murilloskills.gui.active"), x + 28, y + 40,
                        0xFF00AA00, false);
            }

            // Render perk roadmap dots (shows progress toward perks)
            if (!selectionMode) {
                renderPerkRoadmap(context, x + 28, y + 55, skill, stats.level, isLocked);
            }

            // Tooltip Logic
            if (isHovered) {
                boolean hoveringButton = this.children().stream()
                        .filter(element -> element instanceof ButtonWidget)
                        .anyMatch(btn -> ((ButtonWidget) btn).isMouseOver(mouseX, mouseY));

                if (!hoveringButton) {
                    long handle = MinecraftClient.getInstance().getWindow().getHandle();
                    boolean isExpanded = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                            GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
                    tooltipToRender = getSkillTooltip(skill, stats.level, isLocked, isParagon, selectionMode,
                            isPendingSelect, isExpanded);
                }
            }
        }

        // 3. Renderizar Widgets Nativos (Os botões adicionados no init)
        super.render(context, mouseX, mouseY, delta);

        // 4. Render Daily Challenges Panel (right side)
        if (!selectionMode) {
            renderDailyChallengesPanel(context, mouseX, mouseY);
        }

        // 5. Renderizar Tooltip por último (topo de tudo)
        if (tooltipToRender != null) {
            context.drawTooltip(this.textRenderer, tooltipToRender, mouseX, mouseY);
        }
    }

    private String formatTime(long seconds) {
        if (seconds > 60)
            return (seconds / 60) + Text.translatable("murilloskills.gui.time.minutes").getString();
        return seconds + Text.translatable("murilloskills.gui.time.seconds").getString();
    }

    /**
     * Gets the current text for the toast toggle button based on enabled state.
     */
    private Text getToastToggleText() {
        if (XpToastRenderer.isEnabled()) {
            return Text.translatable("murilloskills.gui.xp_toasts_on");
        } else {
            return Text.translatable("murilloskills.gui.xp_toasts_off");
        }
    }

    private long getSkillCooldown(MurilloSkillsList skill) {
        return switch (skill) {
            case MINER -> SkillConfig.toTicksLong(SkillConfig.MINER_ABILITY_COOLDOWN_SECONDS);
            case WARRIOR -> SkillConfig.toTicksLong(SkillConfig.WARRIOR_ABILITY_COOLDOWN_SECONDS);
            case ARCHER -> SkillConfig.toTicksLong(SkillConfig.ARCHER_ABILITY_COOLDOWN_SECONDS);
            case FARMER -> SkillConfig.toTicksLong(SkillConfig.FARMER_ABILITY_COOLDOWN_SECONDS);
            case FISHER -> SkillConfig.toTicksLong(SkillConfig.FISHER_ABILITY_COOLDOWN_SECONDS);
            case BLACKSMITH -> SkillConfig.toTicksLong(SkillConfig.BLACKSMITH_ABILITY_COOLDOWN_SECONDS);
            case BUILDER -> SkillConfig.toTicksLong(SkillConfig.BUILDER_ABILITY_COOLDOWN_SECONDS);
            default -> 6000L;
        };
    }

    private Text getSpecialAbilityDescription(MurilloSkillsList skill) {
        return Text.translatable("murilloskills.ability.desc." + skill.name().toLowerCase());
    }

    private List<Text> getSkillTooltip(MurilloSkillsList skill, int level, boolean isLocked, boolean isParagon,
            boolean selectionMode, boolean isPendingSelect, boolean isExpanded) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(getTranslatableSkillName(skill).copy().formatted(Formatting.GOLD, Formatting.BOLD));

        // Check if this specific skill is already active (server-confirmed selection)
        boolean isThisSkillActive = ClientSkillData.isSkillSelected(skill);

        // Show selection tooltip only if in selection mode AND this skill is NOT
        // already active
        if (selectionMode && !isThisSkillActive) {
            // Selection mode tooltip for unselected skills
            if (isPendingSelect) {
                tooltip.add(Text.translatable("murilloskills.gui.btn_selected").formatted(Formatting.GREEN));
            }
            tooltip.add(Text.empty());
            tooltip.add(isPendingSelect
                    ? Text.translatable("murilloskills.gui.click_to_deselect").formatted(Formatting.YELLOW)
                    : Text.translatable("murilloskills.gui.click_to_select").formatted(Formatting.YELLOW));
            tooltip.add(Text.empty());
            tooltip.add(Text.translatable("murilloskills.gui.description").formatted(Formatting.GRAY));
            tooltip.add(getSkillDescription(skill).copy().formatted(Formatting.WHITE));
        } else {
            // Normal mode tooltip
            if (isParagon)
                tooltip.add(Text.translatable("murilloskills.gui.paragon_active").formatted(Formatting.YELLOW));

            // Show prestige level and bonuses if prestige > 0
            int prestige = ClientSkillData.get(skill).prestige;
            if (prestige > 0) {
                int xpBonus = prestige * 5; // 5% per prestige
                int passiveBonus = prestige * 2; // 2% per prestige
                String prestigeSymbol = getPrestigeSymbol(prestige);
                tooltip.add(Text.empty());
                tooltip.add(Text.literal("⭐ ").formatted(Formatting.GOLD)
                        .append(Text.translatable("murilloskills.gui.prestige.level", prestige)
                                .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD))
                        .append(Text.literal(" " + prestigeSymbol).formatted(Formatting.YELLOW)));
                tooltip.add(Text.literal("  📈 ").formatted(Formatting.GREEN)
                        .append(Text.translatable("murilloskills.gui.prestige.xp_active", xpBonus)
                                .formatted(Formatting.GREEN)));
                tooltip.add(Text.literal("  💪 ").formatted(Formatting.AQUA)
                        .append(Text.translatable("murilloskills.gui.prestige.passive_active", passiveBonus)
                                .formatted(Formatting.AQUA)));
            }

            if (isLocked) {
                tooltip.add(Text.translatable("murilloskills.gui.skill_locked").formatted(Formatting.RED));
                tooltip.add(Text.translatable("murilloskills.gui.skill_not_selected").formatted(Formatting.DARK_GRAY));
                tooltip.add(Text.translatable("murilloskills.gui.cannot_gain_xp").formatted(Formatting.DARK_GRAY));
                return tooltip;
            }

            // XP Progress Logic (always shown)
            double currentXp = ClientSkillData.get(skill).xp;
            double maxXp = 60 + (level * 15) + (2 * level * level);
            int percent = (int) ((currentXp / maxXp) * 100);

            tooltip.add(Text.empty());
            tooltip.add(Text.translatable("murilloskills.gui.progress_xp", level + 1, percent)
                    .formatted(Formatting.GRAY));

            // === NEXT PERK SECTION (always shown) ===
            SkillUiData.PerkInfo nextPerk = SkillUiData.getNextPerk(skill, level);
            if (nextPerk != null) {
                int levelsRemaining = nextPerk.level() - level;
                tooltip.add(Text.literal("→ ").append(Text.translatable(nextPerk.nameKey()))
                        .append(Text.literal(" (" + levelsRemaining + " lvls)")).formatted(Formatting.YELLOW));
            } else {
                tooltip.add(Text.translatable("murilloskills.gui.all_perks_unlocked").formatted(Formatting.GOLD));
            }

            // === EXPANDED SECTION (only with Shift) ===
            if (isExpanded) {
                tooltip.add(Text.empty());
                tooltip.add(Text.translatable("murilloskills.gui.special_ability").formatted(Formatting.GRAY));
                tooltip.add(getSpecialAbilityDescription(skill).copy().formatted(Formatting.BLUE));

                tooltip.add(Text.empty());
                tooltip.add(Text.translatable("murilloskills.gui.xp_format",
                        String.format("%,.0f", currentXp), String.format("%,.0f", maxXp))
                        .formatted(Formatting.DARK_GRAY));

                tooltip.add(Text.empty());
                tooltip.add(getXpGainDescription(skill).copy().formatted(Formatting.GRAY));

                // Next Perk details
                if (nextPerk != null) {
                    tooltip.add(Text.empty());
                    tooltip.add(Text.translatable("murilloskills.gui.next_perk").formatted(Formatting.LIGHT_PURPLE));
                    tooltip.add(
                            Text.literal("🔓 ").append(Text.translatable(nextPerk.nameKey()))
                                    .formatted(Formatting.YELLOW));
                    tooltip.add(
                            Text.literal("   ").append(Text.translatable(nextPerk.descKey()))
                                    .formatted(Formatting.GRAY));
                    tooltip.add(Text
                            .translatable("murilloskills.gui.perk_remaining", nextPerk.level(),
                                    nextPerk.level() - level)
                            .formatted(Formatting.AQUA));
                }

                tooltip.add(Text.empty());
                tooltip.add(Text.translatable("murilloskills.gui.passives").formatted(Formatting.GRAY));
            } else {
                // Compact mode hint
                tooltip.add(Text.empty());
                tooltip.add(Text.translatable("murilloskills.gui.shift_for_details").formatted(Formatting.DARK_GRAY,
                        Formatting.ITALIC));
            }

            // Passives section (only in expanded mode)
            if (isExpanded) {
                // Get prestige multiplier for this skill (prestige variable already declared
                // above at line 817)
                float prestigeMultiplier = 1.0f + (prestige * 0.02f); // +2% per prestige level
                boolean hasPrestigeBonus = prestige > 0;
                String prestigeIndicator = hasPrestigeBonus ? " (+P)" : "";

                switch (skill) {
                    case MINER -> {
                        int baseSpeed = (int) (level * SkillConfig.MINER_SPEED_PER_LEVEL * 100);
                        int speed = (int) (baseSpeed * prestigeMultiplier);
                        tooltip.add(Text.translatable("murilloskills.passive.miner.mining_speed", speed)
                                .append(Text.literal(prestigeIndicator).formatted(Formatting.LIGHT_PURPLE))
                                .formatted(Formatting.GREEN));

                        int baseFortune = (int) (level * SkillConfig.MINER_FORTUNE_PER_LEVEL);
                        int fortune = (int) (baseFortune * prestigeMultiplier);
                        if (fortune > 0)
                            tooltip.add(Text.translatable("murilloskills.passive.miner.extra_fortune", fortune)
                                    .append(Text.literal(prestigeIndicator).formatted(Formatting.LIGHT_PURPLE))
                                    .formatted(Formatting.GREEN));

                        if (level >= SkillConfig.MINER_NIGHT_VISION_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.miner.night_vision")
                                    .formatted(Formatting.AQUA));
                        if (level >= SkillConfig.MINER_DURABILITY_LEVEL)
                            tooltip.add(
                                    Text.translatable("murilloskills.passive.miner.durability")
                                            .formatted(Formatting.AQUA));
                        if (level >= SkillConfig.MINER_RADAR_LEVEL)
                            tooltip.add(
                                    Text.translatable("murilloskills.passive.miner.ore_radar")
                                            .formatted(Formatting.AQUA));
                    }
                    case WARRIOR -> {
                        double baseDamage = level * SkillConfig.WARRIOR_DAMAGE_PER_LEVEL;
                        double damage = baseDamage * prestigeMultiplier;
                        tooltip.add(Text
                                .translatable("murilloskills.passive.warrior.base_damage",
                                        String.format("%.1f", damage))
                                .append(Text.literal(prestigeIndicator).formatted(Formatting.LIGHT_PURPLE))
                                .formatted(Formatting.RED));

                        int extraHearts = 0;
                        if (level >= 10)
                            extraHearts++;
                        if (level >= 50)
                            extraHearts++;
                        if (level >= 100)
                            extraHearts += 3;
                        if (extraHearts > 0)
                            tooltip.add(Text.translatable("murilloskills.passive.warrior.max_health", extraHearts)
                                    .formatted(Formatting.RED));

                        if (level >= SkillConfig.RESISTANCE_UNLOCK_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.warrior.iron_skin")
                                    .formatted(Formatting.GOLD));
                        if (level >= SkillConfig.LIFESTEAL_UNLOCK_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.warrior.vampirism")
                                    .formatted(Formatting.DARK_PURPLE));
                    }
                    case FARMER -> {
                        int baseDoubleChance = (int) (level * SkillConfig.FARMER_DOUBLE_HARVEST_PER_LEVEL * 100);
                        int doubleChance = (int) (baseDoubleChance * prestigeMultiplier);
                        tooltip.add(Text.translatable("murilloskills.passive.farmer.double_harvest", doubleChance)
                                .append(Text.literal(prestigeIndicator).formatted(Formatting.LIGHT_PURPLE))
                                .formatted(Formatting.GREEN));

                        int baseGoldenChance = (int) (level * SkillConfig.FARMER_GOLDEN_CROP_PER_LEVEL * 100);
                        int goldenChance = (int) (baseGoldenChance * prestigeMultiplier);
                        if (goldenChance > 0)
                            tooltip.add(Text.translatable("murilloskills.passive.farmer.golden_crop", goldenChance)
                                    .append(Text.literal(prestigeIndicator).formatted(Formatting.LIGHT_PURPLE))
                                    .formatted(Formatting.GOLD));

                        if (level >= SkillConfig.FARMER_GREEN_THUMB_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.farmer.green_thumb")
                                    .formatted(Formatting.GREEN));
                        if (level >= SkillConfig.FARMER_FERTILE_GROUND_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.farmer.fertile_ground")
                                    .formatted(Formatting.AQUA));
                        if (level >= SkillConfig.FARMER_NUTRIENT_CYCLE_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.farmer.nutrient_cycle")
                                    .formatted(Formatting.AQUA));
                        if (level >= SkillConfig.FARMER_ABUNDANT_HARVEST_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.farmer.abundant_harvest")
                                    .formatted(Formatting.GOLD));

                        // Key binding info
                        if (level >= SkillConfig.FARMER_AREA_PLANTING_LEVEL) {
                            tooltip.add(Text.empty());
                            tooltip.add(Text.translatable("murilloskills.keybind.hint.area_planting")
                                    .formatted(Formatting.LIGHT_PURPLE));
                        }
                    }
                    case ARCHER -> {
                        // Dano base por flecha (+2% por level) com bônus de prestígio
                        int baseArrowDamage = (int) (level * SkillConfig.ARCHER_DAMAGE_PER_LEVEL * 100);
                        int arrowDamage = (int) (baseArrowDamage * prestigeMultiplier);
                        tooltip.add(Text.translatable("murilloskills.passive.archer.arrow_damage", arrowDamage)
                                .append(Text.literal(prestigeIndicator).formatted(Formatting.LIGHT_PURPLE))
                                .formatted(Formatting.GREEN));

                        // Nível 10: Flechas mais rápidas
                        if (level >= SkillConfig.ARCHER_FAST_ARROWS_LEVEL) {
                            int speedBonus = (int) ((SkillConfig.ARCHER_ARROW_SPEED_MULTIPLIER - 1) * 100);
                            tooltip.add(Text.translatable("murilloskills.passive.archer.arrow_speed", speedBonus)
                                    .formatted(Formatting.AQUA));
                        }

                        // Nível 25: +5% dano adicional
                        if (level >= SkillConfig.ARCHER_BONUS_DAMAGE_LEVEL) {
                            int bonusDamage = (int) (SkillConfig.ARCHER_BONUS_DAMAGE_AMOUNT * 100);
                            tooltip.add(Text.translatable("murilloskills.passive.archer.bonus_damage", bonusDamage)
                                    .formatted(Formatting.AQUA));
                        }

                        // Nível 50: Penetração de flechas
                        if (level >= SkillConfig.ARCHER_PENETRATION_LEVEL) {
                            tooltip.add(Text.translatable("murilloskills.passive.archer.penetration")
                                    .formatted(Formatting.AQUA));
                        }

                        // Nível 75: Tiros mais estáveis
                        if (level >= SkillConfig.ARCHER_STABLE_SHOT_LEVEL) {
                            int spreadReduction = (int) (SkillConfig.ARCHER_SPREAD_REDUCTION * 100);
                            tooltip.add(Text.translatable("murilloskills.passive.archer.precision", spreadReduction)
                                    .formatted(Formatting.AQUA));
                        }

                        // Nível 100: Master Ranger
                        if (level >= SkillConfig.ARCHER_MASTER_LEVEL) {
                            tooltip.add(
                                    Text.translatable("murilloskills.passive.archer.master")
                                            .formatted(Formatting.GOLD));
                        }
                    }
                    case FISHER -> {
                        int baseFishingSpeed = (int) (level * SkillConfig.FISHER_SPEED_PER_LEVEL * 100);
                        int fishingSpeed = (int) (baseFishingSpeed * prestigeMultiplier);
                        tooltip.add(Text.translatable("murilloskills.passive.fisher.fishing_speed", fishingSpeed)
                                .append(Text.literal(prestigeIndicator).formatted(Formatting.LIGHT_PURPLE))
                                .formatted(Formatting.AQUA));
                        if (level >= SkillConfig.FISHER_WAIT_REDUCTION_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.fisher.wait_reduction")
                                    .formatted(Formatting.GREEN));
                        if (level >= SkillConfig.FISHER_TREASURE_BONUS_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.fisher.treasure_chance")
                                    .formatted(Formatting.GREEN));
                        if (level >= SkillConfig.FISHER_DOLPHIN_GRACE_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.fisher.dolphins_grace")
                                    .formatted(Formatting.AQUA));
                        if (level >= SkillConfig.FISHER_LUCK_SEA_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.fisher.luck_of_sea")
                                    .formatted(Formatting.AQUA));
                    }
                    case BLACKSMITH -> {
                        int baseResistance = (int) (level * SkillConfig.BLACKSMITH_RESISTANCE_PER_LEVEL * 100);
                        int resistance = (int) (baseResistance * prestigeMultiplier);
                        tooltip.add(
                                Text.translatable("murilloskills.passive.blacksmith.physical_resistance", resistance)
                                        .append(Text.literal(prestigeIndicator).formatted(Formatting.LIGHT_PURPLE))
                                        .formatted(Formatting.GOLD));
                        if (level >= SkillConfig.BLACKSMITH_IRON_SKIN_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.blacksmith.iron_skin")
                                    .formatted(Formatting.GREEN));
                        if (level >= SkillConfig.BLACKSMITH_EFFICIENT_ANVIL_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.blacksmith.efficient_anvil")
                                    .formatted(Formatting.GREEN));
                        if (level >= SkillConfig.BLACKSMITH_FORGED_RESILIENCE_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.blacksmith.forged_resilience")
                                    .formatted(Formatting.AQUA));
                        if (level >= SkillConfig.BLACKSMITH_THORNS_MASTER_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.blacksmith.thorns_master")
                                    .formatted(Formatting.AQUA));
                    }
                    case BUILDER -> {
                        double baseReach = level * SkillConfig.BUILDER_REACH_PER_LEVEL;
                        double reach = baseReach * prestigeMultiplier;
                        tooltip.add(
                                Text.translatable("murilloskills.passive.builder.extra_reach",
                                        String.format("%.1f", reach))
                                        .append(Text.literal(prestigeIndicator).formatted(Formatting.LIGHT_PURPLE))
                                        .formatted(Formatting.AQUA));
                        if (level >= SkillConfig.BUILDER_EXTENDED_REACH_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.builder.extended_reach")
                                    .formatted(Formatting.GREEN));
                        if (level >= SkillConfig.BUILDER_EFFICIENT_CRAFTING_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.builder.efficient_crafting")
                                    .formatted(Formatting.GREEN));
                        if (level >= SkillConfig.BUILDER_SAFE_LANDING_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.builder.safe_landing")
                                    .formatted(Formatting.AQUA));
                        if (level >= SkillConfig.BUILDER_SCAFFOLD_MASTER_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.builder.scaffold_master")
                                    .formatted(Formatting.AQUA));
                        if (level >= SkillConfig.BUILDER_MASTER_REACH_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.builder.master_reach")
                                    .formatted(Formatting.GOLD));

                        // Key binding info for Creative Brush mode
                        if (level >= SkillConfig.BUILDER_MASTER_LEVEL) {
                            tooltip.add(Text.empty());
                            tooltip.add(Text.translatable("murilloskills.keybind.hint.hollow_mode")
                                    .formatted(Formatting.LIGHT_PURPLE));
                        }
                    }
                    case EXPLORER -> {
                        // Velocidade base com bônus de prestígio
                        int baseSpeedBonus = (int) (level * SkillConfig.EXPLORER_SPEED_PER_LEVEL * 100);
                        int speedBonusExplorer = (int) (baseSpeedBonus * prestigeMultiplier);
                        tooltip.add(Text.translatable("murilloskills.passive.explorer.speed", speedBonusExplorer)
                                .append(Text.literal(prestigeIndicator).formatted(Formatting.LIGHT_PURPLE))
                                .formatted(Formatting.GREEN));

                        int luck = level / SkillConfig.EXPLORER_LUCK_INTERVAL;
                        if (luck > 0)
                            tooltip.add(Text.translatable("murilloskills.passive.explorer.luck", luck)
                                    .formatted(Formatting.GOLD));

                        if (level >= SkillConfig.EXPLORER_STEP_ASSIST_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.explorer.step_assist")
                                    .formatted(Formatting.GREEN));
                        if (level >= SkillConfig.EXPLORER_AQUATIC_LEVEL)
                            tooltip.add(
                                    Text.translatable("murilloskills.passive.explorer.aquatic")
                                            .formatted(Formatting.AQUA));
                        if (level >= SkillConfig.EXPLORER_NIGHT_VISION_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.explorer.night_vision")
                                    .formatted(Formatting.AQUA));
                        if (level >= SkillConfig.EXPLORER_FEATHER_FEET_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.explorer.feather_feet")
                                    .formatted(Formatting.AQUA));
                        if (level >= SkillConfig.EXPLORER_NETHER_WALKER_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.explorer.nether_walker")
                                    .formatted(Formatting.GOLD));
                        if (level >= SkillConfig.EXPLORER_MASTER_LEVEL)
                            tooltip.add(Text.translatable("murilloskills.passive.explorer.sixth_sense")
                                    .formatted(Formatting.GOLD));

                        // Key binding info
                        if (level >= SkillConfig.EXPLORER_NIGHT_VISION_LEVEL) {
                            tooltip.add(Text.empty());
                            if (level >= SkillConfig.EXPLORER_MASTER_LEVEL) {
                                tooltip.add(Text.translatable("murilloskills.keybind.hint.sixth_sense")
                                        .formatted(Formatting.LIGHT_PURPLE));
                            } else {
                                tooltip.add(Text.translatable("murilloskills.keybind.hint.night_vision")
                                        .formatted(Formatting.LIGHT_PURPLE));
                            }
                        }
                    }
                } // End of switch
            } // End of if (isExpanded)
        } // End of else (normal mode)

        return tooltip;
    }

    private Text getSkillDescription(MurilloSkillsList skill) {
        return Text.translatable("murilloskills.skill.desc." + skill.name().toLowerCase());
    }

    private Text getXpGainDescription(MurilloSkillsList skill) {
        return Text.translatable("murilloskills.tooltip.xp_gain." + skill.name().toLowerCase());
    }

    private void renderXpBar(DrawContext context, int x, int y, SkillGlobalState.SkillStats stats, boolean isLocked) {
        int width = 100;
        int height = 6;

        // Background with subtle border
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, XP_BAR_BORDER);
        context.fill(x, y, x + width, y + height, XP_BAR_BG);

        // Simple progress calculation based on current level progress
        double xpNeeded = 60 + (stats.level * 15) + (2 * stats.level * stats.level);
        float progress = (float) MathHelper.clamp(stats.xp / xpNeeded, 0, 1);
        int filledWidth = (int) (width * progress);

        // Elegant single-color fill with subtle gradient effect
        if (filledWidth > 0 && stats.level < 100) {
            int fillColor = isLocked ? 0xFF663333 : 0xFF22AA66; // Elegant teal/green
            int glowColor = isLocked ? 0x20FF4444 : 0x3022DDAA;

            // Subtle outer glow
            context.fill(x - 1, y - 1, x + filledWidth + 1, y + height + 1, glowColor);
            // Main fill
            context.fill(x, y, x + filledWidth, y + height, fillColor);
            // Top highlight for gradient effect
            context.fill(x, y, x + filledWidth, y + 1, 0x30FFFFFF);
            // Bottom shadow for depth
            context.fill(x, y + height - 1, x + filledWidth, y + height, 0x20000000);
        }

        // Max level - elegant gold fill
        if (stats.level >= 100 && !isLocked) {
            int goldColor = 0xFFDDA520;
            context.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0x30FFD700);
            context.fill(x, y, x + width, y + height, goldColor);
            context.fill(x, y, x + width, y + 1, 0x40FFFFFF);
        }
    }

    /**
     * Renders a mini perk roadmap showing upcoming perks as small colored dots
     */
    private void renderPerkRoadmap(DrawContext context, int x, int y, MurilloSkillsList skill, int currentLevel,
            boolean isLocked) {
        if (isLocked)
            return;

        List<SkillUiData.PerkInfo> perks = SkillUiData.SKILL_PERKS.get(skill);
        if (perks == null || perks.isEmpty())
            return;

        int dotSize = 4;
        int spacing = 6;
        int dotY = y;
        int dotX = x;

        // Show up to 4 perk indicators
        int shown = 0;
        for (SkillUiData.PerkInfo perk : perks) {
            if (shown >= 4)
                break;

            // Determine dot color based on unlock status
            int dotColor;
            if (currentLevel >= perk.level()) {
                // Unlocked - green
                dotColor = 0xFF44AA44;
            } else if (currentLevel >= perk.level() - 15) {
                // Close to unlock - yellow
                dotColor = 0xFFAAAA44;
            } else {
                // Locked - gray
                dotColor = 0xFF555555;
            }

            // Draw dot with subtle border
            context.fill(dotX, dotY, dotX + dotSize, dotY + dotSize, 0x80000000);
            context.fill(dotX + 1, dotY, dotX + dotSize, dotY + dotSize - 1, dotColor);

            dotX += spacing;
            shown++;
        }
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    /**
     * Renders the Daily Challenges panel on the right side of the screen.
     */
    private void renderDailyChallengesPanel(DrawContext context, int mouseX, int mouseY) {
        var challenges = ClientSkillData.getDailyChallenges();
        if (challenges.isEmpty())
            return;

        int panelWidth = 200;
        int panelHeight = 30 + (challenges.size() * 35);
        int panelX = 8; // Left side
        int panelY = this.height - panelHeight - 8; // Bottom aligned

        // Panel background
        context.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF1A1A25);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE8141420);

        // Header
        int completed = ClientSkillData.getCompletedChallengeCount();
        Text header = Text.translatable("murilloskills.gui.challenges.title", completed, challenges.size());
        context.drawTextWithShadow(textRenderer, header, panelX + 6, panelY + 6, PALETTE.accentGold());

        // Render each challenge
        int y = panelY + 22;
        for (var challenge : challenges) {
            // Challenge type text
            Text typeText = Text.translatable("murilloskills.challenge." + challenge.type().toLowerCase());
            int typeColor = challenge.completed() ? 0xFF44AA44 : 0xFFCCCCCC;
            context.drawTextWithShadow(textRenderer, typeText, panelX + 6, y, typeColor);

            // Progress bar
            int barX = panelX + 6;
            int barY = y + 11;
            int barWidth = panelWidth - 12;
            int barHeight = 4;

            context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF0A0A12);

            if (challenge.progress() > 0) {
                int fillWidth = (int) (barWidth * challenge.getProgressPercentage());
                int fillColor = challenge.completed() ? 0xFF44AA44 : 0xFF00AA44;
                context.fill(barX, barY, barX + Math.min(fillWidth, barWidth), barY + barHeight, fillColor);
            }

            // Progress text
            String progressStr = challenge.progress() + "/" + challenge.target();
            int progressWidth = textRenderer.getWidth(progressStr);
            context.drawText(textRenderer, progressStr, panelX + panelWidth - progressWidth - 6, y, 0xFFAAAAAA, false);

            // Checkmark if complete
            if (challenge.completed()) {
                context.drawText(textRenderer, "✓", panelX + panelWidth - 10, y + 10, 0xFF44AA44, false);
            }

            y += 35;
        }

        // Bonus indicator if all complete
        if (ClientSkillData.areAllChallengesComplete()) {
            Text bonus = Text.translatable("murilloskills.gui.challenges.all_complete");
            context.drawTextWithShadow(textRenderer, bonus, panelX + 6, y, 0xFFFFAA00);
        }
    }

    /**
     * Gets prestige symbol for display.
     */
    private String getPrestigeSymbol(int prestige) {
        if (prestige <= 0)
            return "";
        return switch (prestige) {
            case 1 -> "★";
            case 2 -> "★★";
            case 3 -> "★★★";
            case 4 -> "✦";
            case 5 -> "✦✦";
            case 6 -> "✦✦✦";
            case 7 -> "◆";
            case 8 -> "◆◆";
            case 9 -> "◆◆◆";
            case 10 -> "👑";
            default -> "P" + prestige;
        };
    }

    /**
     * Gets prestige color for display based on prestige level.
     */
    private int getPrestigeColor(int prestige) {
        return switch (prestige) {
            case 1, 2 -> 0xFF88FF88; // Light green
            case 3, 4 -> 0xFF88FFFF; // Cyan
            case 5, 6 -> 0xFFFFFF88; // Yellow
            case 7, 8 -> 0xFFFF88FF; // Magenta
            case 9, 10 -> 0xFFFFDD00; // Gold
            default -> 0xFFFFFFFF; // White
        };
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // === MODERN VISUAL EFFECT HELPERS ===

    /**
     * Renders the modern gradient header with title
     */
    private void renderHeader(DrawContext context, boolean selectionMode) {
        // Gradient header background
        for (int i = 0; i < headerHeight; i++) {
            float ratio = (float) i / headerHeight;
            int alpha = (int) (0xF0 * (1 - ratio * 0.3f));
            int color = (alpha << 24) | 0x101018;
            context.fill(0, i, this.width, i + 1, color);
        }

        // Decorative accent line at bottom
        context.fill(0, headerHeight - 2, this.width, headerHeight - 1, 0x30FFFFFF);
        context.fill(this.width / 4, headerHeight - 1, this.width * 3 / 4, headerHeight, PALETTE.accentGold());

        // Title
        int titleY = (headerHeight - 20) / 2;
        if (selectionMode) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("murilloskills.gui.choose_skills").formatted(Formatting.GOLD, Formatting.BOLD),
                    this.width / 2, titleY, PALETTE.textGold());
            int count = pendingSelection.size();
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("murilloskills.gui.select_skills_count", count).formatted(Formatting.YELLOW),
                    this.width / 2, titleY + 12, PALETTE.textGray());
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("murilloskills.gui.title").formatted(Formatting.GOLD, Formatting.BOLD),
                    this.width / 2, titleY + 5, PALETTE.textGold());
        }
    }

    /**
     * Draws a subtle vignette corner for depth effect
     */
    private void drawVignetteCorner(DrawContext context, int x, int y, int size, boolean left, boolean top) {
        // Simplified vignette - just darken corners slightly
        int steps = 8;
        for (int i = 0; i < steps; i++) {
            float ratio = (float) i / steps;
            int alpha = (int) (0x15 * (1 - ratio));
            if (alpha <= 0)
                continue;

            int color = (alpha << 24);
            int offset = (int) (size * ratio);

            int x1 = left ? x : x + offset;
            int x2 = left ? x + size - offset : x + size;
            int y1 = top ? y : y + offset;
            int y2 = top ? y + size - offset : y + size;

            if (x1 < x2 && y1 < y2) {
                context.fill(x1, y1, x2, y2, color);
            }
        }
    }

    /**
     * Renders a modern card with depth effects
     */
    private void renderModernCard(DrawContext context, int x, int y, int width, int height,
            int bgColor, int borderColor, boolean isHovered) {
        // Outer shadow (only visible on hover)
        if (isHovered) {
            context.fill(x - 1, y - 1, x + width + 1, y + height + 1, PALETTE.panelShadow());
        }

        // Main card background
        context.fill(x, y, x + width, y + height, bgColor);

        // Inner highlight (top edge)
        context.fill(x + 1, y + 1, x + width - 1, y + 2, PALETTE.panelHighlight());

        // Border
        drawBorder(context, x, y, width, height, borderColor);

        // Corner accents for hover state (same as ModInfoScreen)
        if (isHovered) {
            int accentSize = 4;
            // Top-left
            context.fill(x, y, x + accentSize, y + 1, borderColor);
            context.fill(x, y, x + 1, y + accentSize, borderColor);
            // Top-right
            context.fill(x + width - accentSize, y, x + width, y + 1, borderColor);
            context.fill(x + width - 1, y, x + width, y + accentSize, borderColor);
            // Bottom-left
            context.fill(x, y + height - 1, x + accentSize, y + height, borderColor);
            context.fill(x, y + height - accentSize, x + 1, y + height, borderColor);
            // Bottom-right
            context.fill(x + width - accentSize, y + height - 1, x + width, y + height, borderColor);
            context.fill(x + width - 1, y + height - accentSize, x + width, y + height, borderColor);
        }
    }

    /**
     * Renders gradient background identical to ModInfoScreen
     */
    private void renderGradientBackground(DrawContext context) {
        // Vertical gradient (same as ModInfoScreen)
        for (int y = 0; y < this.height; y++) {
            float ratio = (float) y / this.height;
            int r = (int) (8 + ratio * 4);
            int g = (int) (8 + ratio * 4);
            int b = (int) (16 + ratio * 8);
            int color = 0xF0000000 | (r << 16) | (g << 8) | b;
            context.fill(0, y, this.width, y + 1, color);
        }
    }
}