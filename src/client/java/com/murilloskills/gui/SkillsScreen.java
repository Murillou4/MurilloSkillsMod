package com.murilloskills.gui;

import com.murilloskills.data.ClientSkillData;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.network.ParagonActivationC2SPayload;
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

    // Cores
    private static final int BG_OVERLAY = 0xCC000000;
    private static final int CARD_BG_NORMAL = 0xFF202020;
    private static final int CARD_BG_HOVER = 0xFF303030;
    private static final int CARD_BG_PARAGON = 0xFF2A2A10;
    private static final int CARD_BG_SELECTED = 0xFF1A3A1A; // Green tint for selected skills
    private static final int CARD_BG_LOCKED = 0xFF1A1A1A; // Darker for locked skills
    private static final int CARD_BG_PENDING_SELECT = 0xFF2A2A3A; // Blue tint for pending selection
    private static final int BORDER_NORMAL = 0xFF555555;
    private static final int BORDER_HOVER = 0xFFFFAA00;
    private static final int BORDER_SELECTED = 0xFF00FF00; // Green border for selected
    private static final int BORDER_LOCKED = 0xFF333333; // Dark border for locked
    private static final int XP_BAR_BG = 0xFF000000;

    // Layout
    private final int cardWidth = 140;
    private final int cardHeight = 55;
    private final int padding = 10;
    private int startX, startY;

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
            btn.setMessage(Text.literal(isSelected ? "✓ SELECIONADA" : "SELECIONAR"));
        }

        if (confirmButton != null) {
            confirmButton.active = pendingSelection.size() == 2;
            confirmButton.setMessage(Text.literal("CONFIRMAR ESCOLHA (" + pendingSelection.size() + "/2)"));
        }
    }

    @Override
    protected void init() {
        super.init();

        // Calcular posições iniciais
        int totalWidth = (cardWidth * 2) + padding;
        this.startX = (this.width - totalWidth) / 2;
        this.startY = 40;

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

                int col = i % 2;
                int row = i / 2;
                int x = startX + (col * (cardWidth + padding));
                int y = startY + (row * (cardHeight + padding));

                int btnX = x + 20;
                int btnY = y + 35;
                int btnWidth = 100;
                int btnHeight = 16;

                boolean isSelected = pendingSelection.contains(skill);
                ButtonWidget selectBtn = ButtonWidget.builder(
                        Text.literal(isSelected ? "✓ SELECIONADA" : "SELECIONAR"),
                        (button) -> {
                            // Toggle selection
                            if (pendingSelection.contains(skill)) {
                                pendingSelection.remove(skill);
                            } else if (pendingSelection.size() < 2) {
                                pendingSelection.add(skill);
                            }
                            updateSelectionButtonStates();
                        })
                        .dimensions(btnX, btnY, btnWidth, btnHeight)
                        .build();

                selectionButtons.put(skill, selectBtn);
                this.addDrawableChild(selectBtn);
            }

            // Add confirm button at the bottom
            int confirmBtnWidth = 200;
            int confirmBtnHeight = 20;
            int confirmBtnX = (this.width - confirmBtnWidth) / 2;
            int confirmBtnY = this.height - 40;

            confirmButton = ButtonWidget.builder(Text.literal("CONFIRMAR ESCOLHA (0/2)"), (button) -> {
                        if (pendingSelection.size() == 2) {
                            List<MurilloSkillsList> selected = new ArrayList<>(pendingSelection);
                            ClientPlayNetworking.send(new SkillSelectionC2SPayload(selected));
                            this.close();
                        }
                    })
                    .dimensions(confirmBtnX, confirmBtnY, confirmBtnWidth, confirmBtnHeight)
                    .build();
            confirmButton.active = false; // Disabled until 2 skills are selected
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
                    int col = i % 2;
                    int row = i / 2;
                    int x = startX + (col * (cardWidth + padding));
                    int y = startY + (row * (cardHeight + padding));

                    int btnX = x + 20;
                    int btnY = y + 35;
                    int btnWidth = 100;
                    int btnHeight = 16;

                    ButtonWidget paragonBtn = ButtonWidget.builder(Text.literal("TORNAR PARAGON"), (button) -> {
                                ClientPlayNetworking.send(new ParagonActivationC2SPayload(skill));
                                this.close();
                            })
                            .dimensions(btnX, btnY, btnWidth, btnHeight)
                            .build();

                    this.addDrawableChild(paragonBtn);
                }
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. Fundo Escuro da Tela
        context.fill(0, 0, this.width, this.height, BG_OVERLAY);

        boolean selectionMode = isSelectionMode();

        // Title changes based on mode
        if (selectionMode) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("ESCOLHA SUAS HABILIDADES").formatted(Formatting.GOLD, Formatting.BOLD), this.width / 2, 10, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Selecione 2 habilidades para evoluir").formatted(Formatting.YELLOW), this.width / 2, 22, 0xFFFFFFFF);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFFFF);
        }

        MurilloSkillsList[] skills = MurilloSkillsList.values();
        List<Text> tooltipToRender = null;
        MurilloSkillsList paragon = ClientSkillData.getParagonSkill();
        long worldTime = MinecraftClient.getInstance().world != null ? MinecraftClient.getInstance().world.getTime() : 0;

        // 2. Renderizar os Cartões (Fundo, Ícones, Texto)
        for (int i = 0; i < skills.length; i++) {
            MurilloSkillsList skill = skills[i];
            var stats = ClientSkillData.get(skill);

            int col = i % 2;
            int row = i / 2;
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
                    borderColor = isHovered ? BORDER_HOVER : BORDER_NORMAL;
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
            context.drawTextWithShadow(this.textRenderer, capitalize(skill.name()), x + 28, y + 5, titleColor);

            // Lock icon for non-selected skills (normal mode only)
            if (!selectionMode && isLocked) {
                context.drawTextWithShadow(this.textRenderer, "🔒", x + cardWidth - 15, y + 5, 0xFF666666);
            }

            // Nível
            String lvlStr = String.valueOf(stats.level);
            int lvlWidth = this.textRenderer.getWidth("Lvl " + lvlStr);
            int lvlColor = isLocked ? 0xFF666666 : 0xFFFFFFFF;
            context.drawTextWithShadow(this.textRenderer, "Lvl " + lvlStr, x + cardWidth - lvlWidth - 5, y + 5, lvlColor);

            // Barra de XP
            renderXpBar(context, x + 28, y + 25, stats, isLocked);

            // Status text below XP bar (buttons handle selection mode text)
            if (isLocked) {
                context.drawText(this.textRenderer, "BLOQUEADA", x + 28, y + 40, 0xFFAA0000, false);
            } else if (isParagon) {
                long cooldownTicks = getSkillCooldown(skill);
                long timeSinceUse = worldTime - stats.lastAbilityUse;

                if (timeSinceUse < cooldownTicks) {
                    long secondsLeft = (cooldownTicks - timeSinceUse) / 20;
                    String cdText = "CD: " + formatTime(secondsLeft);
                    context.drawText(this.textRenderer, cdText, x + 28, y + 40, 0xFFFF5555, false);
                } else {
                    context.drawText(this.textRenderer, "PRONTO (Z)", x + 28, y + 40, 0xFF00FF00, false);
                }
                context.drawTextWithShadow(this.textRenderer, "👑", x + 120, y - 4, 0xFFFFAA00);
            } else if (isSelected) {
                context.drawText(this.textRenderer, "ATIVA", x + 28, y + 40, 0xFF00AA00, false);
            }

            // Tooltip Logic
            if (isHovered) {
                boolean hoveringButton = this.children().stream()
                        .filter(element -> element instanceof ButtonWidget)
                        .anyMatch(btn -> ((ButtonWidget) btn).isMouseOver(mouseX, mouseY));

                if (!hoveringButton) {
                    tooltipToRender = getSkillTooltip(skill, stats.level, isLocked, isParagon, selectionMode, isPendingSelect);
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
        if (seconds > 60) return (seconds / 60) + "m";
        return seconds + "s";
    }

    private long getSkillCooldown(MurilloSkillsList skill) {
        return switch (skill) {
            case MINER -> SkillConfig.MINER_ABILITY_COOLDOWN;
            case WARRIOR -> SkillConfig.WARRIOR_ABILITY_COOLDOWN;
            default -> 6000L;
        };
    }

    private String getSpecialAbilityDescription(MurilloSkillsList skill) {
        return switch (skill) {
            case MINER -> "Raio-X: Revela minérios valiosos em uma grande área.";
            case WARRIOR -> "Berserk: Ganha Força II, Velocidade II e Resistência.";
            case FARMER -> "Colheita: Cresce instantaneamente as plantações ao redor.";
            case ARCHER -> "Olho de Águia: O próximo tiro causa dano massivo.";
            default -> "Habilidade especial em desenvolvimento.";
        };
    }

    private List<Text> getSkillTooltip(MurilloSkillsList skill, int level, boolean isLocked, boolean isParagon, boolean selectionMode, boolean isPendingSelect) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.literal(capitalize(skill.name())).formatted(Formatting.GOLD, Formatting.BOLD));

        if (selectionMode) {
            // Selection mode tooltip
            if (isPendingSelect) {
                tooltip.add(Text.literal("✓ SELECIONADA").formatted(Formatting.GREEN));
            }
            tooltip.add(Text.empty());
            tooltip.add(Text.literal("Clique para " + (isPendingSelect ? "desmarcar" : "selecionar")).formatted(Formatting.YELLOW));
            tooltip.add(Text.empty());
            tooltip.add(Text.literal("Descrição:").formatted(Formatting.GRAY));
            tooltip.add(Text.literal(getSkillDescription(skill)).formatted(Formatting.WHITE));
        } else {
            // Normal mode tooltip
            if (isParagon) tooltip.add(Text.literal("★ PARAGON ATIVO ★").formatted(Formatting.YELLOW));
            if (isLocked) {
                tooltip.add(Text.literal("🔒 BLOQUEADA").formatted(Formatting.RED));
                tooltip.add(Text.literal("Esta habilidade não foi selecionada.").formatted(Formatting.DARK_GRAY));
                tooltip.add(Text.literal("Você não pode ganhar XP nela.").formatted(Formatting.DARK_GRAY));
                return tooltip;
            }

            tooltip.add(Text.empty());
            tooltip.add(Text.literal("Habilidade Especial (Tecla Z):").formatted(Formatting.GRAY));
            tooltip.add(Text.literal(getSpecialAbilityDescription(skill)).formatted(Formatting.BLUE));

            tooltip.add(Text.empty());
            tooltip.add(Text.literal("Passivas:").formatted(Formatting.GRAY));

            switch (skill) {
                case MINER -> {
                    int speed = (int) (level * SkillConfig.MINER_SPEED_PER_LEVEL * 100);
                    tooltip.add(Text.literal("• Mineração: +" + speed + "% Vel.").formatted(Formatting.GREEN));

                    int fortune = (int) (level * SkillConfig.MINER_FORTUNE_PER_LEVEL);
                    if (fortune > 0) tooltip.add(Text.literal("• Fortuna Extra: +" + fortune).formatted(Formatting.GREEN));

                    if (level >= SkillConfig.MINER_NIGHT_VISION_LEVEL) tooltip.add(Text.literal("• Visão Noturna (Cavernas)").formatted(Formatting.AQUA));
                    if (level >= SkillConfig.MINER_DURABILITY_LEVEL) tooltip.add(Text.literal("• Durabilidade Infinita (Chance)").formatted(Formatting.AQUA));
                    if (level >= SkillConfig.MINER_RADAR_LEVEL) tooltip.add(Text.literal("• Radar de Minérios").formatted(Formatting.AQUA));
                }
                case WARRIOR -> {
                    double damage = level * SkillConfig.WARRIOR_DAMAGE_PER_LEVEL;
                    tooltip.add(Text.literal("• Dano Base: +" + String.format("%.1f", damage)).formatted(Formatting.RED));

                    int extraHearts = 0;
                    if(level >= 10) extraHearts++;
                    if(level >= 50) extraHearts++;
                    if(level >= 100) extraHearts+=3;
                    if(extraHearts > 0) tooltip.add(Text.literal("• Vida Max: +" + extraHearts + " ❤").formatted(Formatting.RED));

                    if (level >= SkillConfig.RESISTANCE_UNLOCK_LEVEL) tooltip.add(Text.literal("• Pele de Ferro (Resistência)").formatted(Formatting.GOLD));
                    if (level >= SkillConfig.LIFESTEAL_UNLOCK_LEVEL) tooltip.add(Text.literal("• Vampirismo (Roubo de Vida)").formatted(Formatting.DARK_PURPLE));
                }
                case FARMER -> {
                    tooltip.add(Text.literal("• Crescimento Extra (Em breve)").formatted(Formatting.GREEN));
                    tooltip.add(Text.literal("• Colheita Dupla (Em breve)").formatted(Formatting.GREEN));
                }
                case ARCHER -> {
                    tooltip.add(Text.literal("• Dano de Flecha (Em breve)").formatted(Formatting.GREEN));
                    tooltip.add(Text.literal("• Precisão (Em breve)").formatted(Formatting.GREEN));
                }
                case FISHER -> tooltip.add(Text.literal("• Sorte no Mar (Em breve)").formatted(Formatting.GREEN));
                default -> tooltip.add(Text.literal("• Status em desenvolvimento").formatted(Formatting.DARK_GRAY));
            }
        }

        return tooltip;
    }

    private String getSkillDescription(MurilloSkillsList skill) {
        return switch (skill) {
            case MINER -> "Especialista em mineração. Ganha bônus ao minerar blocos.";
            case WARRIOR -> "Mestre do combate. Causa mais dano e tem mais vida.";
            case FARMER -> "Agricultor experiente. Melhora colheitas e plantações.";
            case ARCHER -> "Atirador preciso. Aumenta dano e precisão com arcos.";
            case FISHER -> "Pescador habilidoso. Melhora sorte na pesca.";
            case BUILDER -> "Construtor talentoso. Bônus em construção.";
            case BLACKSMITH -> "Ferreiro mestre. Melhora forja e reparos.";
            case EXPLORER -> "Explorador destemido. Bônus em exploração.";
        };
    }

    private void renderXpBar(DrawContext context, int x, int y, SkillGlobalState.SkillStats stats, boolean isLocked) {
        int width = 105;
        int height = 5;

        context.fill(x, y, x + width, y + height, XP_BAR_BG);

        double maxXp = 50 + (stats.level * 10) + (4 * stats.level * stats.level);
        float progress = (float) MathHelper.clamp(stats.xp / maxXp, 0.0, 1.0);
        int filledWidth = (int) (width * progress);

        int color = (stats.level >= 100) ? 0xFFFFAA00 : 0xFF00AA00;
        if (isLocked) color = 0xFFAA0000;

        context.fill(x, y, x + filledWidth, y + height, color);
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
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
    public boolean shouldPause() { return false; }
}