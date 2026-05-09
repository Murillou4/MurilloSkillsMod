package com.murilloskills.gui;

import com.murilloskills.client.config.UltmineClientConfig;
import com.murilloskills.network.TerminalBulkCraftC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class TerminalBulkCraftAmountScreen extends Screen {
    private static final int MAX_AMOUNT = 100_000;
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 174;
    private static final int PANEL_PADDING = 18;

    private final Screen parent;
    private final ItemStack resultKey;

    private TextFieldWidget amountField;
    private Text error = Text.empty();
    private String amountText;
    private int panelX;
    private int panelY;

    public TerminalBulkCraftAmountScreen(Screen parent, ItemStack resultKey) {
        super(Text.translatable("murilloskills.terminal_bulk_craft.title"));
        this.parent = parent;
        this.resultKey = resultKey.copy();
        this.resultKey.setCount(1);
    }

    @Override
    protected void init() {
        super.init();
        panelX = (this.width - PANEL_WIDTH) / 2;
        panelY = Math.max(10, (this.height - PANEL_HEIGHT) / 2);

        int amountY = panelY + 78;
        amountField = new TextFieldWidget(textRenderer, panelX + 112, amountY,
                PANEL_WIDTH - PANEL_PADDING * 2 - 94, 20,
                Text.translatable("murilloskills.terminal_bulk_craft.amount"));
        amountField.setMaxLength(6);
        amountField.setText(amountText == null
                ? Integer.toString(UltmineClientConfig.getTerminalBulkCraftAmount())
                : amountText);
        amountField.setChangedListener(text -> amountText = text);
        this.addDrawableChild(amountField);
        this.setInitialFocus(amountField);

        int quickY = amountY + 30;
        int quickGap = 7;
        int quickW = (PANEL_WIDTH - PANEL_PADDING * 2 - quickGap * 4) / 5;
        int quickX = panelX + PANEL_PADDING;
        addAmountButton("1", quickX, quickY, quickW, 1);
        addAmountButton("64", quickX + (quickW + quickGap), quickY, quickW, 64);
        addAmountButton("4096", quickX + (quickW + quickGap) * 2, quickY, quickW, 4096);
        addAmountButton("20k", quickX + (quickW + quickGap) * 3, quickY, quickW, 20000);
        addAmountButton("100k", quickX + (quickW + quickGap) * 4, quickY, quickW, MAX_AMOUNT);

        int buttonY = panelY + PANEL_HEIGHT - 32;
        int craftW = 124;
        int cancelW = 88;
        int buttonsX = panelX + (PANEL_WIDTH - craftW - cancelW - 12) / 2;
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("murilloskills.terminal_bulk_craft.craft").formatted(Formatting.GREEN),
                button -> send())
                .dimensions(buttonsX, buttonY, craftW, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(buttonsX + craftW + 12, buttonY, cancelW, 20)
                .build());
    }

    private void addAmountButton(String label, int x, int y, int width, int amount) {
        this.addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> {
            amountField.setText(Integer.toString(amount));
            error = Text.empty();
        })
                .dimensions(x, y, width, 18)
                .build());
    }

    private void send() {
        int amount = parseAmount();
        if (amount <= 0) {
            error = Text.translatable("murilloskills.terminal_bulk_craft.invalid_amount").formatted(Formatting.RED);
            return;
        }
        UltmineClientConfig.setTerminalBulkCraftAmount(amount);
        UltmineClientConfig.save();
        ClientPlayNetworking.send(new TerminalBulkCraftC2SPayload(amount));
        close();
    }

    private int parseAmount() {
        String raw = amountField.getText().trim();
        if (raw.isEmpty()) {
            return -1;
        }
        try {
            int amount = Integer.parseInt(raw);
            if (amount <= 0) {
                return -1;
            }
            return Math.min(amount, MAX_AMOUNT);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int key = keyInput.key();
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            send();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xAA05070D);
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xF0121722);
        drawBorder(context, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF58A6FF);

        context.drawCenteredTextWithShadow(textRenderer,
                title.copy().formatted(Formatting.AQUA, Formatting.BOLD),
                this.width / 2, panelY + 14, 0xFFFFFFFF);

        context.drawItem(resultKey, panelX + PANEL_PADDING, panelY + 42);
        context.drawTextWithShadow(textRenderer, trimmed(resultKey.getName().getString(), PANEL_WIDTH - 62),
                panelX + 42, panelY + 45, 0xFFE6EDF3);

        context.drawTextWithShadow(textRenderer,
                Text.translatable("murilloskills.terminal_bulk_craft.amount"),
                panelX + PANEL_PADDING, panelY + 83, 0xFFE6EDF3);

        if (!error.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, error, this.width / 2,
                    panelY + PANEL_HEIGHT - 52, 0xFFFF6B6B);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private String trimmed(String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String result = text;
        while (result.length() > 1 && textRenderer.getWidth(result + "..") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "..";
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }
}
