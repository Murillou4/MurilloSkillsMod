package com.murilloskills.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * A modern, elegant confirmation modal screen.
 * Displays a warning with styled confirm/cancel buttons before executing an
 * action.
 */
public class ConfirmationScreen extends Screen {

    // === MODERN PREMIUM COLOR PALETTE ===
    private static final int BG_OVERLAY = 0xE8000008;

    // Modal Colors
    private static final int MODAL_BG = 0xF8101018;
    private static final int MODAL_INNER_SHADOW = 0x40000000;
    private static final int MODAL_HIGHLIGHT = 0x15FFFFFF;

    // Border & Accent
    private static final int BORDER_OUTER = 0xFF1A1A25;
    private static final int BORDER_ACCENT = 0xFFDD8800;
    private static final int BORDER_GLOW = 0x40FFAA00;

    // Text Colors
    private static final int TITLE_COLOR = 0xFFFFCC44;
    private static final int MESSAGE_COLOR = 0xFFCCCCDD;
    private static final int WARNING_ICON_COLOR = 0xFFFFAA00;

    private final Screen parent;
    private final Text title;
    private final Text message;
    private final Runnable onConfirm;

    // Responsive modal dimensions
    private int modalWidth;
    private int modalHeight;
    private int modalX;
    private int modalY;

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

        // Responsive modal sizing
        modalWidth = Math.min(320, Math.max(250, this.width / 3));
        modalHeight = Math.min(150, Math.max(120, this.height / 4));
        modalX = (this.width - modalWidth) / 2;
        modalY = (this.height - modalHeight) / 2;

        // Button dimensions - responsive
        int btnWidth = Math.min(100, (modalWidth - 50) / 2);
        int btnHeight = 20;
        int btnY = modalY + modalHeight - 32;
        int btnSpacing = 15;

        // Cancel button (left) - neutral styling
        ButtonWidget cancelBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.confirm.btn_cancel"),
                (button) -> this.close())
                .dimensions(modalX + (modalWidth / 2) - btnWidth - btnSpacing / 2, btnY, btnWidth, btnHeight)
                .build();

        // Confirm button (right) - action styling
        ButtonWidget confirmBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.confirm.btn_confirm"),
                (button) -> {
                    onConfirm.run();
                    this.close();
                })
                .dimensions(modalX + (modalWidth / 2) + btnSpacing / 2, btnY, btnWidth, btnHeight)
                .build();

        this.addDrawableChild(cancelBtn);
        this.addDrawableChild(confirmBtn);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. Dark overlay with fade effect
        context.fill(0, 0, this.width, this.height, BG_OVERLAY);

        // 2. Modal outer glow/shadow
        int glowSize = 4;
        for (int i = glowSize; i > 0; i--) {
            int alpha = (int) (0x15 * ((float) i / glowSize));
            int glowColor = (alpha << 24) | 0x000000;
            context.fill(modalX - i, modalY - i, modalX + modalWidth + i, modalY + modalHeight + i, glowColor);
        }

        // 3. Modal background with depth
        // Outer border
        context.fill(modalX - 1, modalY - 1, modalX + modalWidth + 1, modalY + modalHeight + 1, BORDER_OUTER);

        // Main background
        context.fill(modalX, modalY, modalX + modalWidth, modalY + modalHeight, MODAL_BG);

        // Inner highlight (top edge)
        context.fill(modalX + 1, modalY + 1, modalX + modalWidth - 1, modalY + 2, MODAL_HIGHLIGHT);

        // Inner shadow (bottom)
        context.fill(modalX + 1, modalY + modalHeight - 2, modalX + modalWidth - 1, modalY + modalHeight - 1,
                MODAL_INNER_SHADOW);

        // 4. Accent border at top
        context.fill(modalX, modalY, modalX + modalWidth, modalY + 2, BORDER_ACCENT);

        // Corner accents
        int cornerSize = 6;
        // Top-left corner
        context.fill(modalX, modalY, modalX + cornerSize, modalY + 2, BORDER_GLOW);
        context.fill(modalX, modalY, modalX + 2, modalY + cornerSize, BORDER_GLOW);
        // Top-right corner
        context.fill(modalX + modalWidth - cornerSize, modalY, modalX + modalWidth, modalY + 2, BORDER_GLOW);
        context.fill(modalX + modalWidth - 2, modalY, modalX + modalWidth, modalY + cornerSize, BORDER_GLOW);

        // 5. Warning icon
        Text warningIcon = Text.translatable("murilloskills.gui.icon.warning");
        int iconWidth = this.textRenderer.getWidth(warningIcon);
        context.drawTextWithShadow(this.textRenderer, warningIcon,
                modalX + (modalWidth - iconWidth) / 2, modalY + 12, WARNING_ICON_COLOR);

        // 6. Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                modalX + modalWidth / 2, modalY + 28, TITLE_COLOR);

        // 7. Separator line
        int separatorY = modalY + 42;
        context.fill(modalX + 20, separatorY, modalX + modalWidth - 20, separatorY + 1, 0x30FFFFFF);

        // 8. Message (wrapped if needed)
        int messageY = modalY + 50;
        int maxWidth = modalWidth - 30;

        String messageStr = this.message.getString();
        java.util.List<String> lines = wrapText(messageStr, maxWidth);

        for (String line : lines) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(line),
                    modalX + modalWidth / 2, messageY, MESSAGE_COLOR);
            messageY += 11;
        }

        // 9. Render widgets (buttons) - must be last for proper layering
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

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(this.parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
