package com.murilloskills.gui;

import com.murilloskills.data.ClientSkillData;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.network.ParagonActivationC2SPayload;
import com.murilloskills.network.SkillResetC2SPayload;
import com.murilloskills.network.SkillSelectionC2SPayload;
import com.murilloskills.skills.MurilloSkillsList;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkillsScreen extends Screen {

    // Perk info record for next perk tooltip
    private record PerkInfo(int level, String name, String description) {
    }

    // Static perk definitions per skill (ordered by level)
    private static final java.util.Map<MurilloSkillsList, List<PerkInfo>> SKILL_PERKS = new java.util.HashMap<>();
    static {
        // MINER
        SKILL_PERKS.put(MurilloSkillsList.MINER, List.of(
                new PerkInfo(10, "Visão Noturna", "Visão noturna em cavernas"),
                new PerkInfo(30, "Durabilidade", "Ferramentas duram 15% mais"),
                new PerkInfo(60, "Radar de Minérios", "Som ao se aproximar de minérios"),
                new PerkInfo(100, "Master Miner", "Pulso revela minérios próximos")));

        // WARRIOR
        SKILL_PERKS.put(MurilloSkillsList.WARRIOR, List.of(
                new PerkInfo(10, "+1 Coração", "+1 coração de vida máxima"),
                new PerkInfo(25, "Pele de Ferro", "+15% resistência a dano"),
                new PerkInfo(50, "+1 Coração", "+1 coração de vida máxima"),
                new PerkInfo(75, "Vampirismo", "15% roubo de vida"),
                new PerkInfo(100, "Master Warrior", "+3 corações de vida máxima")));

        // FARMER
        SKILL_PERKS.put(MurilloSkillsList.FARMER, List.of(
                new PerkInfo(10, "Green Thumb", "+5% colheita, 10% semente salva"),
                new PerkInfo(25, "Fertile Ground", "Plantações crescem 25% mais rápido"),
                new PerkInfo(50, "Nutrient Cycle", "2x Bone Meal, 5% sementes extras"),
                new PerkInfo(75, "Abundant Harvest", "+15% colheita, colheita adjacente"),
                new PerkInfo(100, "Harvest Moon", "Colhe todos os cultivos ao redor")));

        // ARCHER
        SKILL_PERKS.put(MurilloSkillsList.ARCHER, List.of(
                new PerkInfo(10, "Flechas Rápidas", "Flechas voam 25% mais rápido"),
                new PerkInfo(25, "Dano Bônus", "+5% dano à distância"),
                new PerkInfo(50, "Penetração", "Flechas atravessam inimigos"),
                new PerkInfo(75, "Tiro Estável", "50% menos dispersão"),
                new PerkInfo(100, "Master Ranger", "Flechas perseguem inimigos")));

        // FISHER
        SKILL_PERKS.put(MurilloSkillsList.FISHER, List.of(
                new PerkInfo(10, "Pesca Rápida", "-25% tempo de espera"),
                new PerkInfo(25, "Caçador de Tesouros", "+10% tesouro, +10% XP"),
                new PerkInfo(50, "Dolphin's Grace", "Velocidade na água"),
                new PerkInfo(75, "Luck of the Sea", "Sorte passiva nível 1"),
                new PerkInfo(100, "Rain Dance", "Invoca chuva com bônus massivos")));

        // BLACKSMITH
        SKILL_PERKS.put(MurilloSkillsList.BLACKSMITH, List.of(
                new PerkInfo(10, "Iron Skin", "+5% resistência física"),
                new PerkInfo(25, "Efficient Anvil", "25% desconto XP, 10% material salvo"),
                new PerkInfo(50, "Forged Resilience", "+10% fogo/explosão, +1 Protection"),
                new PerkInfo(75, "Thorns Master", "20% reflexo, -50% knockback"),
                new PerkInfo(100, "Titanium Aura", "Imunidade/regeneração/resistência")));

        // BUILDER
        SKILL_PERKS.put(MurilloSkillsList.BUILDER, List.of(
                new PerkInfo(10, "Extended Reach", "+1 bloco de alcance"),
                new PerkInfo(15, "Efficient Crafting", "20% economia decorativos"),
                new PerkInfo(25, "Safe Landing", "-25% dano de queda"),
                new PerkInfo(50, "Scaffold Master", "Velocidade scaffolding"),
                new PerkInfo(75, "Master Reach", "+5 blocos de alcance"),
                new PerkInfo(100, "Creative Brush", "Preenche áreas automaticamente")));

        // EXPLORER
        SKILL_PERKS.put(MurilloSkillsList.EXPLORER, List.of(
                new PerkInfo(10, "Passo Leve", "Sobe blocos automaticamente"),
                new PerkInfo(20, "Aquático", "+50% respiração submerso"),
                new PerkInfo(35, "Olhos de Gato", "Visão noturna permanente"),
                new PerkInfo(65, "Pés de Pena", "-40% dano de queda"),
                new PerkInfo(80, "Nether Walker", "Imune a magma, soul sand normal"),
                new PerkInfo(100, "Sexto Sentido", "Baús e Spawners brilham")));
    }

    /**
     * Gets the next perk for a skill based on current level
     * 
     * @return PerkInfo of next perk, or null if all perks unlocked
     */
    private static PerkInfo getNextPerk(MurilloSkillsList skill, int currentLevel) {
        List<PerkInfo> perks = SKILL_PERKS.get(skill);
        if (perks == null)
            return null;

        for (PerkInfo perk : perks) {
            if (perk.level() > currentLevel) {
                return perk;
            }
        }
        return null; // All perks unlocked
    }

    /**
     * Gets the translatable skill name for i18n support
     */
    private static Text getTranslatableSkillName(MurilloSkillsList skill) {
        return Text.translatable("murilloskills.skill.name." + skill.name().toLowerCase());
    }

    // Cores Premium
    private static final int BG_OVERLAY = 0xE0101018;
    private static final int CARD_BG_NORMAL = 0xFF1A1A24;
    private static final int CARD_BG_HOVER = 0xFF2A2A38;
    private static final int CARD_BG_PARAGON = 0xFF2A2818;
    private static final int CARD_BG_SELECTED = 0xFF1A2A20; // Green tint for selected skills
    private static final int CARD_BG_LOCKED = 0xFF141418; // Darker for locked skills
    private static final int CARD_BG_PENDING_SELECT = 0xFF202038; // Blue tint for pending selection
    private static final int BORDER_NORMAL = 0xFF404050;
    private static final int BORDER_HOVER = 0xFFFFCC44;
    private static final int BORDER_SELECTED = 0xFF44FF66; // Green border for selected
    private static final int BORDER_LOCKED = 0xFF282830; // Dark border for locked
    private static final int BORDER_PARAGON = 0xFFFFAA00; // Gold border for paragon
    private static final int XP_BAR_BG = 0xFF0A0A10;
    private static final int HEADER_BG = 0xDD101018;

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

    public SkillsScreen() {
        super(Text.of("§6§lHabilidades"));
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

                    ButtonWidget resetBtn = ButtonWidget.builder(Text.literal("⟳"), (button) -> {
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

                    ButtonWidget resetBtn = ButtonWidget.builder(Text.literal("⟳"), (button) -> {
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
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. Fundo Escuro da Tela
        context.fill(0, 0, this.width, this.height, BG_OVERLAY);

        boolean selectionMode = isSelectionMode();

        // 1.5. Header Background Panel
        context.fill(0, 0, this.width, headerHeight, HEADER_BG);
        // Subtle gradient line at bottom of header
        context.fill(0, headerHeight - 1, this.width, headerHeight, 0x40FFFFFF);

        // Title changes based on mode
        int titleY = (headerHeight - 20) / 2;
        if (selectionMode) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("murilloskills.gui.choose_skills").formatted(Formatting.GOLD, Formatting.BOLD),
                    this.width / 2, titleY, 0xFFFFFFFF);
            int count = pendingSelection.size();
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("murilloskills.gui.select_skills_count", count).formatted(Formatting.YELLOW),
                    this.width / 2,
                    titleY + 12, 0xFFFFFFFF);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("murilloskills.gui.title").formatted(Formatting.GOLD, Formatting.BOLD),
                    this.width / 2, titleY + 5, 0xFFFFFFFF);
        }

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

            // Determine card background color
            int cardBg;
            int borderColor;

            if (selectionMode) {
                // Selection mode colors
                if (isPendingSelect) {
                    cardBg = isHovered ? CARD_BG_HOVER : CARD_BG_PENDING_SELECT;
                    borderColor = BORDER_SELECTED;
                } else {
                    cardBg = isHovered ? CARD_BG_HOVER : CARD_BG_NORMAL;
                    borderColor = isHovered ? BORDER_HOVER : BORDER_NORMAL;
                }
            } else {
                // Normal mode colors
                if (isParagon) {
                    cardBg = CARD_BG_PARAGON;
                    borderColor = isHovered ? BORDER_HOVER : BORDER_PARAGON;
                } else if (isLocked) {
                    cardBg = CARD_BG_LOCKED;
                    borderColor = BORDER_LOCKED;
                } else if (isSelected) {
                    cardBg = isHovered ? CARD_BG_HOVER : CARD_BG_SELECTED;
                    borderColor = isHovered ? BORDER_HOVER : BORDER_SELECTED;
                } else {
                    cardBg = isHovered ? CARD_BG_HOVER : CARD_BG_NORMAL;
                    borderColor = isHovered ? BORDER_HOVER : BORDER_NORMAL;
                }
            }

            // Fundo do Card
            context.fill(x, y, x + cardWidth, y + cardHeight, cardBg);
            drawBorder(context, x, y, cardWidth, cardHeight, borderColor);

            // Ícone e Título
            int titleColor = isLocked ? 0xFF666666 : 0xFFFFAA00;
            context.drawItem(getSkillIcon(skill), x + 5, y + 14);
            context.drawTextWithShadow(this.textRenderer, getTranslatableSkillName(skill), x + 28, y + 5, titleColor);

            // Lock icon for non-selected skills (normal mode only)
            if (!selectionMode && isLocked) {
                context.drawTextWithShadow(this.textRenderer, "🔒", x + cardWidth - 15, y + 5, 0xFF666666);
            }

            // Nível
            String lvlStr = String.valueOf(stats.level);
            int lvlWidth = this.textRenderer.getWidth("Lvl " + lvlStr);
            int lvlColor = isLocked ? 0xFF666666 : 0xFFFFFFFF;
            context.drawTextWithShadow(this.textRenderer, "Lvl " + lvlStr, x + cardWidth - lvlWidth - 5, y + 5,
                    lvlColor);

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
                context.drawTextWithShadow(this.textRenderer, "👑", x + 120, y - 4, 0xFFFFAA00);
            } else if (isSelected) {
                context.drawText(this.textRenderer, Text.translatable("murilloskills.gui.active"), x + 28, y + 40,
                        0xFF00AA00, false);
            }

            // Tooltip Logic
            if (isHovered) {
                boolean hoveringButton = this.children().stream()
                        .filter(element -> element instanceof ButtonWidget)
                        .anyMatch(btn -> ((ButtonWidget) btn).isMouseOver(mouseX, mouseY));

                if (!hoveringButton) {
                    tooltipToRender = getSkillTooltip(skill, stats.level, isLocked, isParagon, selectionMode,
                            isPendingSelect);
                }
            }
        }

        // 3. Renderizar Widgets Nativos (Os botões adicionados no init)
        super.render(context, mouseX, mouseY, delta);

        // 4. Renderizar Tooltip por último (topo de tudo)
        if (tooltipToRender != null) {
            context.drawTooltip(this.textRenderer, tooltipToRender, mouseX, mouseY);
        }
    }

    private String formatTime(long seconds) {
        if (seconds > 60)
            return (seconds / 60) + "m";
        return seconds + "s";
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
            boolean selectionMode, boolean isPendingSelect) {
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
            if (isLocked) {
                tooltip.add(Text.translatable("murilloskills.gui.skill_locked").formatted(Formatting.RED));
                tooltip.add(Text.translatable("murilloskills.gui.skill_not_selected").formatted(Formatting.DARK_GRAY));
                tooltip.add(Text.translatable("murilloskills.gui.cannot_gain_xp").formatted(Formatting.DARK_GRAY));
                return tooltip;
            }

            tooltip.add(Text.empty());
            tooltip.add(Text.translatable("murilloskills.gui.special_ability").formatted(Formatting.GRAY));
            tooltip.add(getSpecialAbilityDescription(skill).copy().formatted(Formatting.BLUE));

            // XP Progress Logic
            double currentXp = ClientSkillData.get(skill).xp;
            double maxXp = 60 + (level * 15) + (2 * level * level);
            int percent = (int) ((currentXp / maxXp) * 100);

            tooltip.add(Text.empty());
            tooltip.add(Text.literal("Progresso para o Nível " + (level + 1) + ": " + percent + "%")
                    .formatted(Formatting.GRAY));
            tooltip.add(
                    Text.literal(String.format("%,.0f / %,.0f XP", currentXp, maxXp)).formatted(Formatting.DARK_GRAY));

            tooltip.add(Text.empty());
            tooltip.add(getXpGainDescription(skill).copy().formatted(Formatting.GRAY));

            // === NEXT PERK SECTION ===
            PerkInfo nextPerk = getNextPerk(skill, level);
            tooltip.add(Text.empty());
            if (nextPerk != null) {
                int levelsRemaining = nextPerk.level() - level;
                tooltip.add(Text.translatable("murilloskills.gui.next_perk").formatted(Formatting.LIGHT_PURPLE));
                tooltip.add(Text.literal("🔓 " + nextPerk.name()).formatted(Formatting.YELLOW));
                tooltip.add(Text.literal("   " + nextPerk.description()).formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("murilloskills.gui.perk_remaining", nextPerk.level(), levelsRemaining)
                        .formatted(Formatting.AQUA));
            } else {
                tooltip.add(
                        Text.translatable("murilloskills.gui.all_perks_unlocked").formatted(Formatting.GOLD,
                                Formatting.BOLD));
            }

            tooltip.add(Text.empty());
            tooltip.add(Text.translatable("murilloskills.gui.passives").formatted(Formatting.GRAY));

            switch (skill) {
                case MINER -> {
                    int speed = (int) (level * SkillConfig.MINER_SPEED_PER_LEVEL * 100);
                    tooltip.add(Text.literal("• Mineração: +" + speed + "% Vel.").formatted(Formatting.GREEN));

                    int fortune = (int) (level * SkillConfig.MINER_FORTUNE_PER_LEVEL);
                    if (fortune > 0)
                        tooltip.add(Text.literal("• Fortuna Extra: +" + fortune).formatted(Formatting.GREEN));

                    if (level >= SkillConfig.MINER_NIGHT_VISION_LEVEL)
                        tooltip.add(Text.literal("• Visão Noturna (Cavernas)").formatted(Formatting.AQUA));
                    if (level >= SkillConfig.MINER_DURABILITY_LEVEL)
                        tooltip.add(Text.literal("• Durabilidade Infinita (Chance)").formatted(Formatting.AQUA));
                    if (level >= SkillConfig.MINER_RADAR_LEVEL)
                        tooltip.add(Text.literal("• Radar de Minérios").formatted(Formatting.AQUA));
                }
                case WARRIOR -> {
                    double damage = level * SkillConfig.WARRIOR_DAMAGE_PER_LEVEL;
                    tooltip.add(
                            Text.literal("• Dano Base: +" + String.format("%.1f", damage)).formatted(Formatting.RED));

                    int extraHearts = 0;
                    if (level >= 10)
                        extraHearts++;
                    if (level >= 50)
                        extraHearts++;
                    if (level >= 100)
                        extraHearts += 3;
                    if (extraHearts > 0)
                        tooltip.add(Text.literal("• Vida Max: +" + extraHearts + " ❤").formatted(Formatting.RED));

                    if (level >= SkillConfig.RESISTANCE_UNLOCK_LEVEL)
                        tooltip.add(Text.literal("• Pele de Ferro (Resistência)").formatted(Formatting.GOLD));
                    if (level >= SkillConfig.LIFESTEAL_UNLOCK_LEVEL)
                        tooltip.add(Text.literal("• Vampirismo (Roubo de Vida)").formatted(Formatting.DARK_PURPLE));
                }
                case FARMER -> {
                    // Dano base
                    int doubleChance = (int) (level * SkillConfig.FARMER_DOUBLE_HARVEST_PER_LEVEL * 100);
                    tooltip.add(
                            Text.literal("• Colheita Dupla: " + doubleChance + "% chance").formatted(Formatting.GREEN));

                    int goldenChance = (int) (level * SkillConfig.FARMER_GOLDEN_CROP_PER_LEVEL * 100);
                    if (goldenChance > 0)
                        tooltip.add(Text.literal("• Cultivo Dourado: " + goldenChance + "% chance")
                                .formatted(Formatting.GOLD));

                    if (level >= SkillConfig.FARMER_GREEN_THUMB_LEVEL)
                        tooltip.add(Text.literal("• Green Thumb (+5% colheita)").formatted(Formatting.GREEN));
                    if (level >= SkillConfig.FARMER_FERTILE_GROUND_LEVEL)
                        tooltip.add(Text.literal("• Fertile Ground (25% crescimento)").formatted(Formatting.AQUA));
                    if (level >= SkillConfig.FARMER_NUTRIENT_CYCLE_LEVEL)
                        tooltip.add(Text.literal("• Nutrient Cycle (2x Bone Meal)").formatted(Formatting.AQUA));
                    if (level >= SkillConfig.FARMER_ABUNDANT_HARVEST_LEVEL)
                        tooltip.add(Text.literal("• Abundant Harvest (+15% colheita)").formatted(Formatting.GOLD));

                    // Key binding info
                    if (level >= SkillConfig.FARMER_AREA_PLANTING_LEVEL) {
                        tooltip.add(Text.empty());
                        tooltip.add(Text.literal("⌨ Tecla G: Toggle Plantio em Área 3x3")
                                .formatted(Formatting.LIGHT_PURPLE));
                    }
                }
                case ARCHER -> {
                    // Dano base por flecha (+2% por level)
                    int arrowDamage = (int) (level * SkillConfig.ARCHER_DAMAGE_PER_LEVEL * 100);
                    tooltip.add(Text.literal("• Dano de Flecha: +" + arrowDamage + "%").formatted(Formatting.GREEN));

                    // Nível 10: Flechas mais rápidas
                    if (level >= SkillConfig.ARCHER_FAST_ARROWS_LEVEL) {
                        int speedBonus = (int) ((SkillConfig.ARCHER_ARROW_SPEED_MULTIPLIER - 1) * 100);
                        tooltip.add(Text.literal("• Velocidade de Flecha: +" + speedBonus + "%")
                                .formatted(Formatting.AQUA));
                    }

                    // Nível 25: +5% dano adicional
                    if (level >= SkillConfig.ARCHER_BONUS_DAMAGE_LEVEL) {
                        int bonusDamage = (int) (SkillConfig.ARCHER_BONUS_DAMAGE_AMOUNT * 100);
                        tooltip.add(Text.literal("• Bônus de Dano: +" + bonusDamage + "%").formatted(Formatting.AQUA));
                    }

                    // Nível 50: Penetração de flechas
                    if (level >= SkillConfig.ARCHER_PENETRATION_LEVEL) {
                        tooltip.add(Text.literal("• Penetração (Piercing)").formatted(Formatting.AQUA));
                    }

                    // Nível 75: Tiros mais estáveis
                    if (level >= SkillConfig.ARCHER_STABLE_SHOT_LEVEL) {
                        int spreadReduction = (int) (SkillConfig.ARCHER_SPREAD_REDUCTION * 100);
                        tooltip.add(Text.literal("• Precisão: +" + spreadReduction + "%").formatted(Formatting.AQUA));
                    }

                    // Nível 100: Master Ranger
                    if (level >= SkillConfig.ARCHER_MASTER_LEVEL) {
                        tooltip.add(Text.literal("• Master Ranger (Habilidade Ativa)").formatted(Formatting.GOLD));
                    }
                }
                case FISHER -> {
                    tooltip.add(Text
                            .literal("• Velocidade de Pesca: +"
                                    + (int) (level * SkillConfig.FISHER_SPEED_PER_LEVEL * 100) + "%")
                            .formatted(Formatting.AQUA));
                    if (level >= SkillConfig.FISHER_WAIT_REDUCTION_LEVEL)
                        tooltip.add(Text.literal("• -25% tempo de espera").formatted(Formatting.GREEN));
                    if (level >= SkillConfig.FISHER_TREASURE_BONUS_LEVEL)
                        tooltip.add(Text.literal("• +10% chance de tesouro").formatted(Formatting.GREEN));
                    if (level >= SkillConfig.FISHER_DOLPHIN_GRACE_LEVEL)
                        tooltip.add(Text.literal("• Dolphin's Grace (água)").formatted(Formatting.AQUA));
                    if (level >= SkillConfig.FISHER_LUCK_SEA_LEVEL)
                        tooltip.add(Text.literal("• Luck of the Sea passivo").formatted(Formatting.AQUA));
                }
                case BLACKSMITH -> {
                    tooltip.add(Text
                            .literal("• Resistência Física: +"
                                    + (int) (level * SkillConfig.BLACKSMITH_RESISTANCE_PER_LEVEL * 100) + "%")
                            .formatted(Formatting.GOLD));
                    if (level >= SkillConfig.BLACKSMITH_IRON_SKIN_LEVEL)
                        tooltip.add(Text.literal("• Iron Skin (+5% resistência)").formatted(Formatting.GREEN));
                    if (level >= SkillConfig.BLACKSMITH_EFFICIENT_ANVIL_LEVEL)
                        tooltip.add(Text.literal("• Efficient Anvil (25% desconto)").formatted(Formatting.GREEN));
                    if (level >= SkillConfig.BLACKSMITH_FORGED_RESILIENCE_LEVEL)
                        tooltip.add(Text.literal("• Forged Resilience (fogo/explosão)").formatted(Formatting.AQUA));
                    if (level >= SkillConfig.BLACKSMITH_THORNS_MASTER_LEVEL)
                        tooltip.add(Text.literal("• Thorns Master (reflexo de dano)").formatted(Formatting.AQUA));
                }
                case BUILDER -> {
                    double reach = level * SkillConfig.BUILDER_REACH_PER_LEVEL;
                    tooltip.add(Text.literal("• Alcance Extra: +" + String.format("%.1f", reach) + " blocos")
                            .formatted(Formatting.AQUA));
                    if (level >= SkillConfig.BUILDER_EXTENDED_REACH_LEVEL)
                        tooltip.add(Text.literal("• Extended Reach (+1 bloco)").formatted(Formatting.GREEN));
                    if (level >= SkillConfig.BUILDER_EFFICIENT_CRAFTING_LEVEL)
                        tooltip.add(Text.literal("• Efficient Crafting (economia)").formatted(Formatting.GREEN));
                    if (level >= SkillConfig.BUILDER_SAFE_LANDING_LEVEL)
                        tooltip.add(Text.literal("• Safe Landing (-25% queda)").formatted(Formatting.AQUA));
                    if (level >= SkillConfig.BUILDER_SCAFFOLD_MASTER_LEVEL)
                        tooltip.add(Text.literal("• Scaffold Master (velocidade)").formatted(Formatting.AQUA));
                    if (level >= SkillConfig.BUILDER_MASTER_REACH_LEVEL)
                        tooltip.add(Text.literal("• Master Reach (+5 blocos)").formatted(Formatting.GOLD));

                    // Key binding info for Creative Brush mode
                    if (level >= SkillConfig.BUILDER_MASTER_LEVEL) {
                        tooltip.add(Text.empty());
                        tooltip.add(
                                Text.literal("⌨ Tecla H: Toggle Modo Oco/Sólido").formatted(Formatting.LIGHT_PURPLE));
                    }
                }
                case EXPLORER -> {
                    // Velocidade base
                    int speedBonus = (int) (level * SkillConfig.EXPLORER_SPEED_PER_LEVEL * 100);
                    tooltip.add(Text.literal("• Velocidade: +" + speedBonus + "%").formatted(Formatting.GREEN));

                    int luck = level / SkillConfig.EXPLORER_LUCK_INTERVAL;
                    if (luck > 0)
                        tooltip.add(Text.literal("• Sorte: +" + luck).formatted(Formatting.GOLD));

                    if (level >= SkillConfig.EXPLORER_STEP_ASSIST_LEVEL)
                        tooltip.add(Text.literal("• Passo Leve (auto-step)").formatted(Formatting.GREEN));
                    if (level >= SkillConfig.EXPLORER_AQUATIC_LEVEL)
                        tooltip.add(Text.literal("• Aquático (+50% respiração)").formatted(Formatting.AQUA));
                    if (level >= SkillConfig.EXPLORER_NIGHT_VISION_LEVEL)
                        tooltip.add(Text.literal("• Visão Noturna (toggleável)").formatted(Formatting.AQUA));
                    if (level >= SkillConfig.EXPLORER_FEATHER_FEET_LEVEL)
                        tooltip.add(Text.literal("• Pés de Pena (-40% queda)").formatted(Formatting.AQUA));
                    if (level >= SkillConfig.EXPLORER_NETHER_WALKER_LEVEL)
                        tooltip.add(Text.literal("• Caminhante do Nether (magma imune)").formatted(Formatting.GOLD));
                    if (level >= SkillConfig.EXPLORER_MASTER_LEVEL)
                        tooltip.add(Text.literal("• Sexto Sentido (Baús/Spawners brilham)").formatted(Formatting.GOLD));

                    // Key binding info
                    if (level >= SkillConfig.EXPLORER_NIGHT_VISION_LEVEL) {
                        tooltip.add(Text.empty());
                        tooltip.add(Text
                                .literal("⌨ Tecla Z: Toggle Visão Noturna"
                                        + (level >= SkillConfig.EXPLORER_MASTER_LEVEL ? " / Ativar Sexto Sentido" : ""))
                                .formatted(Formatting.LIGHT_PURPLE));
                    }
                }

            }
        }

        return tooltip;
    }

    private Text getSkillDescription(MurilloSkillsList skill) {
        return Text.translatable("murilloskills.skill.desc." + skill.name().toLowerCase());
    }

    private Text getXpGainDescription(MurilloSkillsList skill) {
        return Text.translatable("murilloskills.tooltip.xp_gain." + skill.name().toLowerCase());
    }

    private void renderXpBar(DrawContext context, int x, int y, SkillGlobalState.SkillStats stats, boolean isLocked) {
        int width = 105;
        int height = 5;

        context.fill(x, y, x + width, y + height, XP_BAR_BG);

        double maxXp = 60 + (stats.level * 15) + (2 * stats.level * stats.level);
        float progress = (float) MathHelper.clamp(stats.xp / maxXp, 0.0, 1.0);
        int filledWidth = (int) (width * progress);

        int color = (stats.level >= 100) ? 0xFFFFAA00 : 0xFF00AA00;
        if (isLocked)
            color = 0xFFAA0000;

        context.fill(x, y, x + filledWidth, y + height, color);
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private ItemStack getSkillIcon(MurilloSkillsList skill) {
        Item item = switch (skill) {
            case MINER -> Items.IRON_PICKAXE;
            case FARMER -> Items.IRON_HOE;
            case WARRIOR -> Items.IRON_SWORD;
            case FISHER -> Items.FISHING_ROD;
            case BUILDER -> Items.BRICKS;
            case BLACKSMITH -> Items.ANVIL;
            case ARCHER -> Items.BOW;
            default -> Items.BOOK;
        };
        return new ItemStack(item);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}