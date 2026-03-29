package com.murilloskills.gui;

import com.murilloskills.client.config.UltmineClientConfig;
import com.murilloskills.data.UltmineClientState;
import com.murilloskills.gui.renderer.RenderingHelper;
import com.murilloskills.network.UltmineShapeSelectC2SPayload;
import com.murilloskills.network.VeinMinerDropsToggleC2SPayload;
import com.murilloskills.network.XpDirectToggleC2SPayload;
import com.murilloskills.skills.UltmineShape;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Client-side Ultmine configuration screen.
 * Allows customizing per-shape depth/length/variant and global toggles.
 */
public class UltmineConfigScreen extends Screen {

    private final Screen parent;
    private final ColorPalette palette = ColorPalette.premium();

    private static final int HEADER_HEIGHT = 50;
    private static final int SECTION_GAP = 14;
    private static final int PANEL_PADDING = 10;

    // Selected shape for per-shape config
    private UltmineShape selectedShape = UltmineShape.S_3x3;

    // Text fields for depth/length
    private TextFieldWidget depthField;
    private TextFieldWidget lengthField;

    // Layout
    private int panelX, panelY, panelW, panelH;
    private int toggleSectionY;
    private int shapeSelectorY;
    private int shapeConfigY;
    private int bottomY;

    public UltmineConfigScreen(Screen parent) {
        super(Text.translatable("murilloskills.ultmine_config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();

        int centerX = this.width / 2;

        // === GLOBAL TOGGLES ===
        int toggleW = panelW - PANEL_PADDING * 4;
        int toggleGap = 4;
        int toggleBtnW = (toggleW - toggleGap * 2) / 3;
        int toggleStartX = centerX - toggleW / 2;

        // Drops to Inventory toggle
        ButtonWidget dropsBtn = ButtonWidget.builder(Text.empty(), (b) -> {
            UltmineClientConfig.toggleDropsToInventory();
            ClientPlayNetworking.send(new VeinMinerDropsToggleC2SPayload());
            refreshScreen();
        }).dimensions(toggleStartX, toggleSectionY + 16, toggleBtnW, 20).build();
        this.addDrawableChild(dropsBtn);

        // XP Direct to Player toggle
        ButtonWidget xpDirectBtn = ButtonWidget.builder(Text.empty(), (b) -> {
            UltmineClientConfig.toggleXpDirectToPlayer();
            ClientPlayNetworking.send(new XpDirectToggleC2SPayload(UltmineClientConfig.isXpDirectToPlayer()));
            refreshScreen();
        }).dimensions(toggleStartX + toggleBtnW + toggleGap, toggleSectionY + 16, toggleBtnW, 20).build();
        this.addDrawableChild(xpDirectBtn);

        // Same block only toggle
        ButtonWidget sameBlockBtn = ButtonWidget.builder(Text.empty(), (b) -> {
            UltmineClientConfig.toggleSameBlockOnly();
            refreshScreen();
        }).dimensions(toggleStartX + 2 * (toggleBtnW + toggleGap), toggleSectionY + 16, toggleBtnW, 20).build();
        this.addDrawableChild(sameBlockBtn);

        // === SHAPE SELECTOR BUTTONS ===
        UltmineShape[] shapes = UltmineShape.values();
        int shapeCount = shapes.length;
        int shapeBtnW = Math.min(55, (panelW - PANEL_PADDING * 2 - (shapeCount - 1) * 4) / shapeCount);
        int totalShapesW = shapeCount * shapeBtnW + (shapeCount - 1) * 4;
        int shapeStartX = centerX - totalShapesW / 2;

        for (int i = 0; i < shapeCount; i++) {
            final UltmineShape shape = shapes[i];
            int x = shapeStartX + i * (shapeBtnW + 4);
            ButtonWidget shapeBtn = ButtonWidget.builder(Text.empty(), (b) -> {
                selectedShape = shape;
                refreshScreen();
            }).dimensions(x, shapeSelectorY + 16, shapeBtnW, 18).build();
            this.addDrawableChild(shapeBtn);
        }

        // === PER-SHAPE CONFIG ===
        int maxDepth = SkillConfig.getUltmineShapeMaxDepth(selectedShape);
        int maxLength = SkillConfig.getUltmineShapeMaxLength(selectedShape);
        int currentDepth = getEffectiveDepth(selectedShape);
        int currentLength = getEffectiveLength(selectedShape);

        int ctrlBtnW = 20;
        int ctrlGap = 6;
        int fieldW = 50;

        // Depth text field (only if maxDepth > 1)
        depthField = null;
        if (maxDepth > 1) {
            int depthRowY = shapeConfigY + 30;
            int fieldX = centerX + 20;

            depthField = new TextFieldWidget(textRenderer, fieldX, depthRowY, fieldW, 18, Text.empty());
            depthField.setMaxLength(4);
            depthField.setText(String.valueOf(currentDepth));
            depthField.setChangedListener(text -> {
                try {
                    int val = Integer.parseInt(text.trim());
                    int max = SkillConfig.getUltmineShapeMaxDepth(selectedShape);
                    UltmineClientConfig.setShapeDepth(selectedShape, Math.max(1, Math.min(max, val)));
                } catch (NumberFormatException ignored) {}
            });
            this.addDrawableChild(depthField);
        }

        // Length text field (only if maxLength > 1)
        lengthField = null;
        if (maxLength > 1) {
            int lengthRowY = shapeConfigY + (maxDepth > 1 ? 54 : 30);
            int fieldX = centerX + 20;

            lengthField = new TextFieldWidget(textRenderer, fieldX, lengthRowY, fieldW, 18, Text.empty());
            lengthField.setMaxLength(4);
            lengthField.setText(String.valueOf(currentLength));
            lengthField.setChangedListener(text -> {
                try {
                    int val = Integer.parseInt(text.trim());
                    int max = SkillConfig.getUltmineShapeMaxLength(selectedShape);
                    UltmineClientConfig.setShapeLength(selectedShape, Math.max(1, Math.min(max, val)));
                } catch (NumberFormatException ignored) {}
            });
            this.addDrawableChild(lengthField);
        }

        // Variant controls (only if shape has variants)
        int variantCount = UltmineShape.getVariantCount(selectedShape);
        if (variantCount > 1) {
            int variantRowY = shapeConfigY + 30;
            if (maxDepth > 1) variantRowY += 24;
            if (maxLength > 1) variantRowY += 24;

            int variantBtnW = 20;
            // Place ◀ and ▶ in the right-side area like depth/length, but with a wider value box
            int variantValueW = 100;
            int variantStartX = centerX + 20;
            int variantEndX = variantStartX + variantBtnW + ctrlGap + variantValueW + ctrlGap + variantBtnW;
            // If it overflows the panel, shift left
            int panelRight = panelX + panelW - PANEL_PADDING * 2;
            if (variantEndX > panelRight) {
                variantStartX -= (variantEndX - panelRight);
            }

            ButtonWidget variantLeft = ButtonWidget.builder(Text.literal("◀"), (b) -> {
                int v = UltmineClientConfig.getShapeVariant(selectedShape);
                int count = UltmineShape.getVariantCount(selectedShape);
                UltmineClientConfig.setShapeVariant(selectedShape, (v - 1 + count) % count);
                refreshScreen();
            }).dimensions(variantStartX, variantRowY, variantBtnW, 18).build();
            this.addDrawableChild(variantLeft);

            ButtonWidget variantRight = ButtonWidget.builder(Text.literal("▶"), (b) -> {
                int v = UltmineClientConfig.getShapeVariant(selectedShape);
                int count = UltmineShape.getVariantCount(selectedShape);
                UltmineClientConfig.setShapeVariant(selectedShape, (v + 1) % count);
                refreshScreen();
            }).dimensions(variantStartX + variantBtnW + ctrlGap + variantValueW + ctrlGap, variantRowY, variantBtnW, 18).build();
            this.addDrawableChild(variantRight);
        }

        // === BOTTOM BUTTONS ===
        int btnW = 90;
        int btnGap = 12;

        ButtonWidget saveBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.ultmine_config.save").formatted(Formatting.GREEN),
                (b) -> { UltmineClientConfig.save(); close(); })
                .dimensions(centerX - btnW - btnGap / 2, bottomY, btnW, 20)
                .build();
        this.addDrawableChild(saveBtn);

        ButtonWidget resetBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.ultmine_config.reset").formatted(Formatting.YELLOW),
                (b) -> { resetDefaults(); refreshScreen(); })
                .dimensions(centerX + btnGap / 2, bottomY, btnW, 20)
                .build();
        this.addDrawableChild(resetBtn);
    }

    private void calculateLayout() {
        panelW = Math.min(380, this.width - 20);
        panelX = (this.width - panelW) / 2;
        panelY = 10;

        toggleSectionY = panelY + HEADER_HEIGHT + 4;
        shapeSelectorY = toggleSectionY + 44;
        shapeConfigY = shapeSelectorY + 42;

        // Calculate shape config section height based on selected shape
        int configRows = 0;
        int maxDepth = SkillConfig.getUltmineShapeMaxDepth(selectedShape);
        int maxLength = SkillConfig.getUltmineShapeMaxLength(selectedShape);
        int variantCount = UltmineShape.getVariantCount(selectedShape);
        if (selectedShape == UltmineShape.LEGACY) {
            configRows = 3; // max_blocks, deepslate, tool_damage
        } else {
            if (maxDepth > 1) configRows++;
            if (maxLength > 1) configRows++;
            if (variantCount > 1) configRows++;
        }

        int shapeConfigH = 30 + configRows * 24 + 10;

        bottomY = shapeConfigY + shapeConfigH + SECTION_GAP;
        panelH = bottomY + 28 - panelY;

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
        UltmineClientConfig.setDropsToInventory(true);
        UltmineClientConfig.setSameBlockOnly(false);
        UltmineClientConfig.setXpDirectToPlayer(false);
        for (UltmineShape shape : UltmineShape.values()) {
            UltmineClientConfig.setShapeDepth(shape, -1);
            UltmineClientConfig.setShapeLength(shape, -1);
            UltmineClientConfig.setShapeVariant(shape, 0);
        }
    }

    private int getEffectiveDepth(UltmineShape shape) {
        int saved = UltmineClientConfig.getShapeDepth(shape);
        if (saved < 1) return SkillConfig.getUltmineShapeDefaultDepth(shape);
        return Math.min(saved, SkillConfig.getUltmineShapeMaxDepth(shape));
    }

    private int getEffectiveLength(UltmineShape shape) {
        int saved = UltmineClientConfig.getShapeLength(shape);
        if (saved < 1) return SkillConfig.getUltmineShapeDefaultLength(shape);
        return Math.min(saved, SkillConfig.getUltmineShapeMaxLength(shape));
    }

    // ===== RENDERING =====

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderGradientBackground(context);

        // Main panel
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, palette.panelBg());
        RenderingHelper.drawPanelBorder(context, panelX, panelY, panelW, panelH, palette.sectionBorder());
        RenderingHelper.renderCornerAccents(context, panelX, panelY, panelW, panelH, 6, palette.accentGold());

        renderHeader(context);
        renderToggleSection(context);
        renderShapeSelector(context);
        renderShapeConfig(context);

        // Widgets
        super.render(context, mouseX, mouseY, delta);

        // Overlays on top of buttons
        renderToggleButtonContent(context);
        renderShapeSelectorContent(context);
        renderShapeConfigValues(context);
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
                Text.translatable("murilloskills.ultmine_config.subtitle").copy().formatted(Formatting.GRAY),
                centerX, panelY + 26, palette.textMuted());
    }

    private void renderToggleSection(DrawContext context) {
        int centerX = this.width / 2;
        renderSectionDivider(context, toggleSectionY,
                Text.translatable("murilloskills.ultmine_config.section.toggles").getString());
    }

    private void renderToggleButtonContent(DrawContext context) {
        int toggleW = panelW - PANEL_PADDING * 4;
        int toggleGap = 4;
        int toggleBtnW = (toggleW - toggleGap * 2) / 3;
        int toggleStartX = this.width / 2 - toggleW / 2;
        int btnY = toggleSectionY + 16;

        // Drops to inventory
        renderToggleCard(context, toggleStartX, btnY, toggleBtnW,
                Text.translatable("murilloskills.ultmine_config.drops_to_inventory").getString(),
                UltmineClientConfig.isDropsToInventory());

        // XP Direct to Player
        renderToggleCard(context, toggleStartX + toggleBtnW + toggleGap, btnY, toggleBtnW,
                Text.translatable("murilloskills.ultmine_config.xp_direct").getString(),
                UltmineClientConfig.isXpDirectToPlayer());

        // Same block only
        renderToggleCard(context, toggleStartX + 2 * (toggleBtnW + toggleGap), btnY, toggleBtnW,
                Text.translatable("murilloskills.ultmine_config.same_block_only").getString(),
                UltmineClientConfig.isSameBlockOnly());
    }

    private void renderToggleCard(DrawContext context, int x, int y, int w, String label, boolean enabled) {
        int bg = enabled ? palette.sectionBgActive() : palette.sectionBg();
        context.fill(x + 1, y + 1, x + w - 1, y + 19, bg);

        if (enabled) {
            context.fill(x + 1, y + 18, x + w - 1, y + 20, palette.accentGreen());
        }

        String prefix = enabled ? "● " : "○ ";
        int color = enabled ? palette.textGreen() : palette.textGray();

        // Truncate label if needed
        int maxLabelW = w - 20;
        if (textRenderer.getWidth(label) > maxLabelW) {
            while (textRenderer.getWidth(label + "..") > maxLabelW && label.length() > 1) {
                label = label.substring(0, label.length() - 1);
            }
            label = label + "..";
        }

        context.drawCenteredTextWithShadow(textRenderer, prefix + label, x + w / 2, y + 6, color);
    }

    private void renderShapeSelector(DrawContext context) {
        renderSectionDivider(context, shapeSelectorY,
                Text.translatable("murilloskills.ultmine_config.section.shape").getString());
    }

    private void renderShapeSelectorContent(DrawContext context) {
        UltmineShape[] shapes = UltmineShape.values();
        int shapeCount = shapes.length;
        int shapeBtnW = Math.min(55, (panelW - PANEL_PADDING * 2 - (shapeCount - 1) * 4) / shapeCount);
        int totalShapesW = shapeCount * shapeBtnW + (shapeCount - 1) * 4;
        int shapeStartX = this.width / 2 - totalShapesW / 2;
        int btnY = shapeSelectorY + 16;

        for (int i = 0; i < shapeCount; i++) {
            UltmineShape shape = shapes[i];
            boolean active = shape == selectedShape;
            int x = shapeStartX + i * (shapeBtnW + 4);

            int bg = active ? palette.sectionBgActive() : palette.sectionBg();
            context.fill(x + 1, btnY + 1, x + shapeBtnW - 1, btnY + 17, bg);

            if (active) {
                context.fill(x + 1, btnY + 16, x + shapeBtnW - 1, btnY + 18, palette.textAqua());
            }

            // Shape short name
            String name = getShapeShortName(shape);
            int color = active ? palette.textAqua() : palette.textGray();
            int nameW = textRenderer.getWidth(name);
            if (nameW > shapeBtnW - 4) {
                // Further truncate
                while (textRenderer.getWidth(name + ".") > shapeBtnW - 4 && name.length() > 2) {
                    name = name.substring(0, name.length() - 1);
                }
                name = name + ".";
            }
            context.drawCenteredTextWithShadow(textRenderer, name, x + shapeBtnW / 2, btnY + 5, color);
        }
    }

    private void renderShapeConfig(DrawContext context) {
        renderSectionDivider(context, shapeConfigY,
                Text.translatable("murilloskills.ultmine_config.section.shape_config").getString());

        // Shape full name
        String shapeName = Text.translatable(selectedShape.getTranslationKey()).getString();
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(shapeName).formatted(Formatting.WHITE, Formatting.BOLD),
                this.width / 2, shapeConfigY + 16, palette.textWhite());
    }

    private void renderShapeConfigValues(DrawContext context) {
        int centerX = this.width / 2;
        int maxDepth = SkillConfig.getUltmineShapeMaxDepth(selectedShape);
        int maxLength = SkillConfig.getUltmineShapeMaxLength(selectedShape);
        int variantCount = UltmineShape.getVariantCount(selectedShape);

        int ctrlBtnW = 20;
        int ctrlGap = 6;
        int fieldW = 50;
        int labelX = panelX + PANEL_PADDING * 2;

        int currentRow = 0;

        // Depth row
        if (maxDepth > 1) {
            int rowY = shapeConfigY + 30 + currentRow * 24;

            String depthLabel = Text.translatable("murilloskills.ultmine_config.depth").getString();
            context.drawTextWithShadow(textRenderer, depthLabel, labelX, rowY + 5, palette.textLight());

            // Max hint after text field
            int hintX = centerX + 20 + fieldW + 4;
            context.drawTextWithShadow(textRenderer,
                    Text.literal("/ " + maxDepth).formatted(Formatting.GRAY),
                    hintX, rowY + 5, palette.textMuted());

            currentRow++;
        }

        // Length row
        if (maxLength > 1) {
            int rowY = shapeConfigY + 30 + currentRow * 24;

            String lengthLabel = Text.translatable("murilloskills.ultmine_config.length").getString();
            context.drawTextWithShadow(textRenderer, lengthLabel, labelX, rowY + 5, palette.textLight());

            int hintX = centerX + 20 + fieldW + 4;
            context.drawTextWithShadow(textRenderer,
                    Text.literal("/ " + maxLength).formatted(Formatting.GRAY),
                    hintX, rowY + 5, palette.textMuted());

            currentRow++;
        }

        // Variant row
        if (variantCount > 1) {
            int rowY = shapeConfigY + 30 + currentRow * 24;

            String varLabel = Text.translatable("murilloskills.ultmine_config.variant").getString();
            context.drawTextWithShadow(textRenderer, varLabel, labelX, rowY + 5, palette.textLight());

            int variant = UltmineClientConfig.getShapeVariant(selectedShape);
            String varName = Text.translatable(UltmineShape.getVariantTranslationKey(selectedShape, variant)).getString();

            // Match button layout from init(): variantStartX + btnW + gap ... + valueW ... + gap + btnW
            int variantValueW = 100;
            int variantStartX = centerX + 20;
            int variantEndX = variantStartX + ctrlBtnW + ctrlGap + variantValueW + ctrlGap + ctrlBtnW;
            int panelRight = panelX + panelW - PANEL_PADDING * 2;
            if (variantEndX > panelRight) {
                variantStartX -= (variantEndX - panelRight);
            }
            int valueCenterX = variantStartX + ctrlBtnW + ctrlGap + variantValueW / 2;

            // Background box for variant name
            int vBoxX = variantStartX + ctrlBtnW + ctrlGap;
            context.fill(vBoxX, rowY, vBoxX + variantValueW, rowY + 18, palette.sectionBg());
            RenderingHelper.drawPanelBorder(context, vBoxX, rowY, variantValueW, 18, palette.sectionBorder());

            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(varName).formatted(Formatting.AQUA),
                    valueCenterX, rowY + 5, palette.textAqua());
        }

        // Legacy mode: show relevant info instead of "no options"
        if (selectedShape == UltmineShape.LEGACY) {
            int infoY = shapeConfigY + 32;
            int infoLabelX = panelX + PANEL_PADDING * 2;
            int infoValueX = centerX + 20;

            // Max blocks
            String maxBlocksLabel = Text.translatable("murilloskills.ultmine_config.legacy.max_blocks").getString();
            context.drawTextWithShadow(textRenderer, maxBlocksLabel, infoLabelX, infoY, palette.textLight());
            context.drawTextWithShadow(textRenderer,
                    Text.literal(String.valueOf(SkillConfig.getVeinMinerMaxBlocks())).formatted(Formatting.AQUA),
                    infoValueX, infoY, palette.textAqua());

            // Deepslate variants
            infoY += 14;
            String deepslateLabel = Text.translatable("murilloskills.ultmine_config.legacy.deepslate").getString();
            context.drawTextWithShadow(textRenderer, deepslateLabel, infoLabelX, infoY, palette.textLight());
            boolean deepslate = SkillConfig.getVeinMinerMatchDeepslateVariants();
            context.drawTextWithShadow(textRenderer,
                    Text.literal(deepslate ? "ON" : "OFF").formatted(deepslate ? Formatting.GREEN : Formatting.RED),
                    infoValueX, infoY, deepslate ? palette.textGreen() : palette.statusCooldown());

            // Tool damage per block
            infoY += 14;
            String toolDmgLabel = Text.translatable("murilloskills.ultmine_config.legacy.tool_damage").getString();
            context.drawTextWithShadow(textRenderer, toolDmgLabel, infoLabelX, infoY, palette.textLight());
            boolean toolDmg = SkillConfig.getVeinMinerDamageToolPerBlock();
            context.drawTextWithShadow(textRenderer,
                    Text.literal(toolDmg ? "ON" : "OFF").formatted(toolDmg ? Formatting.GREEN : Formatting.RED),
                    infoValueX, infoY, toolDmg ? palette.textGreen() : palette.statusCooldown());
        } else if (maxDepth <= 1 && maxLength <= 1 && variantCount <= 1) {
            // If no configurable options for other shapes
            int noConfigY = shapeConfigY + 32;
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("murilloskills.ultmine_config.no_options").formatted(Formatting.GRAY),
                    centerX, noConfigY, palette.textMuted());
        }
    }

    private void renderSectionDivider(DrawContext context, int y, String title) {
        int centerX = this.width / 2;
        int titleW = textRenderer.getWidth(title);
        int lineW = (panelW - PANEL_PADDING * 2 - titleW - 20) / 2;

        context.fill(panelX + PANEL_PADDING, y + 3,
                panelX + PANEL_PADDING + lineW, y + 4, palette.dividerColor());
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(title).formatted(Formatting.GOLD),
                centerX, y - 1, palette.textGold());
        context.fill(centerX + titleW / 2 + 10, y + 3,
                panelX + panelW - PANEL_PADDING, y + 4, palette.dividerColor());
    }

    private String getShapeShortName(UltmineShape shape) {
        return switch (shape) {
            case S_3x3 -> "3x3";
            case R_2x1 -> "2x1";
            case LINE -> "Line";
            case STAIRS -> "Stair";
            case SQUARE_20x20_D1 -> "20x20";
            case LEGACY -> "Vein";
        };
    }

    @Override
    public void close() {
        UltmineClientConfig.save();
        // Sync current shape's config to server so variant/depth/length changes take effect
        syncActiveShapeToServer();
        MinecraftClient.getInstance().setScreen(parent);
    }

    private void syncActiveShapeToServer() {
        UltmineShape activeShape = UltmineClientState.getSelectedShape();
        int depth = getEffectiveDepth(activeShape);
        int length = getEffectiveLength(activeShape);
        int variant = UltmineClientConfig.getShapeVariant(activeShape);
        UltmineClientState.applyShapeDefaults(activeShape);
        ClientPlayNetworking.send(new UltmineShapeSelectC2SPayload(activeShape, depth, length, variant));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
