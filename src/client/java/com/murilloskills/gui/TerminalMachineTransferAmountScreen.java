package com.murilloskills.gui;

import com.murilloskills.client.config.UltmineClientConfig;
import com.murilloskills.data.TerminalMachineTargetClientState;
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
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class TerminalMachineTransferAmountScreen extends Screen {
    private static final int MAX_AMOUNT = 1_000_000;
    private static final int PANEL_WIDTH = 340;
    private static final int BASE_PANEL_HEIGHT = 172;
    private static final int MAX_VISIBLE_TARGETS = 6;

    private final Screen parent;
    private final ItemStack itemKey;
    private final long available;
    private List<TerminalMachineTargetClientState.Target> targets;
    private int selectedIndex;
    private boolean targetsExpanded;

    private TextFieldWidget amountField;
    private String amountText;
    private Text error = Text.empty();

    public TerminalMachineTransferAmountScreen(Screen parent, ItemStack itemKey, long available) {
        super(Text.translatable("murilloskills.terminal_transfer.title"));
        this.parent = parent;
        this.itemKey = itemKey.copy();
        this.itemKey.setCount(1);
        this.available = Math.max(1L, available);
        refreshTargets();
    }

    @Override
    protected void init() {
        super.init();
        refreshTargets();
        int panelHeight = panelHeight();
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - panelHeight) / 2;
        int targetY = panelY + 68;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("murilloskills.terminal_transfer.targets",
                targets.size()), button -> {
                    amountText = amountField == null ? amountText : amountField.getText();
                    targetsExpanded = !targetsExpanded;
                    this.init();
                }).dimensions(panelX + 18, targetY, PANEL_WIDTH - 36, 20).build());

        int inputY = targetY + 28;
        if (targetsExpanded) {
            int visibleTargets = Math.min(targets.size(), MAX_VISIBLE_TARGETS);
            for (int i = 0; i < visibleTargets; i++) {
                int targetIndex = i;
                TerminalMachineTargetClientState.Target target = targets.get(i);
                this.addDrawableChild(ButtonWidget.builder(Text.literal(targetLabel(targetIndex, target)),
                        button -> {
                            selectedIndex = targetIndex;
                            TerminalMachineTargetClientState.select(targetIndex);
                            amountText = amountField == null ? amountText : amountField.getText();
                            targetsExpanded = false;
                            this.init();
                        }).dimensions(panelX + 18, inputY, PANEL_WIDTH - 36, 18).build());
                inputY += 22;
            }
            inputY += 4;
        }

        amountField = new TextFieldWidget(textRenderer, panelX + 18, inputY, PANEL_WIDTH - 36, 20,
                Text.translatable("murilloskills.terminal_transfer.amount"));
        amountField.setMaxLength(7);
        amountField.setText(amountText == null ? Integer.toString(defaultAmount()) : amountText);
        amountField.setChangedListener(text -> amountText = text);
        this.addDrawableChild(amountField);
        this.setInitialFocus(amountField);

        int quickY = inputY + 28;
        int quickW = 46;
        addAmountButton("1", panelX + 18, quickY, quickW, 1);
        addAmountButton("64", panelX + 70, quickY, quickW, 64);
        addAmountButton("4096", panelX + 122, quickY, quickW, 4096);
        addAmountButton("20k", panelX + 174, quickY, quickW, 20000);
        addAmountButton(Text.translatable("murilloskills.terminal_transfer.all_short").getString(), panelX + 226,
                quickY, 56, availableAmount());

        int buttonY = panelY + panelHeight - 28;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("murilloskills.terminal_transfer.send")
                .formatted(Formatting.GREEN), button -> send())
                .dimensions(panelX + 74, buttonY, 86, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(panelX + 180, buttonY, 86, 20)
                .build());
    }

    private void addAmountButton(String label, int x, int y, int width, int amount) {
        this.addDrawableChild(ButtonWidget.builder(Text.literal(label),
                button -> amountField.setText(Integer.toString(amount)))
                .dimensions(x, y, width, 18)
                .build());
    }

    private void refreshTargets() {
        targets = TerminalMachineTargetClientState.getTargetsSnapshot();
        selectedIndex = TerminalMachineTargetClientState.getSelectedIndex();
        if (selectedIndex < 0 && !targets.isEmpty()) {
            selectedIndex = 0;
        }
    }

    private int panelHeight() {
        int visibleTargets = targetsExpanded ? Math.min(targets.size(), MAX_VISIBLE_TARGETS) : 0;
        return BASE_PANEL_HEIGHT + visibleTargets * 22;
    }

    private TerminalMachineTargetClientState.Target selectedTarget() {
        if (selectedIndex < 0 || selectedIndex >= targets.size()) {
            return null;
        }
        return targets.get(selectedIndex);
    }

    private String targetLabel(int index, TerminalMachineTargetClientState.Target target) {
        String prefix = index == selectedIndex ? "> " : "";
        return prefix + target.name() + " " + target.pos().getX() + " " + target.pos().getY() + " "
                + target.pos().getZ();
    }

    private int defaultAmount() {
        return Math.min(UltmineClientConfig.getTerminalMachineTransferAmount(), availableAmount());
    }

    private int availableAmount() {
        return (int) Math.max(1L, Math.min(available, MAX_AMOUNT));
    }

    private void send() {
        TerminalMachineTargetClientState.Target target = selectedTarget();
        if (target == null) {
            error = Text.translatable("murilloskills.terminal_transfer.no_target").formatted(Formatting.RED);
            return;
        }
        int amount = parseAmount();
        if (amount <= 0) {
            error = Text.translatable("murilloskills.terminal_transfer.invalid_amount").formatted(Formatting.RED);
            return;
        }
        UltmineClientConfig.setTerminalMachineTransferAmount(amount);
        UltmineClientConfig.save();
        TerminalMachineTargetClientState.select(selectedIndex);
        ClientPlayNetworking.send(new TerminalMachineTransferC2SPayload(itemKey.copy(), amount, target.pos(),
                target.face()));
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
        int panelHeight = panelHeight();
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - panelHeight) / 2;
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, 0xF0121722);
        drawBorder(context, panelX, panelY, PANEL_WIDTH, panelHeight, 0xFF4A90E2);
        context.drawCenteredTextWithShadow(textRenderer, title.copy().formatted(Formatting.AQUA, Formatting.BOLD),
                this.width / 2, panelY + 12, 0xFFFFFFFF);

        context.drawItem(itemKey, panelX + 18, panelY + 34);
        context.drawTextWithShadow(textRenderer, itemKey.getName(), panelX + 40, panelY + 36, 0xFFE6EDF3);
        TerminalMachineTargetClientState.Target target = selectedTarget();
        Text selectedTargetText = target == null
                ? Text.translatable("murilloskills.terminal_transfer.no_target")
                : Text.translatable("murilloskills.terminal_transfer.selected_target",
                        target.name(), target.pos().getX(), target.pos().getY(), target.pos().getZ(),
                        selectedIndex + 1, targets.size());
        context.drawTextWithShadow(textRenderer,
                selectedTargetText, panelX + 18, panelY + 56, 0xFF9FB0C3);
        super.render(context, mouseX, mouseY, delta);
        if (!error.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, error, this.width / 2, panelY + panelHeight - 48,
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
