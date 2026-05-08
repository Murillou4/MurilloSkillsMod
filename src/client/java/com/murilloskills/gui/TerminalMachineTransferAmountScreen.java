package com.murilloskills.gui;

import com.murilloskills.client.config.UltmineClientConfig;
import com.murilloskills.network.TerminalMachineTransferC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

public class TerminalMachineTransferAmountScreen extends Screen {
    private static final int MAX_AMOUNT = 4096;
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 150;

    private final Screen parent;
    private final ItemStack itemKey;
    private final long available;
    private final BlockPos targetPos;
    private final Direction face;

    private TextFieldWidget amountField;
    private Text error = Text.empty();

    public TerminalMachineTransferAmountScreen(Screen parent, ItemStack itemKey, long available, BlockPos targetPos,
            Direction face) {
        super(Text.translatable("murilloskills.terminal_transfer.title"));
        this.parent = parent;
        this.itemKey = itemKey.copy();
        this.itemKey.setCount(1);
        this.available = Math.max(1L, available);
        this.targetPos = targetPos;
        this.face = face;
    }

    @Override
    protected void init() {
        super.init();
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int inputY = panelY + 70;

        amountField = new TextFieldWidget(textRenderer, panelX + 18, inputY, PANEL_WIDTH - 36, 20,
                Text.translatable("murilloskills.terminal_transfer.amount"));
        amountField.setMaxLength(6);
        amountField.setText(Integer.toString(defaultAmount()));
        this.addDrawableChild(amountField);
        this.setInitialFocus(amountField);

        int quickY = inputY + 28;
        int quickW = 56;
        addAmountButton("1", panelX + 18, quickY, quickW, 1);
        addAmountButton("64", panelX + 82, quickY, quickW, 64);
        addAmountButton("256", panelX + 146, quickY, quickW, 256);
        addAmountButton(Text.translatable("murilloskills.terminal_transfer.all_short").getString(), panelX + 210,
                quickY, 72, availableAmount());

        int buttonY = panelY + PANEL_HEIGHT - 28;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("murilloskills.terminal_transfer.send")
                .formatted(Formatting.GREEN), button -> send())
                .dimensions(panelX + 54, buttonY, 86, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(panelX + 160, buttonY, 86, 20)
                .build());
    }

    private void addAmountButton(String label, int x, int y, int width, int amount) {
        this.addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> amountField.setText(Integer.toString(amount)))
                .dimensions(x, y, width, 18)
                .build());
    }

    private int defaultAmount() {
        return Math.min(UltmineClientConfig.getTerminalMachineTransferAmount(), availableAmount());
    }

    private int availableAmount() {
        return (int) Math.max(1L, Math.min(available, MAX_AMOUNT));
    }

    private void send() {
        int amount = parseAmount();
        if (amount <= 0) {
            error = Text.translatable("murilloskills.terminal_transfer.invalid_amount").formatted(Formatting.RED);
            return;
        }
        UltmineClientConfig.setTerminalMachineTransferAmount(amount);
        UltmineClientConfig.save();
        ClientPlayNetworking.send(new TerminalMachineTransferC2SPayload(itemKey.copy(), amount, targetPos, face));
        close();
    }

    private int parseAmount() {
        String raw = amountField.getText().trim();
        if (raw.isEmpty()) {
            return -1;
        }
        try {
            int amount = Integer.parseInt(raw);
            return Math.max(1, Math.min(amount, availableAmount()));
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
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xF0121722);
        drawBorder(context, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF4A90E2);
        context.drawCenteredTextWithShadow(textRenderer, title.copy().formatted(Formatting.AQUA, Formatting.BOLD),
                this.width / 2, panelY + 12, 0xFFFFFFFF);

        context.drawItem(itemKey, panelX + 18, panelY + 34);
        context.drawTextWithShadow(textRenderer, itemKey.getName(), panelX + 40, panelY + 36, 0xFFE6EDF3);
        context.drawTextWithShadow(textRenderer,
                Text.translatable("murilloskills.terminal_transfer.target",
                        targetPos.getX(), targetPos.getY(), targetPos.getZ()),
                panelX + 18, panelY + 56, 0xFF9FB0C3);
        super.render(context, mouseX, mouseY, delta);
        if (!error.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, error, this.width / 2, panelY + PANEL_HEIGHT - 48,
                    0xFFFF6B6B);
        }
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }
}
