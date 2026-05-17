package com.murilloskills.gui;

import com.murilloskills.client.config.UltmineClientConfig;
import com.murilloskills.gui.renderer.RenderingHelper;
import com.murilloskills.network.UltmineClassicBlockListSyncC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Block picker for Ultmine classic-mode block lock list.
 * Clicking a block toggles it in the blocked list.
 */
public class UltmineClassicBlockPickerScreen extends Screen {

    private final Screen parent;
    private final ColorPalette palette = ColorPalette.premium();

    private static final int HEADER_HEIGHT = 50;
    private static final int PANEL_PADDING = 10;
    private static final int CELL_SIZE = 20;
    private static final int CELL_GAP = 2;

    private TextFieldWidget searchField;

    private int panelX, panelY, panelW, panelH;
    private int gridX, gridY, gridW, gridH;
    private int cols, rows;
    private int scrollOffset = 0;

    private final List<Block> allBlocks = new ArrayList<>();
    private List<Block> filtered = new ArrayList<>();
    private String lastQuery = "";

    public UltmineClassicBlockPickerScreen(Screen parent) {
        super(Text.translatable("murilloskills.classic_block_picker.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (allBlocks.isEmpty()) {
            for (Block block : Registries.BLOCK) {
                if (block == net.minecraft.block.Blocks.AIR) {
                    continue;
                }
                Item item = block.asItem();
                if (item == Items.AIR) {
                    continue;
                }
                allBlocks.add(block);
            }
            filtered = new ArrayList<>(allBlocks);
        }
        calculateLayout();

        int centerX = this.width / 2;

        int searchW = Math.min(260, panelW - PANEL_PADDING * 4);
        int searchX = centerX - searchW / 2;
        int searchY = panelY + HEADER_HEIGHT + 6;
        searchField = new TextFieldWidget(textRenderer, searchX, searchY, searchW, 18, Text.empty());
        searchField.setMaxLength(64);
        searchField.setText(lastQuery);
        searchField.setPlaceholder(Text.translatable("murilloskills.classic_block_picker.search_placeholder")
                .copy().formatted(Formatting.DARK_GRAY));
        searchField.setChangedListener(this::applyFilter);
        this.addDrawableChild(searchField);
        this.setInitialFocus(searchField);

        int btnW = 90;
        int btnGap = 12;
        int btnY = panelY + panelH - 28;

        ButtonWidget pickHeldBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.classic_block_picker.pick_held").formatted(Formatting.AQUA),
                (b) -> addHeldBlock())
                .dimensions(centerX - btnW - btnGap / 2 - btnW - btnGap, btnY, btnW * 2 + btnGap, 20)
                .build();
        this.addDrawableChild(pickHeldBtn);

        ButtonWidget doneBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.classic_block_picker.done").formatted(Formatting.GREEN),
                (b) -> close())
                .dimensions(centerX + btnGap / 2 + btnW + btnGap, btnY, btnW, 20)
                .build();
        this.addDrawableChild(doneBtn);
    }

    private void calculateLayout() {
        panelW = Math.min(420, this.width - 20);
        panelX = (this.width - panelW) / 2;
        panelY = 10;
        panelH = this.height - 20;

        gridX = panelX + PANEL_PADDING;
        gridY = panelY + HEADER_HEIGHT + 30;
        int gridBottom = panelY + panelH - 36;
        gridW = panelW - PANEL_PADDING * 2;
        gridH = gridBottom - gridY;

        cols = Math.max(1, (gridW + CELL_GAP) / (CELL_SIZE + CELL_GAP));
        rows = Math.max(1, (gridH + CELL_GAP) / (CELL_SIZE + CELL_GAP));
    }

    private void applyFilter(String query) {
        lastQuery = query == null ? "" : query;
        String q = lastQuery.trim().toLowerCase(Locale.ROOT);
        scrollOffset = 0;
        if (q.isEmpty()) {
            filtered = new ArrayList<>(allBlocks);
            return;
        }

        List<Block> result = new ArrayList<>();
        for (Block block : allBlocks) {
            Identifier id = Registries.BLOCK.getId(block);
            if (id.toString().toLowerCase(Locale.ROOT).contains(q)) {
                result.add(block);
                continue;
            }
            String name = block.getName().getString().toLowerCase(Locale.ROOT);
            if (name.contains(q)) {
                result.add(block);
            }
        }
        filtered = result;
    }

    private void addHeldBlock() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty() || !(held.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        String blockId = Registries.BLOCK.getId(blockItem.getBlock()).toString();
        toggleBlockedBlock(blockId);
    }

    private void toggleBlockedBlock(String blockId) {
        if (UltmineClientConfig.isLegacyBlockedBlock(blockId)) {
            UltmineClientConfig.removeLegacyBlockedBlock(blockId);
        } else {
            UltmineClientConfig.addLegacyBlockedBlock(blockId);
        }
        UltmineClientConfig.save();
        ClientPlayNetworking.send(new UltmineClassicBlockListSyncC2SPayload(
                UltmineClientConfig.getLegacyBlockedBlocks()));
    }

    private int totalRows() {
        return (filtered.size() + cols - 1) / cols;
    }

    private int maxScroll() {
        return Math.max(0, totalRows() - rows);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= gridX && mouseX <= gridX + gridW && mouseY >= gridY && mouseY <= gridY + gridH) {
            int max = maxScroll();
            if (max > 0) {
                scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) verticalAmount));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() == 0 && mouseX >= gridX && mouseX < gridX + gridW && mouseY >= gridY && mouseY < gridY + gridH) {
            int relX = (int) (mouseX - gridX);
            int relY = (int) (mouseY - gridY);
            int col = relX / (CELL_SIZE + CELL_GAP);
            int row = relY / (CELL_SIZE + CELL_GAP);
            if (col < cols && row < rows) {
                int cellInnerX = col * (CELL_SIZE + CELL_GAP);
                int cellInnerY = row * (CELL_SIZE + CELL_GAP);
                if (relX - cellInnerX < CELL_SIZE && relY - cellInnerY < CELL_SIZE) {
                    int idx = (row + scrollOffset) * cols + col;
                    if (idx >= 0 && idx < filtered.size()) {
                        Block block = filtered.get(idx);
                        toggleBlockedBlock(Registries.BLOCK.getId(block).toString());
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderGradientBackground(context);

        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, palette.panelBg());
        RenderingHelper.drawPanelBorder(context, panelX, panelY, panelW, panelH, palette.sectionBorder());
        RenderingHelper.renderCornerAccents(context, panelX, panelY, panelW, panelH, 6, palette.accentGold());

        renderHeader(context);

        super.render(context, mouseX, mouseY, delta);

        renderGrid(context, mouseX, mouseY);
        renderFooter(context);
    }

    private void renderGradientBackground(DrawContext context) {
        for (int y = 0; y < this.height; y++) {
            float ratio = (float) y / this.height;
            int r = (int) (8 + ratio * 6);
            int g = (int) (8 + ratio * 4);
            int b = (int) (16 + ratio * 10);
            context.fill(0, y, this.width, y + 1, palette.bgOverlay() | (r << 16) | (g << 8) | b);
        }
    }

    private void renderHeader(DrawContext context) {
        int centerX = this.width / 2;
        int headerBottom = panelY + HEADER_HEIGHT;

        context.fill(panelX + 1, panelY + 1, panelX + panelW - 1, headerBottom, palette.panelBgHeader());

        int lineW = panelW - PANEL_PADDING * 4;
        context.fill(centerX - lineW / 2, headerBottom - 1, centerX + lineW / 2, headerBottom, palette.accentGold());

        context.drawCenteredTextWithShadow(textRenderer,
                this.title.copy().formatted(Formatting.GOLD, Formatting.BOLD),
                centerX, panelY + 10, palette.textGold());

        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("murilloskills.classic_block_picker.subtitle").copy().formatted(Formatting.GRAY),
                centerX, panelY + 26, palette.textMuted());
    }

    private void renderGrid(DrawContext context, int mouseX, int mouseY) {
        int startIdx = scrollOffset * cols;
        Block hovered = null;
        int hoveredX = 0;
        int hoveredY = 0;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int idx = startIdx + row * cols + col;
                if (idx >= filtered.size()) {
                    break;
                }

                int cx = gridX + col * (CELL_SIZE + CELL_GAP);
                int cy = gridY + row * (CELL_SIZE + CELL_GAP);

                Block block = filtered.get(idx);
                String blockId = Registries.BLOCK.getId(block).toString();
                boolean locked = UltmineClientConfig.isLegacyBlockedBlock(blockId);
                boolean isHover = mouseX >= cx && mouseX < cx + CELL_SIZE && mouseY >= cy && mouseY < cy + CELL_SIZE;

                int bg = locked ? 0xD0401018 : (isHover ? palette.sectionBgActive() : palette.sectionBg());
                context.fill(cx, cy, cx + CELL_SIZE, cy + CELL_SIZE, bg);

                int borderColor = locked ? palette.textRed() : (isHover ? palette.accentGold() : palette.sectionBorder());
                RenderingHelper.drawPanelBorder(context, cx, cy, CELL_SIZE, CELL_SIZE, borderColor);

                Item iconItem = block.asItem();
                if (iconItem != Items.AIR) {
                    context.drawItem(new ItemStack(iconItem), cx + 2, cy + 2);
                }

                if (isHover) {
                    hovered = block;
                    hoveredX = mouseX;
                    hoveredY = mouseY;
                }
            }
        }

        int maxScroll = maxScroll();
        if (maxScroll > 0) {
            int barH = Math.max(10, gridH * rows / totalRows());
            int barY = gridY + (int) ((float) scrollOffset / maxScroll * (gridH - barH));
            int barX = gridX + gridW - 3;
            context.fill(barX, gridY, barX + 3, gridY + gridH, palette.scrollbarBg());
            context.fill(barX, barY, barX + 3, barY + barH, palette.scrollbarFg());
        }

        if (hovered != null) {
            String id = Registries.BLOCK.getId(hovered).toString();
            boolean locked = UltmineClientConfig.isLegacyBlockedBlock(id);
            List<Text> tooltip = new ArrayList<>();
            tooltip.add(hovered.getName().copy().formatted(Formatting.WHITE));
            tooltip.add(Text.literal(id).formatted(Formatting.DARK_GRAY));
            tooltip.add(Text.translatable(locked
                            ? "murilloskills.classic_block_picker.click_remove"
                            : "murilloskills.classic_block_picker.click_add")
                    .copy().formatted(locked ? Formatting.RED : Formatting.GREEN));
            context.drawTooltip(textRenderer, tooltip, hoveredX, hoveredY);
        }
    }

    private void renderFooter(DrawContext context) {
        int centerX = this.width / 2;
        int blockedCount = UltmineClientConfig.getLegacyBlockedBlocks().size();
        int matchCount = filtered.size();

        String info = Text.translatable("murilloskills.classic_block_picker.footer",
                String.valueOf(matchCount), String.valueOf(blockedCount)).getString();
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(info).formatted(Formatting.GRAY),
                centerX, panelY + panelH - 44, palette.textMuted());
    }

    @Override
    public void close() {
        UltmineClientConfig.save();
        ClientPlayNetworking.send(new UltmineClassicBlockListSyncC2SPayload(
                UltmineClientConfig.getLegacyBlockedBlocks()));
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
