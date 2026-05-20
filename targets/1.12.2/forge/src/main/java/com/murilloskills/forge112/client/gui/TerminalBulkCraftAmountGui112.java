package com.murilloskills.forge112.client.gui;

import com.murilloskills.forge112.client.render.Forge112NotificationHud;
import com.murilloskills.forge112.client.data.TerminalMachineTargetClientState112;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

import static com.murilloskills.forge112.client.gui.Forge112UiSupport.clamp;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.drawPanelBorder;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.flatButton;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.renderCornerAccents;

public final class TerminalBulkCraftAmountGui112 extends GuiScreen {
    private static final int MINUS = 36000;
    private static final int PLUS = 36001;
    private static final int CONFIRM = 36002;
    private static final int CANCEL = 36003;
    private final GuiScreen parent;
    private int amount = 16;
    private int panelX;
    private int panelY;

    public TerminalBulkCraftAmountGui112(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        panelX = (width - 260) / 2;
        panelY = (height - 126) / 2;
        buttonList.add(flatButton(MINUS, panelX + 44, panelY + 58, 34, 20, "-"));
        buttonList.add(flatButton(PLUS, panelX + 182, panelY + 58, 34, 20, "+"));
        buttonList.add(flatButton(CONFIRM, panelX + 112, panelY + 96, 64, 20, "Craft"));
        buttonList.add(flatButton(CANCEL, panelX + 184, panelY + 96, 64, 20, "Cancel"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == MINUS || button.id == PLUS) {
            amount = clamp(amount + (button.id == PLUS ? 1 : -1), 1, 64);
        } else if (button.id == CONFIRM) {
            TerminalMachineTargetClientState112.setTarget("Bulk craft", amount, 3500L);
            Forge112NotificationHud.addLocalCard("Terminal", "Bulk craft", amount + " items queued", Palette.ACCENT_GOLD);
            mc.displayGuiScreen(parent);
        } else if (button.id == CANCEL) {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            TerminalMachineTargetClientState112.setTarget("Bulk craft", amount, 3500L);
            Forge112NotificationHud.addLocalCard("Terminal", "Bulk craft", amount + " items queued", Palette.ACCENT_GOLD);
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GuiScreen.drawRect(0, 0, width, height, 0xB0000000);
        GuiScreen.drawRect(panelX, panelY, panelX + 260, panelY + 126, Palette.SECTION_BG);
        drawPanelBorder(panelX, panelY, 260, 126, Palette.SECTION_BORDER);
        renderCornerAccents(panelX, panelY, 260, 126, 7, Palette.ACCENT_GOLD);
        drawCenteredString(fontRenderer, "Bulk Craft Amount", panelX + 130, panelY + 14, Palette.TEXT_GOLD);
        drawCenteredString(fontRenderer, "Amount", panelX + 130, panelY + 42, Palette.TEXT_MUTED);
        drawCenteredString(fontRenderer, String.valueOf(amount), panelX + 130, panelY + 64, Palette.TEXT_LIGHT);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
