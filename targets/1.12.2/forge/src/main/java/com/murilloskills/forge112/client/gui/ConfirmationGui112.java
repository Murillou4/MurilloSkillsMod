package com.murilloskills.forge112.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

import static com.murilloskills.forge112.client.gui.Forge112UiSupport.drawPanelBorder;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.flatButton;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.renderCornerAccents;

public final class ConfirmationGui112 extends GuiScreen {
    private static final int CONFIRM = 33000;
    private static final int CANCEL = 33001;
    private final GuiScreen parent;
    private final String title;
    private final String message;
    private final Runnable onConfirm;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public ConfirmationGui112(GuiScreen parent, String title, String message, Runnable onConfirm) {
        this.parent = parent;
        this.title = title == null ? "Confirm" : title;
        this.message = message == null ? "" : message;
        this.onConfirm = onConfirm;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        panelW = Math.min(330, width - 24);
        panelH = 112;
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        buttonList.add(flatButton(CONFIRM, panelX + panelW - 148, panelY + panelH - 30, 64, 20, "Confirm"));
        buttonList.add(flatButton(CANCEL, panelX + panelW - 78, panelY + panelH - 30, 64, 20, "Cancel"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == CONFIRM) {
            if (onConfirm != null) {
                onConfirm.run();
            }
            mc.displayGuiScreen(parent);
        } else if (button.id == CANCEL) {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GuiScreen.drawRect(0, 0, width, height, 0xB0000000);
        GuiScreen.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, Palette.SECTION_BG);
        drawPanelBorder(panelX, panelY, panelW, panelH, Palette.SECTION_BORDER);
        renderCornerAccents(panelX, panelY, panelW, panelH, 7, Palette.ACCENT_GOLD);
        GuiScreen.drawRect(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 28, Palette.PANEL_BG_HEADER);
        drawCenteredString(fontRenderer, title, panelX + panelW / 2, panelY + 10, Palette.TEXT_GOLD);
        drawCenteredString(fontRenderer, fit(message, panelW - 24), panelX + panelW / 2, panelY + 48,
                Palette.TEXT_LIGHT);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String fit(String text, int maxWidth) {
        String out = text == null ? "" : text;
        while (out.length() > 3 && fontRenderer.getStringWidth(out) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }
}
