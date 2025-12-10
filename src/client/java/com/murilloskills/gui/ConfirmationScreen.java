package com.murilloskills.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * A reusable confirmation modal screen.
 * Displays a warning with confirm/cancel buttons before executing an action.
 */
public class ConfirmationScreen extends Screen {

    // Premium Colors (matching SkillsScreen)
    private static final int BG_OVERLAY = 0xE0101018;
    private static final int MODAL_BG = 0xFF1A1A24;
    private static final int BORDER_COLOR = 0xFFFFCC44;
    private static final int TITLE_COLOR = 0xFFFFAA00;
    private static final int MESSAGE_COLOR = 0xFFDDDDDD;

    private final Screen parent;
    private final Text title;
    private final Text message;
    private final Runnable onConfirm;

    // Modal dimensions
    private static final int MODAL_WIDTH = 280;
    private static final int MODAL_HEIGHT = 120;

    public ConfirmationScreen(Screen parent, Text title, Text message, Runnable onConfirm) {
        super(title);
        this.parent = parent;
        this.title = title;
        this.message = message;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        super.init();

        int modalX = (this.width - MODAL_WIDTH) / 2;
        int modalY = (this.height - MODAL_HEIGHT) / 2;

        // Confirm button
        int btnWidth = 100;
        int btnHeight = 20;
        int btnY = modalY + MODAL_HEIGHT - 35;

        ButtonWidget confirmBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.confirm.btn_confirm"),
                (button) -> {
                    onConfirm.run();
                    this.close();
                })
                .dimensions(modalX + 25, btnY, btnWidth, btnHeight)
                .build();

        // Cancel button
        ButtonWidget cancelBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.confirm.btn_cancel"),
                (button) -> this.close())
                .dimensions(modalX + MODAL_WIDTH - btnWidth - 25, btnY, btnWidth, btnHeight)
                .build();

        this.addDrawableChild(confirmBtn);
        this.addDrawableChild(cancelBtn);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark overlay background
        context.fill(0, 0, this.width, this.height, BG_OVERLAY);

        int modalX = (this.width - MODAL_WIDTH) / 2;
        int modalY = (this.height - MODAL_HEIGHT) / 2;

        // Modal background
        context.fill(modalX, modalY, modalX + MODAL_WIDTH, modalY + MODAL_HEIGHT, MODAL_BG);

        // Modal border
        drawBorder(context, modalX, modalY, MODAL_WIDTH, MODAL_HEIGHT, BORDER_COLOR);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, modalY + 15, TITLE_COLOR);

        // Message (wrapped if needed)
        int messageY = modalY + 40;
        int maxWidth = MODAL_WIDTH - 30;

        // Simple word wrap for the message
        String messageStr = this.message.getString();
        java.util.List<String> lines = wrapText(messageStr, maxWidth);

        for (String line : lines) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(line), this.width / 2, messageY,
                    MESSAGE_COLOR);
            messageY += 12;
        }

        // Render widgets (buttons)
        super.render(context, mouseX, mouseY, delta);
    }

    private java.util.List<String> wrapText(String text, int maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            if (this.textRenderer.getWidth(testLine) <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(this.parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
