package com.murilloskills.gui;

import com.murilloskills.client.config.UltmineClientConfig;
import com.murilloskills.gui.renderer.RenderingHelper;
import com.murilloskills.network.TrashListSyncC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Item picker for the Auto-Trash list. Shows a searchable grid of all registered
 * items. Clicking an item toggles it in the trash list. Items already in the
 * list render with a red highlight.
 */
public class TrashItemPickerScreen extends Screen {

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

    private final List<Item> allItems = new ArrayList<>();
    private List<Item> filtered = new ArrayList<>();
    private String lastQuery = "";

    public TrashItemPickerScreen(Screen parent) {
        super(Text.translatable("murilloskills.trash_picker.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (allItems.isEmpty()) {
            for (Item item : Registries.ITEM) {
                if (item == net.minecraft.item.Items.AIR) continue;
                allItems.add(item);
            }
            filtered = new ArrayList<>(allItems);
        }
        calculateLayout();

        int centerX = this.width / 2;

        // Search field
        int searchW = Math.min(260, panelW - PANEL_PADDING * 4);
        int searchX = centerX - searchW / 2;
        int searchY = panelY + HEADER_HEIGHT + 6;
        searchField = new TextFieldWidget(textRenderer, searchX, searchY, searchW, 18, Text.empty());
        searchField.setMaxLength(64);
        searchField.setText(lastQuery);
        searchField.setPlaceholder(Text.translatable("murilloskills.trash_picker.search_placeholder")
                .copy().formatted(Formatting.DARK_GRAY));
        searchField.setChangedListener(this::applyFilter);
        this.addDrawableChild(searchField);
        this.setInitialFocus(searchField);

        // Bottom buttons
        int btnW = 90;
        int btnGap = 12;
        int btnY = panelY + panelH - 28;

        ButtonWidget pickHeldBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.trash_picker.pick_held").formatted(Formatting.AQUA),
                (b) -> addHeldItem())
                .dimensions(centerX - btnW - btnGap / 2 - btnW - btnGap, btnY, btnW * 2 + btnGap, 20)
                .build();
        this.addDrawableChild(pickHeldBtn);

        ButtonWidget doneBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.trash_picker.done").formatted(Formatting.GREEN),
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

        // Grid area sits below the search bar and above the bottom buttons.
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
            filtered = new ArrayList<>(allItems);
            return;
        }
        List<Item> result = new ArrayList<>();
        for (Item item : allItems) {
            Identifier id = Registries.ITEM.getId(item);
            if (id.toString().toLowerCase(Locale.ROOT).contains(q)) {
                result.add(item);
                continue;
            }
            String name = item.getName().getString().toLowerCase(Locale.ROOT);
            if (name.contains(q)) {
                result.add(item);
            }
        }
        filtered = result;
    }

    private void addHeldItem() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty()) return;
        String id = Registries.ITEM.getId(held.getItem()).toString();
        toggleTrashItem(id);
    }

    private void toggleTrashItem(String id) {
        if (UltmineClientConfig.isTrashItem(id)) {
            UltmineClientConfig.removeTrashItem(id);
        } else {
            UltmineClientConfig.addTrashItem(id);
        }
        UltmineClientConfig.save();
        ClientPlayNetworking.send(new TrashListSyncC2SPayload(UltmineClientConfig.getTrashItems()));
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
                        Item item = filtered.get(idx);
                        toggleTrashItem(Registries.ITEM.getId(item).toString());
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
                Text.translatable("murilloskills.trash_picker.subtitle").copy().formatted(Formatting.GRAY),
                centerX, panelY + 26, palette.textMuted());
    }

    private void renderGrid(DrawContext context, int mouseX, int mouseY) {
        int startIdx = scrollOffset * cols;
        Item hovered = null;
        int hoveredX = 0, hoveredY = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = startIdx + r * cols + c;
                if (idx >= filtered.size()) break;

                int cx = gridX + c * (CELL_SIZE + CELL_GAP);
                int cy = gridY + r * (CELL_SIZE + CELL_GAP);

                Item item = filtered.get(idx);
                String id = Registries.ITEM.getId(item).toString();
                boolean inTrash = UltmineClientConfig.isTrashItem(id);
                boolean isHover = mouseX >= cx && mouseX < cx + CELL_SIZE && mouseY >= cy && mouseY < cy + CELL_SIZE;

                int bg = inTrash ? 0xD0401018 : (isHover ? palette.sectionBgActive() : palette.sectionBg());
                context.fill(cx, cy, cx + CELL_SIZE, cy + CELL_SIZE, bg);

                int borderColor = inTrash ? palette.textRed() : (isHover ? palette.accentGold() : palette.sectionBorder());
                RenderingHelper.drawPanelBorder(context, cx, cy, CELL_SIZE, CELL_SIZE, borderColor);

                context.drawItem(new ItemStack(item), cx + 2, cy + 2);

                if (isHover) {
                    hovered = item;
                    hoveredX = mouseX;
                    hoveredY = mouseY;
                }
            }
        }

        // Scrollbar
        int maxScroll = maxScroll();
        if (maxScroll > 0) {
            int barH = Math.max(10, gridH * rows / totalRows());
            int barY = gridY + (int) ((float) scrollOffset / maxScroll * (gridH - barH));
            int barX = gridX + gridW - 3;
            context.fill(barX, gridY, barX + 3, gridY + gridH, palette.scrollbarBg());
            context.fill(barX, barY, barX + 3, barY + barH, palette.scrollbarFg());
        }

        if (hovered != null) {
            String id = Registries.ITEM.getId(hovered).toString();
            List<Text> tooltip = new ArrayList<>();
            tooltip.add(hovered.getName().copy().formatted(Formatting.WHITE));
            tooltip.add(Text.literal(id).formatted(Formatting.DARK_GRAY));
            tooltip.add(Text.translatable(UltmineClientConfig.isTrashItem(id)
                            ? "murilloskills.trash_picker.click_remove"
                            : "murilloskills.trash_picker.click_add")
                    .copy().formatted(UltmineClientConfig.isTrashItem(id) ? Formatting.RED : Formatting.GREEN));
            context.drawTooltip(textRenderer, tooltip, hoveredX, hoveredY);
        }
    }

    private void renderFooter(DrawContext context) {
        int centerX = this.width / 2;
        int trashCount = UltmineClientConfig.getTrashItems().size();
        int matchCount = filtered.size();

        String info = Text.translatable("murilloskills.trash_picker.footer",
                String.valueOf(matchCount), String.valueOf(trashCount)).getString();
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(info).formatted(Formatting.GRAY),
                centerX, panelY + panelH - 44, palette.textMuted());
    }

    @Override
    public void close() {
        UltmineClientConfig.save();
        ClientPlayNetworking.send(new TrashListSyncC2SPayload(UltmineClientConfig.getTrashItems()));
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
