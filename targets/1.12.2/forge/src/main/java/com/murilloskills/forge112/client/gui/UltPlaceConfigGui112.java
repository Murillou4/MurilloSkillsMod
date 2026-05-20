package com.murilloskills.forge112.client.gui;

import com.murilloskills.forge112.client.data.UltPlaceClientState112;
import com.murilloskills.forge112.client.data.UltPlaceShape112;
import com.murilloskills.forge112.client.render.Forge112NotificationHud;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

import static com.murilloskills.forge112.client.gui.Forge112UiSupport.clamp;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.drawPanelBorder;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.flatButton;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.renderCornerAccents;

public final class UltPlaceConfigGui112 extends GuiScreen {
    private static final int BACK = 34000;
    private static final int ENABLE = 34001;
    private static final int PREVIEW = 34002;
    private static final int SHAPE_BASE = 34100;
    private static final int SIZE_MINUS = 34200;
    private static final int SIZE_PLUS = 34201;
    private static final int LENGTH_MINUS = 34202;
    private static final int LENGTH_PLUS = 34203;
    private static final int HEIGHT_MINUS = 34204;
    private static final int HEIGHT_PLUS = 34205;
    private static final int SPACING_MINUS = 34206;
    private static final int SPACING_PLUS = 34207;
    private static final int VARIANT = 34208;
    private final GuiScreen parent;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int shapeY;
    private int cardX;
    private int cardY;
    private int cardW;

    public UltPlaceConfigGui112(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        panelW = Math.min(700, width - 20);
        panelH = Math.min(360, height - 20);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        shapeY = panelY + 48;
        cardX = panelX + 14;
        cardY = panelY + 120;
        cardW = panelW - 28;
        buttonList.add(flatButton(BACK, panelX + panelW - 82, panelY + panelH - 28, 70, 20, "Back"));
        buttonList.add(flatButton(ENABLE, panelX + 14, panelY + panelH - 28, 92, 20,
                "UltPlace: " + (UltPlaceClientState112.isEnabled() ? "ON" : "OFF")));
        buttonList.add(flatButton(PREVIEW, panelX + 112, panelY + panelH - 28, 92, 20,
                "Preview: " + (UltPlaceClientState112.isPreviewEnabled() ? "ON" : "OFF")));
        UltPlaceShape112[] shapes = UltPlaceShape112.values();
        int cols = panelW >= 600 ? 5 : 3;
        int gap = 5;
        int shapeW = (panelW - 28 - (cols - 1) * gap) / cols;
        for (int i = 0; i < shapes.length; i++) {
            int col = i % cols;
            int row = i / cols;
            GuiButton button = flatButton(SHAPE_BASE + i, panelX + 14 + col * (shapeW + gap),
                    shapeY + row * 24, shapeW, 20, shapes[i].label());
            button.enabled = shapes[i] != UltPlaceClientState112.getSelectedShape();
            buttonList.add(button);
        }
        int rowX = cardX + 150;
        int rowY = cardY + 42;
        addStepper(SIZE_MINUS, SIZE_PLUS, rowX, rowY);
        addStepper(LENGTH_MINUS, LENGTH_PLUS, rowX, rowY + 26);
        addStepper(HEIGHT_MINUS, HEIGHT_PLUS, rowX, rowY + 52);
        addStepper(SPACING_MINUS, SPACING_PLUS, rowX, rowY + 78);
        buttonList.add(flatButton(VARIANT, rowX + 96, rowY + 104, 104, 18, variantText()));
    }

    private void addStepper(int minus, int plus, int x, int y) {
        buttonList.add(flatButton(minus, x, y, 24, 18, "-"));
        buttonList.add(flatButton(plus, x + 78, y, 24, 18, "+"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        int shapeIndex = button.id - SHAPE_BASE;
        if (button.id == BACK) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (button.id == ENABLE) {
            boolean enabled = UltPlaceClientState112.toggleEnabled();
            if (mc.player != null) {
                mc.player.sendChatMessage("/murilloskills toggle BUILDER ultplace");
            }
            Forge112NotificationHud.addLocalCard("UltPlace", enabled ? "Enabled" : "Disabled",
                    UltPlaceClientState112.summary(), Palette.ACCENT_BLUE);
        } else if (button.id == PREVIEW) {
            UltPlaceClientState112.togglePreview();
        } else if (shapeIndex >= 0 && shapeIndex < UltPlaceShape112.values().length) {
            UltPlaceClientState112.setSelectedShape(UltPlaceShape112.values()[shapeIndex]);
        } else if (button.id == SIZE_MINUS || button.id == SIZE_PLUS) {
            UltPlaceClientState112.adjustSize(button.id == SIZE_PLUS ? 1 : -1);
        } else if (button.id == LENGTH_MINUS || button.id == LENGTH_PLUS) {
            UltPlaceClientState112.adjustLength(button.id == LENGTH_PLUS ? 1 : -1);
        } else if (button.id == HEIGHT_MINUS || button.id == HEIGHT_PLUS) {
            UltPlaceClientState112.adjustHeight(button.id == HEIGHT_PLUS ? 1 : -1);
        } else if (button.id == SPACING_MINUS || button.id == SPACING_PLUS) {
            UltPlaceClientState112.adjustSpacing(button.id == SPACING_PLUS ? 1 : -1);
        } else if (button.id == VARIANT) {
            UltPlaceClientState112.adjustVariant();
        }
        initGui();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (keyCode == Keyboard.KEY_TAB) {
            UltPlaceClientState112.setSelectedShape(UltPlaceClientState112.getSelectedShape().next());
            initGui();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GuiScreen.drawRect(0, 0, width, height, 0xB8000000);
        GuiScreen.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, Palette.SECTION_BG);
        drawPanelBorder(panelX, panelY, panelW, panelH, Palette.SECTION_BORDER);
        renderCornerAccents(panelX, panelY, panelW, panelH, 9, Palette.ACCENT_GOLD);
        GuiScreen.drawRect(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 34, Palette.PANEL_BG_HEADER);
        drawCenteredString(fontRenderer, "UltPlace Config", width / 2, panelY + 10, Palette.TEXT_GOLD);
        drawCenteredString(fontRenderer, "Builder mass placement setup", width / 2, panelY + 23,
                Palette.TEXT_MUTED);

        drawSection(cardX, cardY, cardW, panelH - 172, "Shape settings");
        int rowX = cardX + 24;
        int valueX = cardX + 196;
        int rowY = cardY + 46;
        drawRow("Shape", UltPlaceClientState112.getSelectedShape().label(), rowX, valueX, rowY);
        drawRow("Size", String.valueOf(UltPlaceClientState112.getSize()), rowX, valueX, rowY + 26);
        drawRow("Length", String.valueOf(UltPlaceClientState112.getLength()), rowX, valueX, rowY + 52);
        drawRow("Height", String.valueOf(UltPlaceClientState112.getHeight()), rowX, valueX, rowY + 78);
        drawRow("Spacing", String.valueOf(UltPlaceClientState112.getSpacing()), rowX, valueX, rowY + 104);
        drawRow("Variant", variantText(), rowX, valueX, rowY + 130);
        drawPreviewCard(cardX + cardW - 214, cardY + 40, 190, 112);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawSection(int x, int y, int w, int h, String title) {
        GuiScreen.drawRect(x, y, x + w, y + h, Palette.CARD_BG_SUBTLE);
        drawPanelBorder(x, y, w, h, Palette.SECTION_BORDER);
        GuiScreen.drawRect(x, y, x + w, y + 18, 0x90181828);
        GuiScreen.drawRect(x + 1, y + 1, x + 4, y + h - 1, Palette.ACCENT_BLUE);
        drawString(fontRenderer, title, x + 10, y + 5, Palette.TEXT_GOLD);
    }

    private void drawRow(String label, String value, int x, int valueX, int y) {
        drawString(fontRenderer, label, x, y + 5, Palette.TEXT_MUTED);
        drawCenteredString(fontRenderer, value, valueX, y + 5, Palette.TEXT_LIGHT);
    }

    private void drawPreviewCard(int x, int y, int w, int h) {
        GuiScreen.drawRect(x, y, x + w, y + h, 0xAA101018);
        drawPanelBorder(x, y, w, h, Palette.INFO_BOX_BORDER);
        drawString(fontRenderer, "HUD preview", x + 10, y + 10, Palette.TEXT_GOLD);
        drawString(fontRenderer, UltPlaceClientState112.isEnabled() ? "Mode active" : "Mode inactive",
                x + 10, y + 28, UltPlaceClientState112.isEnabled() ? Palette.TEXT_GREEN : Palette.TEXT_MUTED);
        drawString(fontRenderer, fit(UltPlaceClientState112.summary(), w - 20), x + 10, y + 46,
                Palette.TEXT_LIGHT);
        drawString(fontRenderer, "Tab cycles shape", x + 10, y + h - 28, Palette.TEXT_MUTED);
        drawString(fontRenderer, "K opens this screen", x + 10, y + h - 16, Palette.TEXT_MUTED);
    }

    private String variantText() {
        if (!UltPlaceClientState112.getSelectedShape().supportsVariant()) {
            return "Default";
        }
        return UltPlaceClientState112.getVariant() == 1 ? "Down" : "Up";
    }

    private String fit(String text, int maxWidth) {
        String out = text == null ? "" : text;
        while (out.length() > 3 && fontRenderer.getStringWidth(out) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
