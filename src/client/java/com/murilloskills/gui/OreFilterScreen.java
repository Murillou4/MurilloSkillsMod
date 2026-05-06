package com.murilloskills.gui;

import com.murilloskills.client.config.OreFilterConfig;
import com.murilloskills.gui.renderer.RenderingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Premium ore filter configuration screen with adaptive layout.
 * Cards show item icon, color swatch, name, and ON/OFF state with proper breathing room.
 * Includes a toolbar (Toggle All/None) and a draggable scrollbar.
 */
public class OreFilterScreen extends Screen {

    private final Screen parent;
    private final ColorPalette palette = ColorPalette.premium();

    // Layout constants
    private static final int HEADER_HEIGHT = 38;
    private static final int SECTION_GAP = 12;
    private static final int PANEL_PADDING = 12;
    private static final int CARD_HEIGHT = 28;
    private static final int CARD_GAP = 6;
    private static final int SCROLLBAR_WIDTH = 7;

    // Calculated layout
    private int panelX, panelY, panelW, panelH;
    private int cols;
    private int cardW;
    private int gridX;
    private int gridY;
    private int gridW;
    private int oreViewportH;
    private int visibleRows;
    private int scrollRow;
    private int maxScrollRow;
    private int toolbarY;
    private int maxOresSectionY;
    private int bottomY;
    private TextFieldWidget maxOresField;

    // Scrollbar state
    private int scrollbarTrackY;
    private int scrollbarTrackH;
    private int scrollbarThumbY;
    private int scrollbarThumbH;
    private boolean draggingScrollbar = false;
    private double scrollbarDragGrabOffset = 0;


    public OreFilterScreen(Screen parent) {
        super(Text.translatable("murilloskills.ore_filter.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();

        int centerX = this.width / 2;

        // === TOOLBAR (toggle all / none) ===
        int toolBtnH = 16;
        int toolBtnW = 78;
        int toolGap = 6;
        int toolbarTotalW = toolBtnW * 2 + toolGap;
        int toolbarStartX = panelX + panelW - PANEL_PADDING - toolbarTotalW;

        ButtonWidget allBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.ore_filter.toggle_all").formatted(Formatting.GREEN),
                (b) -> {
                    for (OreFilterConfig.OreFilterOption ore : OreFilterConfig.getFilterOptions()) {
                        OreFilterConfig.setOreEnabled(ore.key(), true);
                    }
                    refreshScreen();
                }).dimensions(toolbarStartX, toolbarY, toolBtnW, toolBtnH).build();
        this.addDrawableChild(allBtn);

        ButtonWidget noneBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.ore_filter.toggle_none").formatted(Formatting.RED),
                (b) -> {
                    for (OreFilterConfig.OreFilterOption ore : OreFilterConfig.getFilterOptions()) {
                        OreFilterConfig.setOreEnabled(ore.key(), false);
                    }
                    refreshScreen();
                }).dimensions(toolbarStartX + toolBtnW + toolGap, toolbarY, toolBtnW, toolBtnH).build();
        this.addDrawableChild(noneBtn);

        // === BOTTOM BUTTONS ===
        int btnW = 90;
        int btnGap = 12;

        ButtonWidget saveBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.ore_filter.save").formatted(Formatting.GREEN),
                (b) -> { OreFilterConfig.save(); close(); })
                .dimensions(centerX - btnW - btnGap / 2, bottomY, btnW, 20)
                .build();
        this.addDrawableChild(saveBtn);

        ButtonWidget resetBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.ore_filter.reset").formatted(Formatting.YELLOW),
                (b) -> { resetDefaults(); refreshScreen(); })
                .dimensions(centerX + btnGap / 2, bottomY, btnW, 20)
                .build();
        this.addDrawableChild(resetBtn);

        // === ORE TOGGLE BUTTONS ===
        List<OreFilterConfig.OreFilterOption> ores = getFilterableOres();
        int firstIndex = scrollRow * cols;
        int lastIndex = Math.min(ores.size(), firstIndex + visibleRows * cols);

        for (int i = firstIndex; i < lastIndex; i++) {
            final OreFilterConfig.OreFilterOption ore = ores.get(i);
            int visibleIndex = i - firstIndex;
            int col = visibleIndex % cols;
            int row = visibleIndex / cols;
            int x = gridX + col * (cardW + CARD_GAP);
            int y = gridY + row * (CARD_HEIGHT + CARD_GAP);

            ButtonWidget btn = ButtonWidget.builder(Text.empty(), (b) -> {
                OreFilterConfig.toggleOre(ore.key());
            }).dimensions(x, y, cardW, CARD_HEIGHT).build();
            this.addDrawableChild(btn);
        }

        // === MAX ORES CONTROLS ===
        int ctrlBtnW = 24;
        int ctrlGap = 8;
        int valueBoxW = 50;
        int totalCtrlW = ctrlBtnW + ctrlGap + valueBoxW + ctrlGap + ctrlBtnW;
        int ctrlStartX = centerX - totalCtrlW / 2;
        int ctrlY = maxOresSectionY + 16;

        ButtonWidget minusBtn = ButtonWidget.builder(Text.literal("−").formatted(Formatting.RED), (b) -> {
            OreFilterConfig.setMaxOres(OreFilterConfig.getMaxOres() - 25);
            refreshScreen();
        }).dimensions(ctrlStartX, ctrlY, ctrlBtnW, 20).build();
        this.addDrawableChild(minusBtn);

        maxOresField = new TextFieldWidget(textRenderer, ctrlStartX + ctrlBtnW + ctrlGap, ctrlY, valueBoxW, 20,
                Text.empty());
        maxOresField.setMaxLength(3);
        maxOresField.setTextPredicate(text -> text.isEmpty() || text.matches("[0-9]{0,3}"));
        maxOresField.setText(String.valueOf(OreFilterConfig.getMaxOres()));
        boolean[] updatingMaxOresField = new boolean[] { false };
        maxOresField.setChangedListener(text -> {
            if (updatingMaxOresField[0] || text == null || text.isBlank()) {
                return;
            }
            try {
                int typedValue = Integer.parseInt(text.trim());
                OreFilterConfig.setMaxOres(typedValue);
                int normalizedValue = OreFilterConfig.getMaxOres();
                if (normalizedValue != typedValue) {
                    updatingMaxOresField[0] = true;
                    maxOresField.setText(String.valueOf(normalizedValue));
                    updatingMaxOresField[0] = false;
                }
            } catch (NumberFormatException ignored) {
            }
        });
        this.addDrawableChild(maxOresField);

        ButtonWidget plusBtn = ButtonWidget.builder(Text.literal("+").formatted(Formatting.GREEN), (b) -> {
            OreFilterConfig.setMaxOres(OreFilterConfig.getMaxOres() + 25);
            refreshScreen();
        }).dimensions(ctrlStartX + ctrlBtnW + ctrlGap + valueBoxW + ctrlGap, ctrlY, ctrlBtnW, 20).build();
        this.addDrawableChild(plusBtn);
    }

    private void calculateLayout() {
        // Choose panel width: prefer wider for 2-col grid; cap by available space
        int desiredW = (this.width >= 520) ? 500 : 380;
        panelW = Math.min(desiredW, this.width - 16);
        panelX = (this.width - panelW) / 2;
        panelY = 8;

        // Card columns based on panel inner width — bigger cards so content breathes
        int innerW = panelW - PANEL_PADDING * 2 - SCROLLBAR_WIDTH - 4;
        if (innerW >= 380) {
            cols = 3;
        } else if (innerW >= 240) {
            cols = 2;
        } else {
            cols = 1;
        }
        cardW = (innerW - (cols - 1) * CARD_GAP) / cols;
        gridW = cols * cardW + (cols - 1) * CARD_GAP;
        gridX = panelX + PANEL_PADDING;

        // Toolbar sits between header and grid
        toolbarY = panelY + HEADER_HEIGHT + 6;
        gridY = toolbarY + 16 + 8;

        List<OreFilterConfig.OreFilterOption> ores = getFilterableOres();
        int oreRows = (int) Math.ceil((double) ores.size() / cols);
        int maxOreRowsByHeight = Math.max(1,
                (this.height - gridY - 90 + CARD_GAP) / (CARD_HEIGHT + CARD_GAP));
        visibleRows = Math.min(oreRows, maxOreRowsByHeight);
        oreViewportH = visibleRows * (CARD_HEIGHT + CARD_GAP) - CARD_GAP;
        maxScrollRow = Math.max(0, oreRows - visibleRows);
        scrollRow = Math.max(0, Math.min(scrollRow, maxScrollRow));

        // Max ores section sits below the grid viewport
        maxOresSectionY = gridY + oreViewportH + SECTION_GAP;
        bottomY = maxOresSectionY + 46;

        panelH = bottomY + 26 - panelY;

        // Clamp if too tall
        if (panelY + panelH > this.height - 4) {
            panelH = this.height - 4 - panelY;
        }
    }

    private void refreshScreen() {
        clearChildren();
        init();
    }

    private void resetDefaults() {
        for (OreFilterConfig.OreFilterOption ore : OreFilterConfig.getFilterOptions()) {
            OreFilterConfig.setOreEnabled(ore.key(), true);
        }
        OreFilterConfig.setMaxOres(500);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderGradientBackground(context);

        // Main panel
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, palette.panelBg());
        RenderingHelper.drawPanelBorder(context, panelX, panelY, panelW, panelH, palette.sectionBorder());
        RenderingHelper.renderCornerAccents(context, panelX, panelY, panelW, panelH, 6, palette.accentGold());

        renderHeader(context);
        renderToolbarLabels(context);
        renderMaxOresSection(context);

        super.render(context, mouseX, mouseY, delta);

        // Card content drawn on top of buttons
        renderOreCards(context, mouseX, mouseY);

        renderScrollBar(context);
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
                Text.literal("⛏ ").formatted(Formatting.GOLD)
                        .append(this.title.copy().formatted(Formatting.GOLD, Formatting.BOLD)),
                centerX, panelY + 5, palette.textGold());

        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("murilloskills.ore_filter.subtitle").copy().formatted(Formatting.GRAY),
                centerX, panelY + 19, palette.textMuted());
    }

    private void renderToolbarLabels(DrawContext context) {
        // Section label on the left
        String label = Text.translatable("murilloskills.ore_filter.section.ores").getString();
        context.drawTextWithShadow(textRenderer,
                Text.literal(label).formatted(Formatting.GOLD, Formatting.BOLD),
                panelX + PANEL_PADDING, toolbarY + 4, palette.textGold());

        // Counter "X / Y enabled"
        List<OreFilterConfig.OreFilterOption> ores = getFilterableOres();
        int enabled = 0;
        for (OreFilterConfig.OreFilterOption ore : ores) {
            if (OreFilterConfig.isOreEnabled(ore.key())) enabled++;
        }
        String count = enabled + " / " + ores.size();
        int labelW = textRenderer.getWidth(label);
        context.drawTextWithShadow(textRenderer,
                Text.literal(count).formatted(Formatting.GRAY),
                panelX + PANEL_PADDING + labelW + 8, toolbarY + 4, palette.textMuted());
    }

    private void renderMaxOresSection(DrawContext context) {
        int centerX = this.width / 2;

        // Section divider with title
        String title = Text.translatable("murilloskills.ore_filter.section.max_ores").getString();
        int titleW = textRenderer.getWidth(title);
        int sideMargin = PANEL_PADDING + 4;
        int lineLeftStart = panelX + sideMargin;
        int lineLeftEnd = centerX - titleW / 2 - 8;
        int lineRightStart = centerX + titleW / 2 + 8;
        int lineRightEnd = panelX + panelW - sideMargin;

        if (lineLeftEnd > lineLeftStart) {
            context.fill(lineLeftStart, maxOresSectionY + 3, lineLeftEnd, maxOresSectionY + 4,
                    palette.dividerColor());
        }
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(title).formatted(Formatting.GOLD),
                centerX, maxOresSectionY - 1, palette.textGold());
        if (lineRightEnd > lineRightStart) {
            context.fill(lineRightStart, maxOresSectionY + 3, lineRightEnd, maxOresSectionY + 4,
                    palette.dividerColor());
        }

        // Range hint below the controls
        int hintY = maxOresSectionY + 16 + 22;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("5 - 500").formatted(Formatting.DARK_GRAY),
                centerX, hintY, palette.textMuted());
    }

    private void renderOreCards(DrawContext context, int mouseX, int mouseY) {
        // Clip cards to viewport so partial bottom rows don't bleed
        context.enableScissor(panelX, gridY, panelX + panelW, gridY + oreViewportH);

        List<OreFilterConfig.OreFilterOption> ores = getFilterableOres();
        int firstIndex = scrollRow * cols;
        int lastIndex = Math.min(ores.size(), firstIndex + visibleRows * cols);

        for (int i = firstIndex; i < lastIndex; i++) {
            OreFilterConfig.OreFilterOption ore = ores.get(i);
            boolean enabled = OreFilterConfig.isOreEnabled(ore.key());

            int visibleIndex = i - firstIndex;
            int col = visibleIndex % cols;
            int row = visibleIndex / cols;
            int x = gridX + col * (cardW + CARD_GAP);
            int y = gridY + row * (CARD_HEIGHT + CARD_GAP);
            int oreColor = OreFilterConfig.getOreColor(ore.key(), ore.color());

            boolean hovered = mouseX >= x && mouseX < x + cardW && mouseY >= y && mouseY < y + CARD_HEIGHT;

            // Card background (over button)
            int bgColor = enabled ? palette.sectionBgActive() : palette.sectionBg();
            context.fill(x + 1, y + 1, x + cardW - 1, y + CARD_HEIGHT - 1, bgColor);

            // Border
            int borderColor = enabled
                    ? (hovered ? palette.sectionBorderActive() : palette.sectionBorder())
                    : (hovered ? palette.sectionBorderActive() : palette.sectionBorder());
            RenderingHelper.drawPanelBorder(context, x, y, cardW, CARD_HEIGHT, borderColor);

            // Left accent bar (always shows ore color, dimmed when disabled)
            int accentAlpha = enabled ? 0xFF : 0x66;
            int accentColor = (accentAlpha << 24) | (oreColor & 0x00FFFFFF);
            context.fill(x + 1, y + 1, x + 4, y + CARD_HEIGHT - 1, accentColor);

            // Top highlight when enabled
            if (enabled) {
                context.fill(x + 4, y + 1, x + cardW - 1, y + 2, 0x30FFFFFF);
            }

            // Item icon
            int iconX = x + 8;
            int iconY = y + (CARD_HEIGHT - 16) / 2;
            ItemStack icon = getIcon(ore);
            context.drawItem(icon, iconX, iconY);

            // Color swatch (small filled square showing highlight color)
            int swatchSize = 8;
            int swatchX = x + cardW - swatchSize - 22;
            int swatchY = y + (CARD_HEIGHT - swatchSize) / 2;
            int swatchColor = enabled ? (0xFF000000 | (oreColor & 0x00FFFFFF))
                    : ((0x60 << 24) | (oreColor & 0x00FFFFFF));
            context.fill(swatchX, swatchY, swatchX + swatchSize, swatchY + swatchSize, swatchColor);
            RenderingHelper.drawPanelBorder(context, swatchX - 1, swatchY - 1, swatchSize + 2, swatchSize + 2,
                    palette.sectionBorder());

            // ON/OFF indicator on the right
            String state = enabled ? "ON" : "OFF";
            int stateColor = enabled ? palette.statusReady() : palette.statusCooldown();
            int stateW = textRenderer.getWidth(state);
            int stateX = x + cardW - stateW - 6;
            int textY = y + (CARD_HEIGHT - 8) / 2;
            context.drawTextWithShadow(textRenderer, state, stateX, textY, stateColor);

            // Ore name (between icon and swatch)
            int nameX = iconX + 20;
            int nameMaxX = swatchX - 4;
            String name = ore.vanilla()
                    ? Text.translatable("murilloskills.ore." + ore.key().toLowerCase()).getString()
                    : ore.displayName();
            int maxNameW = nameMaxX - nameX;
            if (textRenderer.getWidth(name) > maxNameW) {
                while (textRenderer.getWidth(name + "..") > maxNameW && name.length() > 1) {
                    name = name.substring(0, name.length() - 1);
                }
                name = name + "..";
            }
            int nameColor = enabled ? palette.textWhite() : palette.textMuted();
            context.drawTextWithShadow(textRenderer, name, nameX, textY, nameColor);
        }

        context.disableScissor();
    }

    private void renderScrollBar(DrawContext context) {
        if (maxScrollRow <= 0) {
            scrollbarTrackH = 0;
            return;
        }

        scrollbarTrackY = gridY;
        scrollbarTrackH = oreViewportH;
        scrollbarThumbH = Math.max(20, oreViewportH * visibleRows / (visibleRows + maxScrollRow));
        scrollbarThumbY = scrollbarTrackY
                + (oreViewportH - scrollbarThumbH) * scrollRow / maxScrollRow;

        int barX = panelX + panelW - PANEL_PADDING + 2;

        context.fill(barX, scrollbarTrackY, barX + SCROLLBAR_WIDTH, scrollbarTrackY + scrollbarTrackH,
                palette.scrollbarBg());
        int thumbColor = draggingScrollbar ? palette.scrollbarActive() : palette.scrollbarFg();
        context.fill(barX, scrollbarThumbY, barX + SCROLLBAR_WIDTH, scrollbarThumbY + scrollbarThumbH,
                thumbColor);
        // Inner highlight on thumb
        context.fill(barX + 1, scrollbarThumbY + 1, barX + SCROLLBAR_WIDTH - 1, scrollbarThumbY + 2,
                0x40FFFFFF);
    }

    private List<OreFilterConfig.OreFilterOption> getFilterableOres() {
        return OreFilterConfig.getFilterOptions();
    }

    private ItemStack getIcon(OreFilterConfig.OreFilterOption ore) {
        return switch (ore.key()) {
            case "COAL" -> new ItemStack(Items.COAL);
            case "COPPER" -> new ItemStack(Items.COPPER_INGOT);
            case "IRON" -> new ItemStack(Items.IRON_INGOT);
            case "GOLD" -> new ItemStack(Items.GOLD_INGOT);
            case "LAPIS" -> new ItemStack(Items.LAPIS_LAZULI);
            case "REDSTONE" -> new ItemStack(Items.REDSTONE);
            case "DIAMOND" -> new ItemStack(Items.DIAMOND);
            case "EMERALD" -> new ItemStack(Items.EMERALD);
            case "ANCIENT_DEBRIS" -> new ItemStack(Items.NETHERITE_INGOT);
            case "NETHER_QUARTZ" -> new ItemStack(Items.QUARTZ);
            case "NETHER_GOLD" -> new ItemStack(Items.GOLD_NUGGET);
            default -> getModdedOreIcon(ore.key());
        };
    }

    private ItemStack getModdedOreIcon(String key) {
        try {
            Identifier id = Identifier.of(key);
            Item item = Registries.ITEM.get(id);
            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception ignored) {
        }
        return new ItemStack(Items.RAW_IRON);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= gridY && mouseY <= gridY + oreViewportH && maxScrollRow > 0) {
            int direction = verticalAmount > 0 ? -1 : 1;
            int nextRow = Math.max(0, Math.min(maxScrollRow, scrollRow + direction));
            if (nextRow != scrollRow) {
                scrollRow = nextRow;
                refreshScreen();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() == 0 && scrollbarTrackH > 0) {
            int barX = panelX + panelW - PANEL_PADDING + 2;
            // Generous hit test
            if (mouseX >= barX - 2 && mouseX <= barX + SCROLLBAR_WIDTH + 2
                    && mouseY >= scrollbarTrackY && mouseY <= scrollbarTrackY + scrollbarTrackH) {
                if (mouseY >= scrollbarThumbY && mouseY <= scrollbarThumbY + scrollbarThumbH) {
                    draggingScrollbar = true;
                    scrollbarDragGrabOffset = mouseY - scrollbarThumbY;
                } else {
                    draggingScrollbar = true;
                    scrollbarDragGrabOffset = scrollbarThumbH / 2.0;
                    updateScrollFromMouse(mouseY);
                }
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
        if (maxScrollRow <= 0 || scrollbarTrackH <= scrollbarThumbH) {
            return;
        }
        double newThumbTop = mouseY - scrollbarDragGrabOffset;
        double trackRange = scrollbarTrackH - scrollbarThumbH;
        double rel = (newThumbTop - scrollbarTrackY) / trackRange;
        rel = Math.max(0.0, Math.min(1.0, rel));
        int newRow = (int) Math.round(rel * maxScrollRow);
        if (newRow != scrollRow) {
            scrollRow = newRow;
            refreshScreen();
        }
    }

    @Override
    public void close() {
        OreFilterConfig.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
