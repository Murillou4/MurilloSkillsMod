package com.murilloskills.gui;

import com.murilloskills.client.config.UltmineClientConfig;
import com.murilloskills.data.TerminalMachineTargetClientState;
import com.murilloskills.network.TerminalMachineTransferC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TerminalMachineTransferAmountScreen extends Screen {
    private static final int MAX_AMOUNT = 1_000_000;
    private static final int TARGET_PANEL_WIDTH = 460;
    private static final int PANEL_PADDING = 18;
    private static final int ROW_HEIGHT = 22;
    private static final int MIN_PANEL_HEIGHT = 300;
    private static final int MAX_PANEL_HEIGHT = 420;
    private static final int BOTTOM_CONTROLS_HEIGHT = 98;
    private static final int SCROLLBAR_WIDTH = 4;

    private final Screen parent;
    private final ItemStack itemKey;
    private final long available;
    private final Set<BlockPos> selectedTargets = new HashSet<>();

    private List<TerminalMachineTargetClientState.Target> targets;
    private int selectedIndex;
    private boolean initializedSelection;
    private int scrollOffset;
    private boolean draggingScrollbar;
    private double scrollbarDragGrabOffset;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int visibleRows;
    private int scrollbarTrackY;
    private int scrollbarTrackH;
    private int scrollbarThumbY;
    private int scrollbarThumbH;

    private TextFieldWidget amountField;
    private ButtonWidget sendButton;
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
        calculateLayout();

        int selectorY = panelY + 84;
        int selectorW = 82;
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("murilloskills.terminal_transfer.select_all"),
                button -> {
                    selectAllTargets();
                    updateSendButton();
                })
                .dimensions(panelX + PANEL_PADDING, selectorY, selectorW, 18)
                .build());
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("murilloskills.terminal_transfer.select_none"),
                button -> {
                    selectedTargets.clear();
                    error = Text.empty();
                    updateSendButton();
                })
                .dimensions(panelX + PANEL_PADDING + selectorW + 8, selectorY, selectorW, 18)
                .build());
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("murilloskills.terminal_transfer.select_current"),
                button -> {
                    selectedTargets.clear();
                    TerminalMachineTargetClientState.Target target = selectedTarget();
                    if (target != null) {
                        selectedTargets.add(target.pos());
                    }
                    error = Text.empty();
                    updateSendButton();
                })
                .dimensions(panelX + PANEL_PADDING + (selectorW + 8) * 2, selectorY, 104, 18)
                .build());

        int amountY = panelY + panelH - BOTTOM_CONTROLS_HEIGHT + 6;
        int amountX = panelX + 126;
        amountField = new TextFieldWidget(textRenderer, amountX, amountY, panelX + panelW - PANEL_PADDING - amountX,
                20, Text.translatable("murilloskills.terminal_transfer.amount"));
        amountField.setMaxLength(7);
        amountField.setText(amountText == null ? Integer.toString(defaultAmount()) : amountText);
        amountField.setChangedListener(text -> amountText = text);
        this.addDrawableChild(amountField);
        this.setInitialFocus(amountField);

        int quickY = amountY + 28;
        int quickGap = 8;
        int quickW = (panelW - PANEL_PADDING * 2 - quickGap * 4) / 5;
        int quickX = panelX + PANEL_PADDING;
        addAmountButton("1", quickX, quickY, quickW, 1);
        addAmountButton("64", quickX + (quickW + quickGap), quickY, quickW, 64);
        addAmountButton("4096", quickX + (quickW + quickGap) * 2, quickY, quickW, 4096);
        addAmountButton("20k", quickX + (quickW + quickGap) * 3, quickY, quickW, 20000);
        addAmountButton(Text.translatable("murilloskills.terminal_transfer.all_short").getString(),
                quickX + (quickW + quickGap) * 4, quickY, quickW, availableAmount());

        int buttonY = panelY + panelH - 28;
        int sendW = 134;
        int cancelW = 96;
        int buttonsX = panelX + (panelW - sendW - cancelW - 14) / 2;
        sendButton = ButtonWidget.builder(sendButtonText(), button -> send())
                .dimensions(buttonsX, buttonY, sendW, 20)
                .build();
        this.addDrawableChild(sendButton);
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(buttonsX + sendW + 14, buttonY, cancelW, 20)
                .build());
        updateSendButton();
    }

    private void calculateLayout() {
        panelW = Math.min(TARGET_PANEL_WIDTH, Math.max(340, this.width - 28));
        panelH = Math.min(MAX_PANEL_HEIGHT, this.height - 24);
        if (panelH < MIN_PANEL_HEIGHT) {
            panelH = Math.max(220, this.height - 12);
        }
        panelX = (this.width - panelW) / 2;
        panelY = Math.max(6, (this.height - panelH) / 2);

        listX = panelX + PANEL_PADDING;
        listY = panelY + 108;
        int listBottom = panelY + panelH - BOTTOM_CONTROLS_HEIGHT - 8;
        listW = panelW - PANEL_PADDING * 2;
        listH = Math.max(ROW_HEIGHT * 3, listBottom - listY);
        visibleRows = Math.max(1, listH / ROW_HEIGHT);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll()));
        updateScrollbarGeometry();
    }

    private void addAmountButton(String label, int x, int y, int width, int amount) {
        this.addDrawableChild(ButtonWidget.builder(Text.literal(label),
                button -> {
                    amountField.setText(Integer.toString(amount));
                    error = Text.empty();
                })
                .dimensions(x, y, width, 18)
                .build());
    }

    private void refreshTargets() {
        targets = TerminalMachineTargetClientState.getTargetsSnapshot();
        selectedIndex = TerminalMachineTargetClientState.getSelectedIndex();
        if (selectedIndex < 0 && !targets.isEmpty()) {
            selectedIndex = 0;
        }
        selectedTargets.removeIf(pos -> targets.stream().noneMatch(target -> target.pos().equals(pos)));
        if (!initializedSelection) {
            TerminalMachineTargetClientState.Target target = selectedTarget();
            if (target != null) {
                selectedTargets.add(target.pos());
            }
            initializedSelection = true;
        }
    }

    private TerminalMachineTargetClientState.Target selectedTarget() {
        if (selectedIndex < 0 || selectedIndex >= targets.size()) {
            return null;
        }
        return targets.get(selectedIndex);
    }

    private void selectAllTargets() {
        selectedTargets.clear();
        for (TerminalMachineTargetClientState.Target target : targets) {
            selectedTargets.add(target.pos());
        }
        error = Text.empty();
    }

    private List<IndexedTarget> selectedTargetsOrdered() {
        List<IndexedTarget> selected = new ArrayList<>();
        for (int i = 0; i < targets.size(); i++) {
            TerminalMachineTargetClientState.Target target = targets.get(i);
            if (selectedTargets.contains(target.pos())) {
                selected.add(new IndexedTarget(i, target));
            }
        }
        return selected;
    }

    private String targetLabel(int index, TerminalMachineTargetClientState.Target target) {
        return target.name() + " " + target.pos().getX() + " " + target.pos().getY() + " "
                + target.pos().getZ() + " (" + (index + 1) + ")";
    }

    private int defaultAmount() {
        return Math.min(UltmineClientConfig.getTerminalMachineTransferAmount(), availableAmount());
    }

    private int availableAmount() {
        return (int) Math.max(1L, Math.min(available, MAX_AMOUNT));
    }

    private void send() {
        List<IndexedTarget> selected = selectedTargetsOrdered();
        if (selected.isEmpty()) {
            error = Text.translatable("murilloskills.terminal_transfer.no_selected_targets").formatted(Formatting.RED);
            updateSendButton();
            return;
        }
        int amount = parseAmount();
        if (amount <= 0) {
            error = Text.translatable("murilloskills.terminal_transfer.invalid_amount").formatted(Formatting.RED);
            return;
        }
        UltmineClientConfig.setTerminalMachineTransferAmount(amount);
        UltmineClientConfig.save();
        for (IndexedTarget indexedTarget : selected) {
            TerminalMachineTargetClientState.Target target = indexedTarget.target();
            ClientPlayNetworking.send(new TerminalMachineTransferC2SPayload(itemKey.copy(), amount, target.pos(),
                    target.face()));
        }
        IndexedTarget lastTarget = selected.get(selected.size() - 1);
        selectedIndex = lastTarget.index();
        TerminalMachineTargetClientState.select(selectedIndex);
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
            return Math.min(amount, availableAmount());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private Text sendButtonText() {
        return Text.translatable("murilloskills.terminal_transfer.send_selected", selectedTargets.size())
                .formatted(Formatting.GREEN);
    }

    private void updateSendButton() {
        if (sendButton != null) {
            sendButton.setMessage(sendButtonText());
            sendButton.active = !selectedTargets.isEmpty();
        }
    }

    private int maxScroll() {
        return Math.max(0, targets.size() - visibleRows);
    }

    private void updateScrollbarGeometry() {
        int maxScroll = maxScroll();
        scrollbarTrackY = listY + 2;
        scrollbarTrackH = Math.max(0, visibleRows * ROW_HEIGHT - 4);
        if (maxScroll <= 0 || targets.isEmpty()) {
            scrollbarThumbY = scrollbarTrackY;
            scrollbarThumbH = scrollbarTrackH;
            return;
        }
        scrollbarThumbH = Math.max(16, scrollbarTrackH * visibleRows / targets.size());
        scrollbarThumbY = scrollbarTrackY
                + (int) ((double) scrollOffset / maxScroll * Math.max(0, scrollbarTrackH - scrollbarThumbH));
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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            int next = scrollOffset + (verticalAmount > 0 ? -3 : 3);
            int clamped = Math.max(0, Math.min(maxScroll(), next));
            if (clamped != scrollOffset) {
                scrollOffset = clamped;
                updateScrollbarGeometry();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() == 0 && maxScroll() > 0) {
            int barX = listX + listW - SCROLLBAR_WIDTH - 2;
            if (mouseX >= barX - 3 && mouseX <= barX + SCROLLBAR_WIDTH + 3
                    && mouseY >= scrollbarTrackY && mouseY <= scrollbarTrackY + scrollbarTrackH) {
                draggingScrollbar = true;
                scrollbarDragGrabOffset = mouseY >= scrollbarThumbY && mouseY <= scrollbarThumbY + scrollbarThumbH
                        ? mouseY - scrollbarThumbY
                        : scrollbarThumbH / 2.0;
                updateScrollFromMouse(mouseY);
                return true;
            }
        }
        if (click.button() == 0 && mouseX >= listX && mouseX <= listX + listW
                && mouseY >= listY && mouseY <= listY + visibleRows * ROW_HEIGHT) {
            int row = (int) ((mouseY - listY) / ROW_HEIGHT);
            int index = scrollOffset + row;
            if (index >= 0 && index < targets.size()) {
                TerminalMachineTargetClientState.Target target = targets.get(index);
                if (selectedTargets.contains(target.pos())) {
                    selectedTargets.remove(target.pos());
                } else {
                    selectedTargets.add(target.pos());
                }
                selectedIndex = index;
                TerminalMachineTargetClientState.select(index);
                error = Text.empty();
                updateSendButton();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (draggingScrollbar && click.button() == 0) {
            updateScrollFromMouse(click.y());
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingScrollbar && click.button() == 0) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    private void updateScrollFromMouse(double mouseY) {
        int maxScroll = maxScroll();
        if (maxScroll <= 0 || scrollbarTrackH <= scrollbarThumbH) {
            scrollOffset = 0;
            updateScrollbarGeometry();
            return;
        }
        double thumbTop = mouseY - scrollbarDragGrabOffset;
        double ratio = (thumbTop - scrollbarTrackY) / (scrollbarTrackH - scrollbarThumbH);
        scrollOffset = Math.max(0, Math.min(maxScroll, (int) Math.round(ratio * maxScroll)));
        updateScrollbarGeometry();
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
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF0121722);
        drawBorder(context, panelX, panelY, panelW, panelH, 0xFF4A90E2);

        context.drawCenteredTextWithShadow(textRenderer, title.copy().formatted(Formatting.AQUA, Formatting.BOLD),
                this.width / 2, panelY + 12, 0xFFFFFFFF);

        context.drawItem(itemKey, panelX + PANEL_PADDING, panelY + 34);
        context.drawTextWithShadow(textRenderer, trimmed(itemKey.getName().getString(), panelW - 62),
                panelX + 40, panelY + 36, 0xFFE6EDF3);

        TerminalMachineTargetClientState.Target target = selectedTarget();
        Text selectedTargetText = target == null
                ? Text.translatable("murilloskills.terminal_transfer.no_target")
                : Text.translatable("murilloskills.terminal_transfer.selected_target",
                        target.name(), target.pos().getX(), target.pos().getY(), target.pos().getZ(),
                        selectedIndex + 1, targets.size());
        context.drawTextWithShadow(textRenderer, trimmed(selectedTargetText.getString(), panelW - PANEL_PADDING * 2),
                panelX + PANEL_PADDING, panelY + 56, 0xFF9FB0C3);

        context.drawTextWithShadow(textRenderer,
                Text.translatable("murilloskills.terminal_transfer.selected_targets",
                        selectedTargets.size(), targets.size()),
                panelX + PANEL_PADDING, panelY + 72, 0xFF9FB0C3);

        renderTargetList(context, mouseX, mouseY);

        int amountY = panelY + panelH - BOTTOM_CONTROLS_HEIGHT + 11;
        context.drawTextWithShadow(textRenderer,
                Text.translatable("murilloskills.terminal_transfer.amount_per_machine"),
                panelX + PANEL_PADDING, amountY, 0xFFE6EDF3);

        if (!error.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, error, this.width / 2,
                    panelY + panelH - BOTTOM_CONTROLS_HEIGHT - 18, 0xFFFF6B6B);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderTargetList(DrawContext context, int mouseX, int mouseY) {
        int listBottom = listY + visibleRows * ROW_HEIGHT;
        context.fill(listX, listY, listX + listW, listBottom, 0xD0080B12);
        drawBorder(context, listX, listY, listW, listBottom - listY, 0xFF243649);

        int start = scrollOffset;
        int end = Math.min(targets.size(), start + visibleRows);
        for (int i = start; i < end; i++) {
            int rowY = listY + (i - start) * ROW_HEIGHT;
            TerminalMachineTargetClientState.Target target = targets.get(i);
            boolean checked = selectedTargets.contains(target.pos());
            boolean current = i == selectedIndex;
            boolean hovered = mouseX >= listX && mouseX <= listX + listW - 8
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

            int bg = current ? 0xEE15314A : checked ? 0xCC102232 : hovered ? 0xAA1A2432 : 0x66101822;
            context.fill(listX + 1, rowY + 1, listX + listW - 6, rowY + ROW_HEIGHT - 1, bg);
            if (hovered || current) {
                drawBorder(context, listX + 1, rowY + 1, listW - 7, ROW_HEIGHT - 2,
                        current ? 0xFF45D9FF : 0xFF49657F);
            }

            int checkboxX = listX + 8;
            int checkboxY = rowY + 6;
            context.fill(checkboxX, checkboxY, checkboxX + 10, checkboxY + 10, checked ? 0xFF2DA44E : 0xFF0F1720);
            drawBorder(context, checkboxX, checkboxY, 10, 10, checked ? 0xFF7EE787 : 0xFF6B7785);
            if (checked) {
                context.drawTextWithShadow(textRenderer, "x", checkboxX + 2, checkboxY + 1, 0xFFFFFFFF);
            }

            int textColor = checked ? 0xFFE6EDF3 : 0xFF9FB0C3;
            String label = trimmed(targetLabel(i, target), listW - 54);
            context.drawTextWithShadow(textRenderer, label, listX + 26, rowY + 7, textColor);
        }

        int maxScroll = maxScroll();
        if (maxScroll > 0) {
            int barX = listX + listW - SCROLLBAR_WIDTH - 2;
            context.fill(barX, scrollbarTrackY, barX + SCROLLBAR_WIDTH, scrollbarTrackY + scrollbarTrackH,
                    0xFF17202B);
            context.fill(barX, scrollbarThumbY, barX + SCROLLBAR_WIDTH, scrollbarThumbY + scrollbarThumbH,
                    0xFF58A6FF);
        }
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

    private record IndexedTarget(int index, TerminalMachineTargetClientState.Target target) {
    }
}
