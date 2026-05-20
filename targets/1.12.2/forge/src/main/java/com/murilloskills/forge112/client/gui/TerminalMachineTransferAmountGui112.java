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

public final class TerminalMachineTransferAmountGui112 extends GuiScreen {
    private static final int MINUS = 37000;
    private static final int PLUS = 37001;
    private static final int MODE = 37002;
    private static final int CONFIRM = 37003;
    private static final int CANCEL = 37004;
    private final GuiScreen parent;
    private int amount = 16;
    private boolean toMachine = true;
    private int panelX;
    private int panelY;

    public TerminalMachineTransferAmountGui112(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        panelX = (width - 280) / 2;
        panelY = (height - 142) / 2;
        buttonList.add(flatButton(MINUS, panelX + 54, panelY + 66, 34, 20, "-"));
        buttonList.add(flatButton(PLUS, panelX + 192, panelY + 66, 34, 20, "+"));
        buttonList.add(flatButton(MODE, panelX + 70, panelY + 94, 140, 20, modeText()));
        buttonList.add(flatButton(CONFIRM, panelX + 134, panelY + 114, 64, 20, "Move"));
        buttonList.add(flatButton(CANCEL, panelX + 206, panelY + 114, 64, 20, "Cancel"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == MINUS || button.id == PLUS) {
            amount = clamp(amount + (button.id == PLUS ? 1 : -1), 1, 64);
        } else if (button.id == MODE) {
            toMachine = !toMachine;
            button.displayString = modeText();
        } else if (button.id == CONFIRM) {
            confirm();
        } else if (button.id == CANCEL) {
            mc.displayGuiScreen(parent);
        }
    }

    private void confirm() {
        TerminalMachineTargetClientState112.setTarget(modeText(), amount, 3500L);
        Forge112NotificationHud.addLocalCard("Terminal", modeText(), amount + " items", Palette.ACCENT_BLUE);
        mc.displayGuiScreen(parent);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            confirm();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GuiScreen.drawRect(0, 0, width, height, 0xB0000000);
        GuiScreen.drawRect(panelX, panelY, panelX + 280, panelY + 142, Palette.SECTION_BG);
        drawPanelBorder(panelX, panelY, 280, 142, Palette.SECTION_BORDER);
        renderCornerAccents(panelX, panelY, 280, 142, 7, Palette.ACCENT_GOLD);
        drawCenteredString(fontRenderer, "Machine Transfer", panelX + 140, panelY + 14, Palette.TEXT_GOLD);
        drawCenteredString(fontRenderer, modeText(), panelX + 140, panelY + 40, Palette.TEXT_MUTED);
        drawCenteredString(fontRenderer, String.valueOf(amount), panelX + 140, panelY + 72, Palette.TEXT_LIGHT);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String modeText() {
        return toMachine ? "To machine" : "From machine";
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
