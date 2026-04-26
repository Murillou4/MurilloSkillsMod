package com.murilloskills.gui;

import com.murilloskills.data.ClientSkillData;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.gui.data.SkillUiData;
import com.murilloskills.gui.renderer.RenderingHelper;
import com.murilloskills.network.SkillAbilityC2SPayload;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.PrestigeManager;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ParagonAbilityScreen extends Screen {
    private static final ColorPalette PALETTE = ColorPalette.premium();
    private static final int PANEL_MARGIN = 20;
    private static final int CARD_WIDTH = 190;
    private static final int CARD_HEIGHT = 60;
    private static final int CARD_GAP = 8;

    private final List<MurilloSkillsList> paragons = new ArrayList<>();
    private final Map<MurilloSkillsList, ButtonWidget> activateButtons = new EnumMap<>(MurilloSkillsList.class);

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int columns;
    private int hoveredIndex = -1;

    public ParagonAbilityScreen() {
        super(Text.translatable("murilloskills.paragon_ability.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.clearChildren();
        activateButtons.clear();
        refreshParagons();

        if (paragons.isEmpty()) {
            close();
            return;
        }

        columns = this.width >= 480 && paragons.size() > 1 ? 2 : 1;
        int rows = (int) Math.ceil((double) paragons.size() / columns);
        panelWidth = Math.min(this.width - PANEL_MARGIN * 2, columns * CARD_WIDTH + (columns - 1) * CARD_GAP + 28);
        panelHeight = 58 + rows * CARD_HEIGHT + Math.max(0, rows - 1) * CARD_GAP + 22;
        panelX = (this.width - panelWidth) / 2;
        panelY = Math.max(18, (this.height - panelHeight) / 2);

        for (int i = 0; i < paragons.size(); i++) {
            MurilloSkillsList skill = paragons.get(i);
            int col = i % columns;
            int row = i / columns;
            int cardX = getCardX(col);
            int cardY = getCardY(row);
            int buttonWidth = 58;
            ButtonWidget button = ButtonWidget.builder(getButtonText(skill), ignored -> activate(skill))
                    .dimensions(cardX + CARD_WIDTH - buttonWidth - 8, cardY + CARD_HEIGHT - 20, buttonWidth, 14)
                    .build();
            activateButtons.put(skill, button);
            this.addDrawableChild(button);
        }
        updateButtonStates();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackdrop(context);
        renderPanel(context);

        hoveredIndex = -1;
        long worldTime = getWorldTime();
        for (int i = 0; i < paragons.size(); i++) {
            MurilloSkillsList skill = paragons.get(i);
            int col = i % columns;
            int row = i / columns;
            int cardX = getCardX(col);
            int cardY = getCardY(row);
            boolean hovered = mouseX >= cardX && mouseX <= cardX + CARD_WIDTH
                    && mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT;
            if (hovered) {
                hoveredIndex = i;
            }
            renderParagonCard(context, skill, cardX, cardY, hovered, worldTime);
        }

        updateButtonStates();
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int key = keyInput.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            int index = key - GLFW.GLFW_KEY_1;
            if (index >= 0 && index < paragons.size()) {
                activate(paragons.get(index));
                return true;
            }
        }
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE)
                && hoveredIndex >= 0 && hoveredIndex < paragons.size()) {
            activate(paragons.get(hoveredIndex));
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            close();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void refreshParagons() {
        paragons.clear();
        for (MurilloSkillsList skill : MurilloSkillsList.values()) {
            if (ClientSkillData.isParagonSkill(skill)) {
                paragons.add(skill);
            }
        }
    }

    private void activate(MurilloSkillsList skill) {
        ClientPlayNetworking.send(new SkillAbilityC2SPayload(skill));
        close();
    }

    private void updateButtonStates() {
        for (MurilloSkillsList skill : paragons) {
            ButtonWidget button = activateButtons.get(skill);
            if (button == null) {
                continue;
            }
            boolean ready = isReady(skill, getWorldTime());
            button.active = ready;
            button.setMessage(getButtonText(skill));
        }
    }

    private Text getButtonText(MurilloSkillsList skill) {
        return isReady(skill, getWorldTime())
                ? Text.translatable("murilloskills.paragon_ability.activate")
                : Text.translatable("murilloskills.paragon_ability.wait");
    }

    private void renderBackdrop(DrawContext context) {
        context.fill(0, 0, this.width, this.height, 0xB8000000);
        for (int y = 0; y < this.height; y++) {
            float ratio = (float) y / Math.max(1, this.height);
            int r = (int) (8 + ratio * 6);
            int g = (int) (8 + ratio * 5);
            int b = (int) (14 + ratio * 10);
            context.fill(0, y, this.width, y + 1, 0x70000000 | (r << 16) | (g << 8) | b);
        }
    }

    private void renderPanel(DrawContext context) {
        context.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1,
                PALETTE.panelShadow());
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PALETTE.panelBg());
        context.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + 2, PALETTE.panelHighlight());
        RenderingHelper.drawPanelBorder(context, panelX, panelY, panelWidth, panelHeight, PALETTE.sectionBorder());
        RenderingHelper.renderCornerAccents(context, panelX, panelY, panelWidth, panelHeight, 7, PALETTE.accentGold());

        Text title = Text.translatable("murilloskills.paragon_ability.title").formatted(Formatting.GOLD,
                Formatting.BOLD);
        int titleX = panelX + (panelWidth - this.textRenderer.getWidth(title)) / 2;
        context.drawTextWithShadow(this.textRenderer, title, titleX, panelY + 12, PALETTE.textGold());

        Text subtitle = Text.translatable("murilloskills.paragon_ability.subtitle")
                .formatted(Formatting.GRAY);
        int subtitleX = panelX + (panelWidth - this.textRenderer.getWidth(subtitle)) / 2;
        context.drawText(this.textRenderer, subtitle, subtitleX, panelY + 28, PALETTE.textGray(), false);
    }

    private void renderParagonCard(DrawContext context, MurilloSkillsList skill, int x, int y, boolean hovered,
            long worldTime) {
        PlayerSkillData.SkillStats stats = ClientSkillData.get(skill);
        boolean ready = isReady(skill, worldTime);
        int accentColor = PALETTE.getSkillColor(skill);
        int cardBg = ready ? PALETTE.cardBgParagon() : PALETTE.sectionBg();
        if (hovered) {
            cardBg = PALETTE.sectionBgActive();
        }

        if (ready) {
            RenderingHelper.renderGlowingBorder(context, x, y, CARD_WIDTH, CARD_HEIGHT, PALETTE.cardGlowParagon(),
                    worldTime);
        }
        context.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, cardBg);
        RenderingHelper.drawPanelBorder(context, x, y, CARD_WIDTH, CARD_HEIGHT,
                ready ? PALETTE.accentGold() : PALETTE.sectionBorder());
        context.fill(x, y, x + 3, y + CARD_HEIGHT, accentColor);

        context.drawItem(SkillUiData.getSkillIcon(skill), x + 10, y + 10);
        Text name = Text.translatable("murilloskills.skill.name." + skill.name().toLowerCase())
                .formatted(Formatting.GOLD, Formatting.BOLD);
        context.drawTextWithShadow(this.textRenderer, name, x + 34, y + 8, PALETTE.textGold());

        Text classText = Text.translatable(skill.isMasterClass()
                ? "murilloskills.paragon_ability.class_master"
                : "murilloskills.paragon_ability.class_sub");
        context.drawText(this.textRenderer, classText, x + 34, y + 20,
                skill.isMasterClass() ? PALETTE.textYellow() : PALETTE.textAqua(), false);

        String prestige = PrestigeManager.getPrestigeSymbol(stats.prestige);
        Text levelText = Text.translatable("murilloskills.paragon_ability.level", stats.level,
                prestige.isEmpty() ? "P" + stats.prestige : prestige + " P" + stats.prestige);
        context.drawText(this.textRenderer, levelText, x + 34, y + 32, PALETTE.textLight(), false);

        CooldownInfo cooldown = getCooldownInfo(skill, worldTime);
        int barX = x + 34;
        int barY = y + 45;
        int barWidth = CARD_WIDTH - 108;
        RenderingHelper.renderProgressBar(context, barX, barY, barWidth, 7,
                ready ? 1.0f : cooldown.progress(), PALETTE.progressBarEmpty(),
                ready ? PALETTE.statusReady() : PALETTE.statusCooldown(), PALETTE.progressBarShine());

        Text status = ready
                ? Text.translatable("murilloskills.paragon_ability.ready").formatted(Formatting.GREEN)
                : Text.translatable("murilloskills.paragon_ability.cooldown", formatTime(cooldown.secondsLeft()))
                        .formatted(Formatting.RED);
        context.drawText(this.textRenderer, status, x + CARD_WIDTH - this.textRenderer.getWidth(status) - 8,
                y + 32, ready ? PALETTE.statusReady() : PALETTE.statusCooldown(), false);
    }

    private int getCardX(int col) {
        int gridWidth = columns * CARD_WIDTH + (columns - 1) * CARD_GAP;
        return panelX + (panelWidth - gridWidth) / 2 + col * (CARD_WIDTH + CARD_GAP);
    }

    private int getCardY(int row) {
        return panelY + 46 + row * (CARD_HEIGHT + CARD_GAP);
    }

    private boolean isReady(MurilloSkillsList skill, long worldTime) {
        return getCooldownInfo(skill, worldTime).secondsLeft() <= 0;
    }

    private CooldownInfo getCooldownInfo(MurilloSkillsList skill, long worldTime) {
        PlayerSkillData.SkillStats stats = ClientSkillData.get(skill);
        long cooldownTicks = getSkillCooldown(skill);
        if (stats.lastAbilityUse < 0 || cooldownTicks <= 0) {
            return new CooldownInfo(0, 1.0f);
        }
        long elapsed = Math.max(0, worldTime - stats.lastAbilityUse);
        long ticksLeft = Math.max(0, cooldownTicks - elapsed);
        float progress = cooldownTicks <= 0 ? 1.0f : Math.min(1.0f, elapsed / (float) cooldownTicks);
        return new CooldownInfo((ticksLeft + 19) / 20, progress);
    }

    private long getWorldTime() {
        return MinecraftClient.getInstance().world != null ? MinecraftClient.getInstance().world.getTime() : 0L;
    }

    private long getSkillCooldown(MurilloSkillsList skill) {
        return switch (skill) {
            case MINER -> SkillConfig.toTicksLong(SkillConfig.getMinerAbilityCooldownSeconds());
            case WARRIOR -> SkillConfig.toTicksLong(SkillConfig.getWarriorAbilityCooldownSeconds());
            case ARCHER -> SkillConfig.toTicksLong(SkillConfig.getArcherAbilityCooldownSeconds());
            case FARMER -> SkillConfig.toTicksLong(SkillConfig.getFarmerAbilityCooldownSeconds());
            case FISHER -> SkillConfig.toTicksLong(SkillConfig.getFisherAbilityCooldownSeconds());
            case BLACKSMITH -> SkillConfig.toTicksLong(SkillConfig.getBlacksmithAbilityCooldownSeconds());
            case BUILDER -> SkillConfig.toTicksLong(SkillConfig.getBuilderAbilityCooldownSeconds());
            case EXPLORER -> SkillConfig.toTicksLong(SkillConfig.getExplorerAbilityCooldownSeconds());
        };
    }

    private String formatTime(long seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        if (seconds >= 60) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        }
        return seconds + "s";
    }

    private record CooldownInfo(long secondsLeft, float progress) {
    }
}
