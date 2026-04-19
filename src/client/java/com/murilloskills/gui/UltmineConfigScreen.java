package com.murilloskills.gui;

import com.murilloskills.client.config.UltmineClientConfig;
import com.murilloskills.data.UltmineClientState;
import com.murilloskills.gui.renderer.RenderingHelper;
import com.murilloskills.network.MagnetConfigC2SPayload;
import com.murilloskills.network.TrashListSyncC2SPayload;
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

import java.util.List;

/**
 * Client-side Ultmine configuration screen.
 * Allows customizing per-shape depth/length/variant, global toggles, magnet, and trash list.
 */
public class UltmineConfigScreen extends Screen {

    private final Screen parent;
    private final ColorPalette palette = ColorPalette.premium();

    private static final int HEADER_HEIGHT = 50;
    private static final int SECTION_GAP = 14;
    private static final int PANEL_PADDING = 10;
    private static final int MAX_VISIBLE_TRASH = 4;

    // Selected shape for per-shape config
    private UltmineShape selectedShape = UltmineShape.S_3x3;

    // Text fields
    private TextFieldWidget depthField;
    private TextFieldWidget lengthField;
    private TextFieldWidget magnetRangeField;
    private TextFieldWidget trashItemField;

    // Trash scroll offset
    private int trashScrollOffset = 0;

    // Layout
    private int panelX, panelY, panelW, panelH;
    private int contentHeight;
    private int scrollOffset = 0;
    private int toggleSectionY;
    private int shapeSelectorY;
    private int shapeConfigY;
    private int magnetSectionY;
    private int trashSectionY;
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
        int oY = -scrollOffset; // offset for scrolling

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
        }).dimensions(toggleStartX, toggleSectionY + 16 + oY, toggleBtnW, 20).build();
        this.addDrawableChild(dropsBtn);

        // XP Direct to Player toggle
        ButtonWidget xpDirectBtn = ButtonWidget.builder(Text.empty(), (b) -> {
            UltmineClientConfig.toggleXpDirectToPlayer();
            ClientPlayNetworking.send(new XpDirectToggleC2SPayload(UltmineClientConfig.isXpDirectToPlayer()));
            refreshScreen();
        }).dimensions(toggleStartX + toggleBtnW + toggleGap, toggleSectionY + 16 + oY, toggleBtnW, 20).build();
        this.addDrawableChild(xpDirectBtn);

        // Same block only toggle
        ButtonWidget sameBlockBtn = ButtonWidget.builder(Text.empty(), (b) -> {
            UltmineClientConfig.toggleSameBlockOnly();
            refreshScreen();
        }).dimensions(toggleStartX + 2 * (toggleBtnW + toggleGap), toggleSectionY + 16 + oY, toggleBtnW, 20).build();
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
            }).dimensions(x, shapeSelectorY + 16 + oY, shapeBtnW, 18).build();
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

        depthField = null;
        if (maxDepth > 1) {
            int depthRowY = shapeConfigY + 30 + oY;
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

        lengthField = null;
        if (maxLength > 1) {
            int lengthRowY = shapeConfigY + (maxDepth > 1 ? 54 : 30) + oY;
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

        // Variant controls
        int variantCount = UltmineShape.getVariantCount(selectedShape);
        if (variantCount > 1) {
            int variantRowY = shapeConfigY + 30 + oY;
            if (maxDepth > 1) variantRowY += 24;
            if (maxLength > 1) variantRowY += 24;

            int variantBtnW = 20;
            int variantValueW = 100;
            int variantStartX = centerX + 20;
            int variantEndX = variantStartX + variantBtnW + ctrlGap + variantValueW + ctrlGap + variantBtnW;
            int panelRight = panelX + panelW - PANEL_PADDING * 2;
            if (variantEndX > panelRight) {
                variantStartX -= (variantEndX - panelRight);
            }

            ButtonWidget variantLeft = ButtonWidget.builder(Text.literal("\u25C0"), (b) -> {
                int v = UltmineClientConfig.getShapeVariant(selectedShape);
                int count = UltmineShape.getVariantCount(selectedShape);
                UltmineClientConfig.setShapeVariant(selectedShape, (v - 1 + count) % count);
                refreshScreen();
            }).dimensions(variantStartX, variantRowY, variantBtnW, 18).build();
            this.addDrawableChild(variantLeft);

            ButtonWidget variantRight = ButtonWidget.builder(Text.literal("\u25B6"), (b) -> {
                int v = UltmineClientConfig.getShapeVariant(selectedShape);
                int count = UltmineShape.getVariantCount(selectedShape);
                UltmineClientConfig.setShapeVariant(selectedShape, (v + 1) % count);
                refreshScreen();
            }).dimensions(variantStartX + variantBtnW + ctrlGap + variantValueW + ctrlGap, variantRowY, variantBtnW, 18).build();
            this.addDrawableChild(variantRight);
        }

        // === MAGNET SECTION ===
        int magnetToggleW = (panelW - PANEL_PADDING * 4) / 2 - 2;
        int magnetLeftX = centerX - (panelW - PANEL_PADDING * 4) / 2;

        ButtonWidget magnetToggleBtn = ButtonWidget.builder(Text.empty(), (b) -> {
            UltmineClientConfig.toggleMagnet();
            syncMagnetToServer();
            refreshScreen();
        }).dimensions(magnetLeftX, magnetSectionY + 16 + oY, magnetToggleW, 20).build();
        this.addDrawableChild(magnetToggleBtn);

        // Magnet range field
        int rangeFieldX = magnetLeftX + magnetToggleW + 4 + textRenderer.getWidth(
                Text.translatable("murilloskills.ultmine_config.magnet.range").getString() + " ") + 4;
        int rangeFieldW = 40;
        // Ensure it fits within panel
        int magnetRightAreaX = magnetLeftX + magnetToggleW + 4;
        magnetRangeField = new TextFieldWidget(textRenderer, magnetRightAreaX + 58, magnetSectionY + 17 + oY,
                rangeFieldW, 18, Text.empty());
        magnetRangeField.setMaxLength(2);
        magnetRangeField.setText(String.valueOf(UltmineClientConfig.getMagnetRange()));
        magnetRangeField.setChangedListener(text -> {
            try {
                int val = Integer.parseInt(text.trim());
                UltmineClientConfig.setMagnetRange(val);
            } catch (NumberFormatException ignored) {}
        });
        this.addDrawableChild(magnetRangeField);

        // === TRASH SECTION ===
        int browseBtnW = 80;
        int addBtnW = 36;
        int trashFieldW = panelW - PANEL_PADDING * 4 - browseBtnW - addBtnW - 8;
        int trashFieldX = panelX + PANEL_PADDING * 2;

        trashItemField = new TextFieldWidget(textRenderer, trashFieldX, trashSectionY + 16 + oY,
                trashFieldW, 18, Text.empty());
        trashItemField.setMaxLength(100);
        trashItemField.setPlaceholder(Text.literal("minecraft:cobblestone").formatted(Formatting.DARK_GRAY));
        this.addDrawableChild(trashItemField);

        ButtonWidget addTrashBtn = ButtonWidget.builder(
                Text.literal("+").formatted(Formatting.GREEN),
                (b) -> {
                    String itemId = trashItemField.getText().trim();
                    if (!itemId.isEmpty()) {
                        if (!itemId.contains(":")) {
                            itemId = "minecraft:" + itemId;
                        }
                        UltmineClientConfig.addTrashItem(itemId);
                        trashItemField.setText("");
                        syncTrashToServer();
                        refreshScreen();
                    }
                }).dimensions(trashFieldX + trashFieldW + 4, trashSectionY + 16 + oY, addBtnW, 18).build();
        this.addDrawableChild(addTrashBtn);

        ButtonWidget browseBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.ultmine_config.trash.browse").formatted(Formatting.AQUA),
                (b) -> MinecraftClient.getInstance().setScreen(new TrashItemPickerScreen(this)))
                .dimensions(trashFieldX + trashFieldW + 4 + addBtnW + 4, trashSectionY + 16 + oY, browseBtnW, 18)
                .build();
        this.addDrawableChild(browseBtn);

        // Trash item remove buttons
        List<String> trashItems = UltmineClientConfig.getTrashItems();
        int visibleCount = Math.min(MAX_VISIBLE_TRASH, trashItems.size() - trashScrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            int idx = i + trashScrollOffset;
            if (idx >= trashItems.size()) break;
            int itemY = trashSectionY + 38 + i * 16 + oY;

            final String itemToRemove = trashItems.get(idx);
            ButtonWidget removeBtn = ButtonWidget.builder(
                    Text.literal("\u2715").formatted(Formatting.RED),
                    (b) -> {
                        UltmineClientConfig.removeTrashItem(itemToRemove);
                        syncTrashToServer();
                        refreshScreen();
                    }).dimensions(panelX + panelW - PANEL_PADDING * 2 - 18, itemY, 18, 14).build();
            this.addDrawableChild(removeBtn);
        }

        // Trash scroll buttons if needed
        if (trashItems.size() > MAX_VISIBLE_TRASH) {
            int scrollBtnY = trashSectionY + 38 + MAX_VISIBLE_TRASH * 16 + oY;
            int scrollBtnW = 30;
            int scrollCenterX = centerX;

            if (trashScrollOffset > 0) {
                ButtonWidget scrollUpBtn = ButtonWidget.builder(Text.literal("\u25B2"), (b) -> {
                    trashScrollOffset = Math.max(0, trashScrollOffset - 1);
                    refreshScreen();
                }).dimensions(scrollCenterX - scrollBtnW - 2, scrollBtnY, scrollBtnW, 14).build();
                this.addDrawableChild(scrollUpBtn);
            }

            if (trashScrollOffset + MAX_VISIBLE_TRASH < trashItems.size()) {
                ButtonWidget scrollDownBtn = ButtonWidget.builder(Text.literal("\u25BC"), (b) -> {
                    trashScrollOffset = Math.min(trashItems.size() - MAX_VISIBLE_TRASH, trashScrollOffset + 1);
                    refreshScreen();
                }).dimensions(scrollCenterX + 2, scrollBtnY, scrollBtnW, 14).build();
                this.addDrawableChild(scrollDownBtn);
            }
        }

        // === BOTTOM BUTTONS ===
        int btnW = 90;
        int btnGap = 12;

        ButtonWidget saveBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.ultmine_config.save").formatted(Formatting.GREEN),
                (b) -> { UltmineClientConfig.save(); close(); })
                .dimensions(centerX - btnW - btnGap / 2, bottomY + oY, btnW, 20)
                .build();
        this.addDrawableChild(saveBtn);

        ButtonWidget resetBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.ultmine_config.reset").formatted(Formatting.YELLOW),
                (b) -> { resetDefaults(); refreshScreen(); })
                .dimensions(centerX + btnGap / 2, bottomY + oY, btnW, 20)
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

        // Calculate shape config section height
        int configRows = 0;
        int maxDepth = SkillConfig.getUltmineShapeMaxDepth(selectedShape);
        int maxLength = SkillConfig.getUltmineShapeMaxLength(selectedShape);
        int variantCount = UltmineShape.getVariantCount(selectedShape);
        if (selectedShape == UltmineShape.LEGACY) {
            configRows = 3;
        } else {
            if (maxDepth > 1) configRows++;
            if (maxLength > 1) configRows++;
            if (variantCount > 1) configRows++;
        }
        int shapeConfigH = 30 + configRows * 24 + 10;

        // Magnet section
        magnetSectionY = shapeConfigY + shapeConfigH + SECTION_GAP;

        // Trash section
        trashSectionY = magnetSectionY + 46 + SECTION_GAP;

        // Calculate trash section height
        int trashItems = UltmineClientConfig.getTrashItems().size();
        int visibleTrash = Math.min(MAX_VISIBLE_TRASH, trashItems);
        int trashH = 38 + visibleTrash * 16 + (trashItems > MAX_VISIBLE_TRASH ? 18 : 0) + 8;

        bottomY = trashSectionY + trashH + SECTION_GAP;
        contentHeight = bottomY + 28;
        panelH = contentHeight - panelY;

        // Clamp scroll
        int maxScroll = Math.max(0, contentHeight - (this.height - 4));
        scrollOffset = Math.min(scrollOffset, maxScroll);

        if (panelY + panelH - scrollOffset > this.height - 4) {
            panelH = this.height - 4 - panelY + scrollOffset;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, contentHeight - (this.height - 4));
        if (maxScroll > 0) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (verticalAmount * 12)));
            refreshScreen();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void refreshScreen() {
        clearChildren();
        init();
    }

    private void resetDefaults() {
        UltmineClientConfig.setDropsToInventory(true);
        UltmineClientConfig.setSameBlockOnly(false);
        UltmineClientConfig.setXpDirectToPlayer(false);
        UltmineClientConfig.setMagnetEnabled(false);
        UltmineClientConfig.setMagnetRange(8);
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

    private void syncMagnetToServer() {
        ClientPlayNetworking.send(new MagnetConfigC2SPayload(
                UltmineClientConfig.isMagnetEnabled(),
                UltmineClientConfig.getMagnetRange()));
    }

    private void syncTrashToServer() {
        ClientPlayNetworking.send(new TrashListSyncC2SPayload(UltmineClientConfig.getTrashItems()));
    }

    // ===== RENDERING =====

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderGradientBackground(context);

        int oY = -scrollOffset;

        // Main panel
        int visiblePanelH = Math.min(panelH, this.height - 4 - panelY);
        context.fill(panelX, panelY, panelX + panelW, panelY + visiblePanelH, palette.panelBg());
        RenderingHelper.drawPanelBorder(context, panelX, panelY, panelW, visiblePanelH, palette.sectionBorder());
        RenderingHelper.renderCornerAccents(context, panelX, panelY, panelW, visiblePanelH, 6, palette.accentGold());

        // Enable scissor to clip content to panel
        context.enableScissor(panelX, panelY, panelX + panelW, panelY + visiblePanelH);

        renderHeader(context, oY);
        renderToggleSection(context, oY);
        renderShapeSelector(context, oY);
        renderShapeConfig(context, oY);
        renderMagnetSection(context, oY);
        renderTrashSection(context, oY);

        // Widgets (must be rendered inside scissor too)
        super.render(context, mouseX, mouseY, delta);

        // Overlays on top of buttons
        renderToggleButtonContent(context, oY);
        renderShapeSelectorContent(context, oY);
        renderShapeConfigValues(context, oY);
        renderMagnetContent(context, oY);
        renderTrashContent(context, oY);

        context.disableScissor();

        // Scroll indicator
        int maxScroll = Math.max(0, contentHeight - (this.height - 4));
        if (maxScroll > 0) {
            int barH = Math.max(10, visiblePanelH * visiblePanelH / contentHeight);
            int barY = panelY + (int) ((float) scrollOffset / maxScroll * (visiblePanelH - barH));
            int barX = panelX + panelW - 4;
            context.fill(barX, barY, barX + 3, barY + barH, palette.scrollbarFg());
        }
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

    private void renderHeader(DrawContext context, int oY) {
        int centerX = this.width / 2;
        int headerBottom = panelY + HEADER_HEIGHT + oY;

        context.fill(panelX + 1, panelY + 1 + oY, panelX + panelW - 1, headerBottom, palette.panelBgHeader());

        int lineW = panelW - PANEL_PADDING * 4;
        context.fill(centerX - lineW / 2, headerBottom - 1, centerX + lineW / 2, headerBottom, palette.accentGold());

        context.drawCenteredTextWithShadow(textRenderer,
                this.title.copy().formatted(Formatting.GOLD, Formatting.BOLD),
                centerX, panelY + 10 + oY, palette.textGold());

        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("murilloskills.ultmine_config.subtitle").copy().formatted(Formatting.GRAY),
                centerX, panelY + 26 + oY, palette.textMuted());
    }

    private void renderToggleSection(DrawContext context, int oY) {
        renderSectionDivider(context, toggleSectionY + oY,
                Text.translatable("murilloskills.ultmine_config.section.toggles").getString());
    }

    private void renderToggleButtonContent(DrawContext context, int oY) {
        int toggleW = panelW - PANEL_PADDING * 4;
        int toggleGap = 4;
        int toggleBtnW = (toggleW - toggleGap * 2) / 3;
        int toggleStartX = this.width / 2 - toggleW / 2;
        int btnY = toggleSectionY + 16 + oY;

        renderToggleCard(context, toggleStartX, btnY, toggleBtnW,
                Text.translatable("murilloskills.ultmine_config.drops_to_inventory").getString(),
                UltmineClientConfig.isDropsToInventory());

        renderToggleCard(context, toggleStartX + toggleBtnW + toggleGap, btnY, toggleBtnW,
                Text.translatable("murilloskills.ultmine_config.xp_direct").getString(),
                UltmineClientConfig.isXpDirectToPlayer());

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

        String prefix = enabled ? "\u25CF " : "\u25CB ";
        int color = enabled ? palette.textGreen() : palette.textGray();

        int maxLabelW = w - 20;
        if (textRenderer.getWidth(label) > maxLabelW) {
            while (textRenderer.getWidth(label + "..") > maxLabelW && label.length() > 1) {
                label = label.substring(0, label.length() - 1);
            }
            label = label + "..";
        }

        context.drawCenteredTextWithShadow(textRenderer, prefix + label, x + w / 2, y + 6, color);
    }

    private void renderShapeSelector(DrawContext context, int oY) {
        renderSectionDivider(context, shapeSelectorY + oY,
                Text.translatable("murilloskills.ultmine_config.section.shape").getString());
    }

    private void renderShapeSelectorContent(DrawContext context, int oY) {
        UltmineShape[] shapes = UltmineShape.values();
        int shapeCount = shapes.length;
        int shapeBtnW = Math.min(55, (panelW - PANEL_PADDING * 2 - (shapeCount - 1) * 4) / shapeCount);
        int totalShapesW = shapeCount * shapeBtnW + (shapeCount - 1) * 4;
        int shapeStartX = this.width / 2 - totalShapesW / 2;
        int btnY = shapeSelectorY + 16 + oY;

        for (int i = 0; i < shapeCount; i++) {
            UltmineShape shape = shapes[i];
            boolean active = shape == selectedShape;
            int x = shapeStartX + i * (shapeBtnW + 4);

            int bg = active ? palette.sectionBgActive() : palette.sectionBg();
            context.fill(x + 1, btnY + 1, x + shapeBtnW - 1, btnY + 17, bg);

            if (active) {
                context.fill(x + 1, btnY + 16, x + shapeBtnW - 1, btnY + 18, palette.textAqua());
            }

            String name = getShapeShortName(shape);
            int color = active ? palette.textAqua() : palette.textGray();
            int nameW = textRenderer.getWidth(name);
            if (nameW > shapeBtnW - 4) {
                while (textRenderer.getWidth(name + ".") > shapeBtnW - 4 && name.length() > 2) {
                    name = name.substring(0, name.length() - 1);
                }
                name = name + ".";
            }
            context.drawCenteredTextWithShadow(textRenderer, name, x + shapeBtnW / 2, btnY + 5, color);
        }
    }

    private void renderShapeConfig(DrawContext context, int oY) {
        renderSectionDivider(context, shapeConfigY + oY,
                Text.translatable("murilloskills.ultmine_config.section.shape_config").getString());

        String shapeName = Text.translatable(selectedShape.getTranslationKey()).getString();
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(shapeName).formatted(Formatting.WHITE, Formatting.BOLD),
                this.width / 2, shapeConfigY + 16 + oY, palette.textWhite());
    }

    private void renderShapeConfigValues(DrawContext context, int oY) {
        int centerX = this.width / 2;
        int maxDepth = SkillConfig.getUltmineShapeMaxDepth(selectedShape);
        int maxLength = SkillConfig.getUltmineShapeMaxLength(selectedShape);
        int variantCount = UltmineShape.getVariantCount(selectedShape);

        int ctrlBtnW = 20;
        int ctrlGap = 6;
        int fieldW = 50;
        int labelX = panelX + PANEL_PADDING * 2;

        int currentRow = 0;

        if (maxDepth > 1) {
            int rowY = shapeConfigY + 30 + currentRow * 24 + oY;
            String depthLabel = Text.translatable("murilloskills.ultmine_config.depth").getString();
            context.drawTextWithShadow(textRenderer, depthLabel, labelX, rowY + 5, palette.textLight());
            int hintX = centerX + 20 + fieldW + 4;
            context.drawTextWithShadow(textRenderer,
                    Text.literal("/ " + maxDepth).formatted(Formatting.GRAY),
                    hintX, rowY + 5, palette.textMuted());
            currentRow++;
        }

        if (maxLength > 1) {
            int rowY = shapeConfigY + 30 + currentRow * 24 + oY;
            String lengthLabel = Text.translatable("murilloskills.ultmine_config.length").getString();
            context.drawTextWithShadow(textRenderer, lengthLabel, labelX, rowY + 5, palette.textLight());
            int hintX = centerX + 20 + fieldW + 4;
            context.drawTextWithShadow(textRenderer,
                    Text.literal("/ " + maxLength).formatted(Formatting.GRAY),
                    hintX, rowY + 5, palette.textMuted());
            currentRow++;
        }

        if (variantCount > 1) {
            int rowY = shapeConfigY + 30 + currentRow * 24 + oY;
            String varLabel = Text.translatable("murilloskills.ultmine_config.variant").getString();
            context.drawTextWithShadow(textRenderer, varLabel, labelX, rowY + 5, palette.textLight());

            int variant = UltmineClientConfig.getShapeVariant(selectedShape);
            String varName = Text.translatable(UltmineShape.getVariantTranslationKey(selectedShape, variant)).getString();

            int variantValueW = 100;
            int variantStartX = centerX + 20;
            int variantEndX = variantStartX + ctrlBtnW + ctrlGap + variantValueW + ctrlGap + ctrlBtnW;
            int panelRight = panelX + panelW - PANEL_PADDING * 2;
            if (variantEndX > panelRight) {
                variantStartX -= (variantEndX - panelRight);
            }
            int valueCenterX = variantStartX + ctrlBtnW + ctrlGap + variantValueW / 2;

            int vBoxX = variantStartX + ctrlBtnW + ctrlGap;
            context.fill(vBoxX, rowY, vBoxX + variantValueW, rowY + 18, palette.sectionBg());
            RenderingHelper.drawPanelBorder(context, vBoxX, rowY, variantValueW, 18, palette.sectionBorder());

            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(varName).formatted(Formatting.AQUA),
                    valueCenterX, rowY + 5, palette.textAqua());
        }

        if (selectedShape == UltmineShape.LEGACY) {
            int infoY = shapeConfigY + 32 + oY;
            int infoLabelX = panelX + PANEL_PADDING * 2;
            int infoValueX = centerX + 20;

            String maxBlocksLabel = Text.translatable("murilloskills.ultmine_config.legacy.max_blocks").getString();
            context.drawTextWithShadow(textRenderer, maxBlocksLabel, infoLabelX, infoY, palette.textLight());
            context.drawTextWithShadow(textRenderer,
                    Text.literal(String.valueOf(SkillConfig.getVeinMinerMaxBlocks())).formatted(Formatting.AQUA),
                    infoValueX, infoY, palette.textAqua());

            infoY += 14;
            String deepslateLabel = Text.translatable("murilloskills.ultmine_config.legacy.deepslate").getString();
            context.drawTextWithShadow(textRenderer, deepslateLabel, infoLabelX, infoY, palette.textLight());
            boolean deepslate = SkillConfig.getVeinMinerMatchDeepslateVariants();
            context.drawTextWithShadow(textRenderer,
                    Text.literal(deepslate ? "ON" : "OFF").formatted(deepslate ? Formatting.GREEN : Formatting.RED),
                    infoValueX, infoY, deepslate ? palette.textGreen() : palette.statusCooldown());

            infoY += 14;
            String toolDmgLabel = Text.translatable("murilloskills.ultmine_config.legacy.tool_damage").getString();
            context.drawTextWithShadow(textRenderer, toolDmgLabel, infoLabelX, infoY, palette.textLight());
            boolean toolDmg = SkillConfig.getVeinMinerDamageToolPerBlock();
            context.drawTextWithShadow(textRenderer,
                    Text.literal(toolDmg ? "ON" : "OFF").formatted(toolDmg ? Formatting.GREEN : Formatting.RED),
                    infoValueX, infoY, toolDmg ? palette.textGreen() : palette.statusCooldown());
        } else if (maxDepth <= 1 && maxLength <= 1 && variantCount <= 1) {
            int noConfigY = shapeConfigY + 32 + oY;
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("murilloskills.ultmine_config.no_options").formatted(Formatting.GRAY),
                    centerX, noConfigY, palette.textMuted());
        }
    }

    // ===== MAGNET SECTION =====

    private void renderMagnetSection(DrawContext context, int oY) {
        renderSectionDivider(context, magnetSectionY + oY,
                Text.translatable("murilloskills.ultmine_config.section.magnet").getString());
    }

    private void renderMagnetContent(DrawContext context, int oY) {
        boolean magnetOn = UltmineClientConfig.isMagnetEnabled();
        int magnetToggleW = (panelW - PANEL_PADDING * 4) / 2 - 2;
        int magnetLeftX = this.width / 2 - (panelW - PANEL_PADDING * 4) / 2;
        int btnY = magnetSectionY + 16 + oY;

        // Magnet toggle card
        renderToggleCard(context, magnetLeftX, btnY, magnetToggleW,
                Text.translatable("murilloskills.ultmine_config.magnet.toggle").getString(),
                magnetOn);

        // Range label
        int magnetRightAreaX = magnetLeftX + magnetToggleW + 4;
        String rangeLabel = Text.translatable("murilloskills.ultmine_config.magnet.range").getString();
        context.drawTextWithShadow(textRenderer, rangeLabel, magnetRightAreaX, btnY + 6, palette.textLight());

        // Range max hint
        int hintX = magnetRightAreaX + 58 + 40 + 4;
        context.drawTextWithShadow(textRenderer,
                Text.literal("/ 32").formatted(Formatting.GRAY),
                hintX, btnY + 6, palette.textMuted());
    }

    // ===== TRASH SECTION =====

    private void renderTrashSection(DrawContext context, int oY) {
        renderSectionDivider(context, trashSectionY + oY,
                Text.translatable("murilloskills.ultmine_config.section.trash").getString());
    }

    private void renderTrashContent(DrawContext context, int oY) {
        List<String> trashItems = UltmineClientConfig.getTrashItems();
        int labelX = panelX + PANEL_PADDING * 2;

        if (trashItems.isEmpty()) {
            context.drawTextWithShadow(textRenderer,
                    Text.translatable("murilloskills.ultmine_config.trash.empty").formatted(Formatting.GRAY),
                    labelX, trashSectionY + 40 + oY, palette.textMuted());
        } else {
            int visibleCount = Math.min(MAX_VISIBLE_TRASH, trashItems.size() - trashScrollOffset);
            for (int i = 0; i < visibleCount; i++) {
                int idx = i + trashScrollOffset;
                if (idx >= trashItems.size()) break;
                int itemY = trashSectionY + 38 + i * 16 + oY;

                String itemId = trashItems.get(idx);
                // Shorten display: remove "minecraft:" prefix for vanilla items
                String display = itemId.startsWith("minecraft:") ? itemId.substring(10) : itemId;
                int maxW = panelW - PANEL_PADDING * 4 - 24;
                if (textRenderer.getWidth(display) > maxW) {
                    while (textRenderer.getWidth(display + "..") > maxW && display.length() > 1) {
                        display = display.substring(0, display.length() - 1);
                    }
                    display = display + "..";
                }

                int color = (idx % 2 == 0) ? palette.textLight() : palette.textGray();
                context.drawTextWithShadow(textRenderer, "\u2022 " + display, labelX, itemY + 3, color);
            }

            // Count indicator if scrolling
            if (trashItems.size() > MAX_VISIBLE_TRASH) {
                int countY = trashSectionY + 38 + MAX_VISIBLE_TRASH * 16 + oY;
                String countText = (trashScrollOffset + 1) + "-"
                        + Math.min(trashScrollOffset + MAX_VISIBLE_TRASH, trashItems.size())
                        + " / " + trashItems.size();
                context.drawCenteredTextWithShadow(textRenderer,
                        Text.literal(countText).formatted(Formatting.DARK_GRAY),
                        this.width / 2, countY + 4, palette.textMuted());
            }
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
        syncActiveShapeToServer();
        syncMagnetToServer();
        syncTrashToServer();
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
