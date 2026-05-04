package com.murilloskills.gui;

import com.murilloskills.client.config.OreFilterConfig;
import com.murilloskills.gui.renderer.RenderingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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
 * Uses the mod's ColorPalette for consistent dark-theme styling.
 */
public class OreFilterScreen extends Screen {

    private final Screen parent;
    private final ColorPalette palette = ColorPalette.premium();

    // Layout constants
    private static final int HEADER_HEIGHT = 50;
    private static final int SECTION_GAP = 16;
    private static final int PANEL_PADDING = 10;

    // Calculated layout
    private int panelX, panelY, panelW, panelH;
    private int cols;
    private int cardW, cardH;
    private int cardGap;
    private int oreGridY;
    private int oreViewportH;
    private int visibleRows;
    private int scrollRow;
    private int maxScrollRow;
    private int maxOresSectionY;
    private int bottomY;


    public OreFilterScreen(Screen parent) {
        super(Text.translatable("murilloskills.ore_filter.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();

        int centerX = this.width / 2;

        // === BOTTOM BUTTONS (always visible) ===
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
        int gridW = cols * cardW + (cols - 1) * cardGap;
        int startX = centerX - gridW / 2;
        int firstIndex = scrollRow * cols;
        int lastIndex = Math.min(ores.size(), firstIndex + visibleRows * cols);

        for (int i = firstIndex; i < lastIndex; i++) {
            final OreFilterConfig.OreFilterOption ore = ores.get(i);
            int visibleIndex = i - firstIndex;
            int col = visibleIndex % cols;
            int row = visibleIndex / cols;
            int x = startX + col * (cardW + cardGap);
            int y = oreGridY + row * (cardH + cardGap);

            ButtonWidget btn = ButtonWidget.builder(Text.empty(), (b) -> {
                OreFilterConfig.toggleOre(ore.key());
            }).dimensions(x, y, cardW, cardH).build();
            this.addDrawableChild(btn);
        }

        // === MAX ORES CONTROLS ===
        int ctrlBtnW = 24;
        int ctrlGap = 8;
        int valueBoxW = 50;
        int totalCtrlW = ctrlBtnW + ctrlGap + valueBoxW + ctrlGap + ctrlBtnW;
        int ctrlStartX = centerX - totalCtrlW / 2;
        int ctrlY = maxOresSectionY + 18;

        ButtonWidget minusBtn = ButtonWidget.builder(Text.literal("−").formatted(Formatting.RED), (b) -> {
            OreFilterConfig.setMaxOres(OreFilterConfig.getMaxOres() - 25);
        }).dimensions(ctrlStartX, ctrlY, ctrlBtnW, 20).build();
        this.addDrawableChild(minusBtn);

        ButtonWidget plusBtn = ButtonWidget.builder(Text.literal("+").formatted(Formatting.GREEN), (b) -> {
            OreFilterConfig.setMaxOres(OreFilterConfig.getMaxOres() + 25);
        }).dimensions(ctrlStartX + ctrlBtnW + ctrlGap + valueBoxW + ctrlGap, ctrlY, ctrlBtnW, 20).build();
        this.addDrawableChild(plusBtn);
    }

    private void calculateLayout() {
        // Panel sizing (responsive)
        panelW = Math.min(380, this.width - 20);
        panelX = (this.width - panelW) / 2;
        panelY = 10;

        // Columns based on panel width
        if (panelW < 280) {
            cols = 2;
            cardW = (panelW - PANEL_PADDING * 2 - 6) / 2;
        } else if (panelW < 360) {
            cols = 3;
            cardW = (panelW - PANEL_PADDING * 2 - 12) / 3;
        } else {
            cols = 4;
            cardW = (panelW - PANEL_PADDING * 2 - 18) / 4;
        }
        cardH = 24;
        cardGap = 6;

        // Vertical layout
        oreGridY = panelY + HEADER_HEIGHT + 8;

        List<OreFilterConfig.OreFilterOption> ores = getFilterableOres();
        int oreRows = (int) Math.ceil((double) ores.size() / cols);
        int oreGridH = oreRows * (cardH + cardGap) - cardGap;

        int naturalMaxOresSectionY = oreGridY + oreGridH + SECTION_GAP;
        int maxPanelBottom = Math.min(this.height - 4, panelY + Math.max(238, this.height - 20));
        maxOresSectionY = Math.min(naturalMaxOresSectionY, maxPanelBottom - 80);
        oreViewportH = Math.max(cardH, maxOresSectionY - SECTION_GAP - oreGridY);
        visibleRows = Math.max(1, (oreViewportH + cardGap) / (cardH + cardGap));
        maxScrollRow = Math.max(0, oreRows - visibleRows);
        scrollRow = Math.max(0, Math.min(scrollRow, maxScrollRow));
        bottomY = maxOresSectionY + 52;

        panelH = bottomY + 28 - panelY;

        // Clamp if too tall
        if (panelY + panelH > this.height - 4) {
            panelH = this.height - 4 - panelY;
            bottomY = panelY + panelH - 28;
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
        // === BACKGROUND ===
        renderGradientBackground(context);

        // === MAIN PANEL ===
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, palette.panelBg());
        RenderingHelper.drawPanelBorder(context, panelX, panelY, panelW, panelH, palette.sectionBorder());
        RenderingHelper.renderCornerAccents(context, panelX, panelY, panelW, panelH, 6, palette.accentGold());

        // === HEADER ===
        renderHeader(context);

        // === ORE SECTION (labels over buttons) ===
        renderOreSectionLabels(context);

        // === MAX ORES SECTION ===
        renderMaxOresSection(context);

        // === WIDGETS (buttons) ===
        super.render(context, mouseX, mouseY, delta);

        // === ORE CONTENT (over buttons for layering) ===
        renderOreCardContent(context);

        renderOreScrollBar(context);

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

        // Header bg
        context.fill(panelX + 1, panelY + 1, panelX + panelW - 1, headerBottom, palette.panelBgHeader());

        // Divider line
        int lineW = panelW - PANEL_PADDING * 4;
        context.fill(centerX - lineW / 2, headerBottom - 1, centerX + lineW / 2, headerBottom, palette.accentGold());

        // Title
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("⛏ ").formatted(Formatting.GOLD)
                        .append(this.title.copy().formatted(Formatting.GOLD, Formatting.BOLD)),
                centerX, panelY + 10, palette.textGold());

        // Subtitle
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("murilloskills.ore_filter.subtitle").copy().formatted(Formatting.GRAY),
                centerX, panelY + 26, palette.textMuted());
    }

    private void renderOreSectionLabels(DrawContext context) {
        // Section title above the ore grid
        int labelY = oreGridY - 2;
        // No extra label needed - header makes it clear
    }

    private void renderMaxOresSection(DrawContext context) {
        int centerX = this.width / 2;

        // Section divider with title
        String title = Text.translatable("murilloskills.ore_filter.section.max_ores").getString();
        int titleW = textRenderer.getWidth(title);
        int lineW = (panelW - PANEL_PADDING * 2 - titleW - 20) / 2;

        context.fill(panelX + PANEL_PADDING, maxOresSectionY + 3,
                panelX + PANEL_PADDING + lineW, maxOresSectionY + 4, palette.dividerColor());
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(title).formatted(Formatting.GOLD),
                centerX, maxOresSectionY - 1, palette.textGold());
        context.fill(centerX + titleW / 2 + 10, maxOresSectionY + 3,
                panelX + panelW - PANEL_PADDING, maxOresSectionY + 4, palette.dividerColor());

        // Value display (centered between +/- buttons)
        int boxW = 50;
        int boxH = 20;
        int boxX = centerX - boxW / 2;
        int boxY = maxOresSectionY + 18;

        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, palette.sectionBg());
        RenderingHelper.drawPanelBorder(context, boxX, boxY, boxW, boxH, palette.sectionBorder());

        int maxOres = OreFilterConfig.getMaxOres();
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(String.valueOf(maxOres)).formatted(Formatting.WHITE, Formatting.BOLD),
                centerX, boxY + 6, palette.textWhite());

        // Range hint
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("5 - 500").formatted(Formatting.DARK_GRAY),
                centerX, boxY + boxH + 4, palette.textMuted());
    }

    private void renderOreCardContent(DrawContext context) {
        List<OreFilterConfig.OreFilterOption> ores = getFilterableOres();
        int gridW = cols * cardW + (cols - 1) * cardGap;
        int startX = this.width / 2 - gridW / 2;
        int firstIndex = scrollRow * cols;
        int lastIndex = Math.min(ores.size(), firstIndex + visibleRows * cols);

        for (int i = firstIndex; i < lastIndex; i++) {
            OreFilterConfig.OreFilterOption ore = ores.get(i);
            boolean enabled = OreFilterConfig.isOreEnabled(ore.key());

            int visibleIndex = i - firstIndex;
            int col = visibleIndex % cols;
            int row = visibleIndex / cols;
            int x = startX + col * (cardW + cardGap);
            int y = oreGridY + row * (cardH + cardGap);
            int oreColor = OreFilterConfig.getOreColor(ore.key(), ore.color());

            // Card background overlay (on top of button)
            int bgColor = enabled ? palette.sectionBgActive() : palette.sectionBg();
            context.fill(x + 1, y + 1, x + cardW - 1, y + cardH - 1, bgColor);

            // Left accent bar (ore color)
            int accentAlpha = enabled ? 0xFF : 0x60;
            int accentColor = (accentAlpha << 24) | (oreColor & 0x00FFFFFF);
            context.fill(x + 1, y + 1, x + 3, y + cardH - 1, accentColor);

            // Top highlight when enabled
            if (enabled) {
                context.fill(x + 1, y + 1, x + cardW - 1, y + 2, 0x20FFFFFF);
            }

            // Item icon
            int iconX = x + 5;
            int iconY = y + (cardH - 16) / 2;
            ItemStack icon = getIcon(ore);
            context.drawItem(icon, iconX, iconY);

            // Status dot
            int dotX = iconX + 18;
            int textY = y + (cardH - 8) / 2;
            String dot = enabled ? "●" : "○";
            int dotColor = enabled ? palette.statusReady() : palette.textMuted();
            context.drawTextWithShadow(textRenderer, dot, dotX, textY, dotColor);

            // Ore name
            int nameX = dotX + 10;
            String name = ore.vanilla()
                    ? Text.translatable("murilloskills.ore." + ore.key().toLowerCase()).getString()
                    : ore.displayName();
            int maxNameW = cardW - (nameX - x) - 4;
            if (textRenderer.getWidth(name) > maxNameW) {
                while (textRenderer.getWidth(name + "..") > maxNameW && name.length() > 1) {
                    name = name.substring(0, name.length() - 1);
                }
                name = name + "..";
            }
            int nameColor = enabled ? palette.textWhite() : palette.textMuted();
            context.drawTextWithShadow(textRenderer, name, nameX, textY, nameColor);
        }
    }

    private void renderOreScrollBar(DrawContext context) {
        if (maxScrollRow <= 0) {
            return;
        }

        int barX = panelX + panelW - PANEL_PADDING + 2;
        int barY = oreGridY;
        int barH = oreViewportH;
        int thumbH = Math.max(16, barH * visibleRows / (visibleRows + maxScrollRow));
        int thumbY = barY + (barH - thumbH) * scrollRow / maxScrollRow;

        context.fill(barX, barY, barX + 3, barY + barH, palette.scrollbarBg());
        context.fill(barX, thumbY, barX + 3, thumbY + thumbH, palette.scrollbarFg());
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
        if (mouseY >= oreGridY && mouseY <= oreGridY + oreViewportH && maxScrollRow > 0) {
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
    public void close() {
        OreFilterConfig.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
