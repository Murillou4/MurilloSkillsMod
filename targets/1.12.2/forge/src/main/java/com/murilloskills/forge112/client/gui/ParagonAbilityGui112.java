package com.murilloskills.forge112.client.gui;

import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;
import com.murilloskills.forge112.network.ModNetwork112;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.murilloskills.forge112.client.gui.Forge112UiSupport.drawPanelBorder;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.flatButton;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.renderCornerAccents;
import static com.murilloskills.forge112.skills.Forge112Abilities.abilityCooldownMillis;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.data;

public final class ParagonAbilityGui112 extends GuiScreen {
    private static final int CARD_W = 196;
    private static final int CARD_H = 66;
    private static final int CARD_GAP = 8;
    private final List<SkillType> paragons = new ArrayList<SkillType>();
    private final Map<Integer, SkillType> buttons = new HashMap<Integer, SkillType>();
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int columns;
    private int hovered = -1;

    @Override
    public void initGui() {
        buttonList.clear();
        buttons.clear();
        refreshParagons();
        if (paragons.isEmpty()) {
            mc.displayGuiScreen(null);
            return;
        }
        columns = width >= 480 && paragons.size() > 1 ? 2 : 1;
        int rows = (paragons.size() + columns - 1) / columns;
        panelW = Math.min(width - 24, columns * CARD_W + (columns - 1) * CARD_GAP + 28);
        panelH = 60 + rows * CARD_H + Math.max(0, rows - 1) * CARD_GAP + 18;
        panelX = (width - panelW) / 2;
        panelY = Math.max(18, (height - panelH) / 2);
        for (int i = 0; i < paragons.size(); i++) {
            SkillType skill = paragons.get(i);
            int x = cardX(i);
            int y = cardY(i);
            int id = 32000 + i;
            buttonList.add(flatButton(id, x + CARD_W - 62, y + CARD_H - 20, 54, 14, buttonText(skill)));
            buttons.put(Integer.valueOf(id), skill);
        }
        updateButtons();
    }

    private void refreshParagons() {
        paragons.clear();
        if (mc.player == null) {
            return;
        }
        PlayerSkillDataCore data = data(mc.player);
        for (SkillType skill : SkillType.values()) {
            if (data.isParagonSkill(skill)) {
                paragons.add(skill);
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        SkillType skill = buttons.get(Integer.valueOf(button.id));
        if (skill != null && mc.player != null && isReady(skill)) {
            ModNetwork112.sendSkillAbility(skill);
            mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        if (keyCode >= Keyboard.KEY_1 && keyCode <= Keyboard.KEY_9) {
            int index = keyCode - Keyboard.KEY_1;
            if (index >= 0 && index < paragons.size()) {
                activate(paragons.get(index));
                return;
            }
        }
        if ((keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_SPACE)
                && hovered >= 0 && hovered < paragons.size()) {
            activate(paragons.get(hovered));
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 1) {
            mc.displayGuiScreen(null);
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void activate(SkillType skill) {
        if (mc.player != null && skill != null && isReady(skill)) {
            ModNetwork112.sendSkillAbility(skill);
            mc.displayGuiScreen(null);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GuiScreen.drawRect(0, 0, width, height, 0xB8000000);
        GuiScreen.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, Palette.SECTION_BG);
        drawPanelBorder(panelX, panelY, panelW, panelH, Palette.SECTION_BORDER);
        renderCornerAccents(panelX, panelY, panelW, panelH, 8, Palette.ACCENT_GOLD);
        GuiScreen.drawRect(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 34, Palette.PANEL_BG_HEADER);
        drawCenteredString(fontRenderer, "Paragon Ability", width / 2, panelY + 10, Palette.TEXT_GOLD);
        drawCenteredString(fontRenderer, "Choose which level 100 skill to activate", width / 2, panelY + 23,
                Palette.TEXT_MUTED);

        hovered = -1;
        for (int i = 0; i < paragons.size(); i++) {
            int x = cardX(i);
            int y = cardY(i);
            boolean isHovered = mouseX >= x && mouseX <= x + CARD_W && mouseY >= y && mouseY <= y + CARD_H;
            if (isHovered) {
                hovered = i;
            }
            drawCard(paragons.get(i), x, y, isHovered);
        }
        updateButtons();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCard(SkillType skill, int x, int y, boolean hovered) {
        SkillStatsCore stats = data(mc.player).getSkill(skill);
        int accent = UiData.skillColor(skill);
        boolean ready = isReady(skill);
        GuiScreen.drawRect(x, y, x + CARD_W, y + CARD_H, hovered ? Palette.SECTION_BG_ACTIVE : Palette.CARD_BG_SUBTLE);
        drawPanelBorder(x, y, CARD_W, CARD_H, ready ? Palette.ACCENT_GOLD : Palette.SECTION_BORDER);
        GuiScreen.drawRect(x, y, x + 3, y + CARD_H, accent);
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();
        mc.getRenderItem().renderItemAndEffectIntoGUI(UiData.itemForSkill(skill), x + 10, y + 12);
        GlStateManager.disableDepth();
        RenderHelper.disableStandardItemLighting();
        drawString(fontRenderer, UiData.title(skill), x + 34, y + 8, Palette.TEXT_GOLD);
        drawString(fontRenderer, skill.isMasterClass() ? "Master class" : "Sub class", x + 34, y + 20,
                skill.isMasterClass() ? Palette.TEXT_GOLD : Palette.TEXT_AQUA);
        drawString(fontRenderer, "Level " + stats.getLevel() + " P" + stats.getPrestige(), x + 34, y + 32,
                Palette.TEXT_LIGHT);
        int barX = x + 34;
        int barY = y + 47;
        int barW = CARD_W - 108;
        double progress = cooldownProgress(skill);
        GuiScreen.drawRect(barX, barY, barX + barW, barY + 7, 0xFF282A36);
        GuiScreen.drawRect(barX + 1, barY + 1, barX + 1 + (int) ((barW - 2) * progress), barY + 6,
                ready ? Palette.ACCENT_GREEN : Palette.ACCENT_GOLD);
        String status = ready ? "Ready" : formatCooldown(skill);
        drawString(fontRenderer, status, x + CARD_W - fontRenderer.getStringWidth(status) - 8, y + 32,
                ready ? Palette.TEXT_GREEN : Palette.TEXT_MUTED);
    }

    private void updateButtons() {
        for (GuiButton button : buttonList) {
            SkillType skill = buttons.get(Integer.valueOf(button.id));
            if (skill != null) {
                button.enabled = isReady(skill);
                button.displayString = buttonText(skill);
            }
        }
    }

    private String buttonText(SkillType skill) {
        return isReady(skill) ? "Use" : "Wait";
    }

    private boolean isReady(SkillType skill) {
        if (mc.player == null || skill == null) {
            return false;
        }
        SkillStatsCore stats = data(mc.player).getSkill(skill);
        long last = stats.getLastAbilityUse();
        return last <= 0L || System.currentTimeMillis() - last >= abilityCooldownMillis(stats.getPrestige());
    }

    private double cooldownProgress(SkillType skill) {
        if (mc.player == null || skill == null) {
            return 0.0D;
        }
        SkillStatsCore stats = data(mc.player).getSkill(skill);
        long cooldown = abilityCooldownMillis(stats.getPrestige());
        long elapsed = stats.getLastAbilityUse() <= 0L ? cooldown : System.currentTimeMillis() - stats.getLastAbilityUse();
        return Math.max(0.0D, Math.min(1.0D, elapsed / (double) Math.max(1L, cooldown)));
    }

    private String formatCooldown(SkillType skill) {
        SkillStatsCore stats = data(mc.player).getSkill(skill);
        long left = Math.max(0L, abilityCooldownMillis(stats.getPrestige())
                - (System.currentTimeMillis() - stats.getLastAbilityUse()));
        long seconds = (left + 999L) / 1000L;
        return seconds >= 60L ? (seconds / 60L) + "m " + (seconds % 60L) + "s" : seconds + "s";
    }

    private int cardX(int index) {
        int col = index % columns;
        int gridW = columns * CARD_W + (columns - 1) * CARD_GAP;
        return panelX + (panelW - gridW) / 2 + col * (CARD_W + CARD_GAP);
    }

    private int cardY(int index) {
        int row = index / columns;
        return panelY + 48 + row * (CARD_H + CARD_GAP);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
