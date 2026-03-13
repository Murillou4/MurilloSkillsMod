package com.murilloskills.gui;

import com.murilloskills.client.config.OreFilterConfig;
import com.murilloskills.client.config.OreFilterConfig.DisplayMode;
import com.murilloskills.network.MinerScanResultPayload.OreType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Responsive ore filter configuration screen with adaptive layout.
 * Adjusts columns, spacing and element sizes based on screen dimensions.
 */
public class OreFilterScreen extends Screen {

    private final Screen parent;

    // Colors - Seguindo paleta do ModInfoScreen
    private static final int GOLD = 0xFFD4AF37;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFAAAAAA;
    private static final int TEXT_AQUA = 0xFF55FFFF;
    private static final int PANEL_BG = 0xD0151520;
    private static final int SECTION_BORDER = 0x60FFFFFF;

    // Calculated layout values (set in init)
    private int cols;
    private int cardWidth;
    private int cardHeight;
    private int gap;
    private int oreGridStartY;
    private int modeSectionY;
    private int maxOresSectionY;

    public OreFilterScreen(Screen parent) {
        super(Text.translatable("murilloskills.ore_filter.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        // Calculate responsive layout based on screen size
        calculateLayout();

        int centerX = this.width / 2;
        OreType[] ores = getFilterableOres();

        // === ORE TOGGLE BUTTONS ===
        int gridWidth = cols * cardWidth + (cols - 1) * gap;
        int startX = centerX - gridWidth / 2;

        for (int i = 0; i < ores.length; i++) {
            final OreType ore = ores[i];
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (cardWidth + gap);
            int y = oreGridStartY + row * (cardHeight + gap);

            // Custom accessible button with encapsulated rendering
            ButtonWidget btn = new OreButton(x, y, cardWidth, cardHeight, ore, (b) -> {
                OreFilterConfig.toggleOre(ore);
            });
            this.addDrawableChild(btn);
        }

        // === MODE BUTTONS ===
        DisplayMode current = OreFilterConfig.getDisplayMode();
        int modeWidth = Math.min(130, (this.width - 80) / 3);
        int modeGap = Math.min(15, gap);
        int totalModeWidth = 3 * modeWidth + 2 * modeGap;
        int modeStartX = centerX - totalModeWidth / 2;
        int modeButtonY = modeSectionY + 15;
        int modeButtonHeight = 22;

        ButtonWidget xrayBtn = ButtonWidget.builder(
                getModeButtonText(DisplayMode.XRAY, current == DisplayMode.XRAY),
                (b) -> {
                    OreFilterConfig.setDisplayMode(DisplayMode.XRAY);
                    refreshScreen();
                })
                .dimensions(modeStartX, modeButtonY, modeWidth, modeButtonHeight)
                .build();
        this.addDrawableChild(xrayBtn);

        ButtonWidget visBtn = ButtonWidget.builder(
                getModeButtonText(DisplayMode.VISIBLE_ONLY, current == DisplayMode.VISIBLE_ONLY),
                (b) -> {
                    OreFilterConfig.setDisplayMode(DisplayMode.VISIBLE_ONLY);
                    refreshScreen();
                })
                .dimensions(modeStartX + modeWidth + modeGap, modeButtonY, modeWidth, modeButtonHeight)
                .build();
        this.addDrawableChild(visBtn);

        ButtonWidget nearBtn = ButtonWidget.builder(
                getModeButtonText(DisplayMode.NEAREST_ONLY, current == DisplayMode.NEAREST_ONLY),
                (b) -> {
                    OreFilterConfig.setDisplayMode(DisplayMode.NEAREST_ONLY);
                    refreshScreen();
                })
                .dimensions(modeStartX + 2 * (modeWidth + modeGap), modeButtonY, modeWidth, modeButtonHeight)
                .build();
        this.addDrawableChild(nearBtn);

        // === MAX ORES CONTROLS ===
        int maxControlsY = maxOresSectionY + 20;
        int controlButtonSize = 30;
        int valueBoxWidth = 60;
        int controlsSpacing = 10;
        int totalControlsWidth = controlButtonSize + controlsSpacing + valueBoxWidth + controlsSpacing
                + controlButtonSize;
        int controlsStartX = centerX - totalControlsWidth / 2;

        ButtonWidget minusBtn = ButtonWidget.builder(Text.literal("−").formatted(Formatting.RED), (b) -> {
            OreFilterConfig.setMaxOres(OreFilterConfig.getMaxOres() - 5);
        }).dimensions(controlsStartX, maxControlsY, controlButtonSize, 24).build();
        this.addDrawableChild(minusBtn);

        ButtonWidget plusBtn = ButtonWidget.builder(Text.literal("+").formatted(Formatting.GREEN), (b) -> {
            OreFilterConfig.setMaxOres(OreFilterConfig.getMaxOres() + 5);
        }).dimensions(controlsStartX + controlButtonSize + controlsSpacing + valueBoxWidth + controlsSpacing,
                maxControlsY, controlButtonSize, 24).build();
        this.addDrawableChild(plusBtn);

        // === BOTTOM BUTTONS ===
        int bottomY = this.height - 35;
        int bottomBtnWidth = 100;
        int bottomBtnGap = 20;

        ButtonWidget saveBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.ore_filter.save").formatted(Formatting.GREEN),
                (b) -> {
                    OreFilterConfig.save();
                    close();
                })
                .dimensions(centerX - bottomBtnWidth - bottomBtnGap / 2, bottomY, bottomBtnWidth, 22)
                .build();
        this.addDrawableChild(saveBtn);

        ButtonWidget resetBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.ore_filter.reset").formatted(Formatting.YELLOW),
                (b) -> {
                    resetDefaults();
                    refreshScreen();
                })
                .dimensions(centerX + bottomBtnGap / 2, bottomY, bottomBtnWidth, 22)
                .build();
        this.addDrawableChild(resetBtn);
    }

    /**
     * Calculate responsive layout values based on screen dimensions.
     */
    private void calculateLayout() {
        // Determine number of columns based on width
        if (this.width < 400) {
            cols = 2;
            cardWidth = Math.min(110, (this.width - 40) / 2);
        } else if (this.width < 550) {
            cols = 3;
            cardWidth = Math.min(115, (this.width - 60) / 3);
        } else {
            cols = 4;
            cardWidth = Math.min(120, (this.width - 80) / 4);
        }

        cardHeight = 26;
        gap = Math.max(6, Math.min(10, this.width / 60));

        // Calculate vertical positions
        int headerHeight = 55;

        // Ore grid starts after header (no separate section title needed)
        oreGridStartY = headerHeight + 12;

        // Calculate ore grid height
        OreType[] ores = getFilterableOres();
        int oreRows = (int) Math.ceil((double) ores.length / cols);
        int oreGridHeight = oreRows * (cardHeight + gap) - gap;

        // Mode section starts after ore grid
        modeSectionY = oreGridStartY + oreGridHeight + 25;

        // Description box is 45 pixels below mode section title
        int modeDescBoxOffset = 50;

        // Max ores section
        maxOresSectionY = modeSectionY + modeDescBoxOffset + 35;
    }

    private void refreshScreen() {
        clearChildren();
        init();
    }

    private void resetDefaults() {
        for (OreType ore : OreType.values()) {
            OreFilterConfig.setOreEnabled(ore, ore != OreType.OTHER);
        }
        OreFilterConfig.setDisplayMode(DisplayMode.XRAY);
        OreFilterConfig.setMaxOres(20);
    }

    private Text getModeButtonText(DisplayMode mode, boolean active) {
        String name = Text.translatable("murilloskills.ore_filter.mode." + mode.name().toLowerCase()).getString();
        String prefix = active ? "● " : "○ ";
        Formatting color = active ? Formatting.AQUA : Formatting.GRAY;
        return Text.literal(prefix).formatted(color)
                .append(Text.literal(name).formatted(active ? Formatting.WHITE : Formatting.GRAY));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // === BACKGROUND ===
        renderBackground(context);

        // === HEADER ===
        renderHeader(context);

        // === ORE SECTION ===
        renderOreSection(context);

        // === MODE SECTION ===
        renderModeSection(context);

        // === MAX ORES SECTION ===
        renderMaxOresSection(context);

        // === WIDGETS ===
        super.render(context, mouseX, mouseY, delta);

    }

    private void renderBackground(DrawContext context) {
        // Dark gradient background
        for (int y = 0; y < this.height; y++) {
            float ratio = (float) y / this.height;
            int r = (int) (10 + ratio * 8);
            int g = (int) (10 + ratio * 6);
            int b = (int) (20 + ratio * 12);
            context.fill(0, y, this.width, y + 1, 0xF0000000 | (r << 16) | (g << 8) | b);
        }
    }

    private void renderHeader(DrawContext context) {
        int centerX = this.width / 2;

        // Header panel
        context.fill(0, 0, this.width, 55, 0xE0101018);
        context.fill(0, 54, this.width, 55, 0x40FFFFFF);

        // Gold accent line (responsive width)
        int lineWidth = Math.min(200, this.width - 40);
        context.fill(centerX - lineWidth / 2, 53, centerX + lineWidth / 2, 54, GOLD);

        // Title
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("⚙ ").formatted(Formatting.GOLD)
                        .append(this.title.copy().formatted(Formatting.GOLD, Formatting.BOLD)),
                centerX, 12, GOLD);

        // Subtitle
        Text sub = Text.translatable("murilloskills.ore_filter.subtitle");
        context.drawCenteredTextWithShadow(textRenderer, sub, centerX, 30, 0x888899);

        // Decorative corners
        context.fill(0, 0, 8, 2, GOLD);
        context.fill(0, 0, 2, 8, GOLD);
        context.fill(this.width - 8, 0, this.width, 2, GOLD);
        context.fill(this.width - 2, 0, this.width, 8, GOLD);
    }

    private void renderOreSection(DrawContext context) {
        // No separate ore section title - the header already says what this screen is
        // for
        // This prevents the "duplicated text" feel
    }

    private void renderModeSection(DrawContext context) {
        int centerX = this.width / 2;

        // Section title
        String title = Text.translatable("murilloskills.ore_filter.section.mode").getString();
        int titleWidth = textRenderer.getWidth(title);
        int lineExtent = Math.min(200, (this.width - titleWidth) / 2 - 30);

        context.fill(centerX - lineExtent - titleWidth / 2 - 10, modeSectionY, centerX - titleWidth / 2 - 10,
                modeSectionY + 1, 0x40D4AF37);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(title).formatted(Formatting.GOLD), centerX,
                modeSectionY - 4, GOLD);
        context.fill(centerX + titleWidth / 2 + 10, modeSectionY, centerX + lineExtent + titleWidth / 2 + 10,
                modeSectionY + 1, 0x40D4AF37);

        // Current mode description box
        DisplayMode current = OreFilterConfig.getDisplayMode();
        Text modeDesc = Text.translatable("murilloskills.ore_filter.mode." + current.name().toLowerCase() + ".desc");

        int descBoxWidth = Math.min(300, this.width - 40);
        int descBoxHeight = 22;
        int descBoxX = centerX - descBoxWidth / 2;
        int descBoxY = modeSectionY + 45;

        // Background
        context.fill(descBoxX, descBoxY, descBoxX + descBoxWidth, descBoxY + descBoxHeight, 0xC0101820);
        // Top border
        context.fill(descBoxX, descBoxY, descBoxX + descBoxWidth, descBoxY + 1, TEXT_AQUA);
        // Bottom shadow
        context.fill(descBoxX, descBoxY + descBoxHeight - 1, descBoxX + descBoxWidth, descBoxY + descBoxHeight,
                0x40000000);

        // Description text
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("ℹ ").formatted(Formatting.AQUA).append(modeDesc.copy().formatted(Formatting.WHITE)),
                centerX, descBoxY + 7, TEXT_WHITE);
    }

    private void renderMaxOresSection(DrawContext context) {
        int centerX = this.width / 2;

        // Section title
        String title = Text.translatable("murilloskills.ore_filter.section.max_ores").getString();
        int titleWidth = textRenderer.getWidth(title);
        int lineExtent = Math.min(150, (this.width - titleWidth) / 2 - 30);

        context.fill(centerX - lineExtent - titleWidth / 2 - 10, maxOresSectionY, centerX - titleWidth / 2 - 10,
                maxOresSectionY + 1, 0x40D4AF37);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(title).formatted(Formatting.GOLD), centerX,
                maxOresSectionY - 4, GOLD);
        context.fill(centerX + titleWidth / 2 + 10, maxOresSectionY, centerX + lineExtent + titleWidth / 2 + 10,
                maxOresSectionY + 1, 0x40D4AF37);

        // Value display box (centered between the +/- buttons)
        int boxWidth = 60;
        int boxHeight = 28;
        int boxX = centerX - boxWidth / 2;
        int boxY = maxOresSectionY + 18;

        // Background
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, PANEL_BG);
        // Borders
        context.fill(boxX, boxY, boxX + boxWidth, boxY + 1, SECTION_BORDER);
        context.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, 0x40000000);
        context.fill(boxX, boxY, boxX + 1, boxY + boxHeight, SECTION_BORDER);
        context.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, SECTION_BORDER);

        // Value
        int maxOres = OreFilterConfig.getMaxOres();
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(String.valueOf(maxOres)).formatted(Formatting.WHITE, Formatting.BOLD),
                centerX, boxY + 10, TEXT_WHITE);

        // Range hint
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("(").formatted(Formatting.GRAY)
                        .append(Text.literal("5").formatted(Formatting.AQUA))
                        .append(Text.literal(" - ").formatted(Formatting.GRAY))
                        .append(Text.literal("50").formatted(Formatting.AQUA))
                        .append(Text.literal(")").formatted(Formatting.GRAY)),
                centerX, boxY + boxHeight + 5, TEXT_GRAY);
    }

    private OreType[] getFilterableOres() {
        return new OreType[] {
                OreType.COAL, OreType.COPPER, OreType.IRON, OreType.GOLD,
                OreType.LAPIS, OreType.REDSTONE, OreType.DIAMOND, OreType.EMERALD,
                OreType.ANCIENT_DEBRIS, OreType.NETHER_QUARTZ, OreType.NETHER_GOLD
        };
    }

    private ItemStack getIcon(OreType ore) {
        return switch (ore) {
            case COAL -> new ItemStack(Items.COAL);
            case COPPER -> new ItemStack(Items.COPPER_INGOT);
            case IRON -> new ItemStack(Items.IRON_INGOT);
            case GOLD -> new ItemStack(Items.GOLD_INGOT);
            case LAPIS -> new ItemStack(Items.LAPIS_LAZULI);
            case REDSTONE -> new ItemStack(Items.REDSTONE);
            case DIAMOND -> new ItemStack(Items.DIAMOND);
            case EMERALD -> new ItemStack(Items.EMERALD);
            case ANCIENT_DEBRIS -> new ItemStack(Items.NETHERITE_INGOT);
            case NETHER_QUARTZ -> new ItemStack(Items.QUARTZ);
            case NETHER_GOLD -> new ItemStack(Items.GOLD_NUGGET);
            case OTHER -> new ItemStack(Items.COBBLESTONE);
        };
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

    /**
     * Custom button class that encapsulates ore rendering logic and provides proper accessibility.
     */
    private class OreButton extends ButtonWidget {
        private final OreType ore;

        public OreButton(int x, int y, int width, int height, OreType ore, ButtonWidget.PressAction onPress) {
            super(x, y, width, height, Text.translatable("murilloskills.ore." + ore.name().toLowerCase()), onPress, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
            this.ore = ore;
        }

        @Override
        public void drawMessage(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, int color) {
            // No-op: prevent default text rendering to avoid overlap
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            super.renderWidget(context, mouseX, mouseY, delta);

            boolean enabled = OreFilterConfig.isOreEnabled(ore);

            // Draw item icon at fixed position (left side of button)
            int iconX = this.getX() + 4;
            int iconY = this.getY() + (this.height - 16) / 2;
            ItemStack icon = getIcon(ore);
            context.drawItem(icon, iconX, iconY);

            // Status indicator
            int statusX = iconX + 18;
            int textY = this.getY() + (this.height - 8) / 2;
            String statusIcon = enabled ? "◆" : "◇";
            int statusColor = enabled ? 0xFF55FF55 : 0xFF555555;
            context.drawTextWithShadow(textRenderer, statusIcon, statusX, textY, statusColor);

            // Ore name
            int nameX = statusX + 12;
            String name = Text.translatable("murilloskills.ore." + ore.name().toLowerCase()).getString();

            // Truncate name if too long
            int maxNameWidth = this.width - (nameX - this.getX()) - 4;
            if (textRenderer.getWidth(name) > maxNameWidth) {
                while (textRenderer.getWidth(name + "...") > maxNameWidth && name.length() > 1) {
                    name = name.substring(0, name.length() - 1);
                }
                name = name + "...";
            }

            int nameColor = enabled ? 0xFFFFFFFF : 0xFFAAAAAA;
            context.drawTextWithShadow(textRenderer, name, nameX, textY, nameColor);
        }
    }
}
