package com.murilloskills.gui;

import com.murilloskills.gui.renderer.RenderingHelper;
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

    // === PREMIUM COLOR PALETTE (consistent with ModInfoScreen) ===
    private static final ColorPalette PALETTE = ColorPalette.premium();

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
        // 1. Background gradient with vignette (same as ModInfoScreen)
        renderGradientBackground(context);

        // 2. Modal outer glow/shadow
        int glowSize = 4;
        for (int i = glowSize; i > 0; i--) {
            int alpha = (int) (0x15 * ((float) i / glowSize));
            int glowColor = (alpha << 24) | 0x000000;
            context.fill(modalX - i, modalY - i, modalX + modalWidth + i, modalY + modalHeight + i, glowColor);
        }

        // 3. Modal background with depth
        // Outer shadow
        context.fill(modalX - 1, modalY - 1, modalX + modalWidth + 1, modalY + modalHeight + 1, PALETTE.panelShadow());

        // Main background
        context.fill(modalX, modalY, modalX + modalWidth, modalY + modalHeight, PALETTE.panelBg());

        // Inner highlight (top edge)
        context.fill(modalX + 1, modalY + 1, modalX + modalWidth - 1, modalY + 2, PALETTE.panelHighlight());

        // 4. Border with RenderingHelper
        RenderingHelper.drawPanelBorder(context, modalX, modalY, modalWidth, modalHeight, PALETTE.sectionBorder());

        // 5. Accent border at top (warning color)
        context.fill(modalX, modalY, modalX + modalWidth, modalY + 2, PALETTE.textRed());

        // 6. Corner accents using RenderingHelper
        RenderingHelper.renderCornerAccents(context, modalX, modalY, modalWidth, modalHeight, 6, PALETTE.accentGold());

        // 7. Warning icon with subtle pulse animation
        long tick = MinecraftClient.getInstance().world != null ? MinecraftClient.getInstance().world.getTime() : 0;
        float pulse = 0.8f + 0.2f * (float) Math.sin(tick * 0.15);
        int pulseAlpha = (int) (255 * pulse);
        int warningColor = (pulseAlpha << 24) | 0xFFAA00;

        Text warningIcon = Text.translatable("murilloskills.gui.icon.warning");
        int iconWidth = this.textRenderer.getWidth(warningIcon);
        context.drawTextWithShadow(this.textRenderer, warningIcon,
                modalX + (modalWidth - iconWidth) / 2, modalY + 12, warningColor);

        // 8. Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                modalX + modalWidth / 2, modalY + 28, PALETTE.textGold());

        // 9. Separator line
        int separatorY = modalY + 42;
        RenderingHelper.renderDivider(context, modalX + 20, separatorY, modalWidth - 40, PALETTE.dividerColor());

        // 10. Message (wrapped if needed)
        int messageY = modalY + 50;
        int maxWidth = modalWidth - 30;

        String messageStr = this.message.getString();
        java.util.List<String> lines = wrapText(messageStr, maxWidth);

        for (String line : lines) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(line),
                    modalX + modalWidth / 2, messageY, PALETTE.textLight());
            messageY += 11;
        }

        // 11. Render widgets (buttons) - must be last for proper layering
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderGradientBackground(DrawContext context) {
        // Vertical gradient (same as ModInfoScreen)
        for (int y = 0; y < this.height; y++) {
            float ratio = (float) y / this.height;
            int r = (int) (8 + ratio * 4);
            int g = (int) (8 + ratio * 4);
            int b = (int) (16 + ratio * 8);
            int color = 0xF0000000 | (r << 16) | (g << 8) | b;
            context.fill(0, y, this.width, y + 1, color);
        }

        // Subtle vignette effect
        renderVignette(context);
    }

    private void renderVignette(DrawContext context) {
        int size = Math.min(this.width, this.height) / 3;
        // Corner darkening
        for (int i = 0; i < 6; i++) {
            int alpha = (int) (0x12 * (1 - (float) i / 6));
            if (alpha <= 0)
                continue;
            int color = alpha << 24;
            int offset = size * i / 6;
            // Top-left
            context.fill(0, 0, size - offset, size - offset, color);
            // Top-right
            context.fill(this.width - size + offset, 0, this.width, size - offset, color);
            // Bottom-left
            context.fill(0, this.height - size + offset, size - offset, this.height, color);
            // Bottom-right
            context.fill(this.width - size + offset, this.height - size + offset, this.width, this.height, color);
        }
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
