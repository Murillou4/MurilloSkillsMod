package com.murilloskills.gui;

import com.murilloskills.client.config.UltmineClientConfig;
import com.murilloskills.data.UltmineClientState;
import com.murilloskills.gui.renderer.RenderingHelper;
import com.murilloskills.network.MagnetConfigC2SPayload;
import com.murilloskills.network.TrashListSyncC2SPayload;
import com.murilloskills.network.UltmineClassicBlockListSyncC2SPayload;
import com.murilloskills.network.UltmineShapeSelectC2SPayload;
import com.murilloskills.network.VeinMinerDropsToggleC2SPayload;
import com.murilloskills.network.VeinMinerStorageDropToggleC2SPayload;
import com.murilloskills.network.XpDirectToggleC2SPayload;
import com.murilloskills.skills.UltmineShape;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Client-side Ultmine configuration screen.
 * Allows customizing per-shape depth/length/variant, global toggles, magnet, trash list, and classic block lock list.
 */
public class UltmineConfigScreen extends Screen {

    private final Screen parent;
    private final ColorPalette palette = ColorPalette.premium();

    private static final int HEADER_HEIGHT = 38;
    private static final int SECTION_GAP = 10;
    private static final int PANEL_PADDING = 10;
    private static final int MAX_VISIBLE_TRASH = 4;
    private static final int MAX_VISIBLE_CLASSIC_BLOCKS = 4;
    private static final int SCROLLBAR_WIDTH = 7;

    // Selected shape for per-shape config
    private UltmineShape selectedShape = UltmineShape.S_3x3;

    // Text fields
    private TextFieldWidget depthField;
    private TextFieldWidget lengthField;
    private TextFieldWidget magnetRangeField;
    private TextFieldWidget trashItemField;
    private TextFieldWidget classicBlockField;

    // Trash scroll offset
    private int trashScrollOffset = 0;
    // Classic blocked blocks scroll offset
    private int classicBlockScrollOffset = 0;

    // Layout
    private int panelX, panelY, panelW, panelH;
    private int contentHeight;
    private int scrollOffset = 0;
    private int toggleSectionY;
    private int shapeSelectorY;
    private int shapeConfigY;
    private int magnetSectionY;
    private int trashSectionY;
    private int classicBlockSectionY;
    private int bottomY;

    // Two-column responsive layout
    private boolean twoColumn;
    private int leftColX, leftColW;
    private int rightColX, rightColW;
    private boolean showClassicBlockSection; // only LEGACY shape uses this list

    // Section "anchor" — set before rendering / placing widgets for each section
    private int secX, secW, secCx;

    // Scrollbar drag state
    private boolean draggingScrollbar = false;
    private int scrollbarThumbY = 0;
    private int scrollbarThumbH = 0;
    private int scrollbarTrackY = 0;
    private int scrollbarTrackH = 0;
    private double scrollbarDragGrabOffset = 0;

    private void setSectionAnchor(int x, int w) {
        this.secX = x;
        this.secW = w;
        this.secCx = x + w / 2;
    }

    public UltmineConfigScreen(Screen parent) {
        super(Text.translatable("murilloskills.ultmine_config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        selectedShape = UltmineClientConfig.getSelectedShape();
        super.init();
        calculateLayout();

        int oY = -scrollOffset; // offset for scrolling

        // === LEFT COLUMN (or single column) ===
        setSectionAnchor(leftColX, leftColW);

        // === GLOBAL TOGGLES ===
        int toggleW = secW - PANEL_PADDING * 2;
        int toggleGap = 4;
        int toggleBtnW = (toggleW - toggleGap * 3) / 4;
        int toggleStartX = secCx - toggleW / 2;
        int toggleY = toggleSectionY + 16 + oY;

        // Drops to Inventory toggle
        ButtonWidget dropsBtn = ButtonWidget.builder(Text.empty(), (b) -> {
            UltmineClientConfig.toggleDropsToInventory();
            ClientPlayNetworking.send(new VeinMinerDropsToggleC2SPayload());
            refreshScreen();
        }).dimensions(toggleStartX, toggleY, toggleBtnW, 20).build();
        this.addDrawableChild(dropsBtn);

        // Drops to Storage (Tom's Storage Wireless Terminal) toggle
        ButtonWidget storageDropsBtn = ButtonWidget.builder(Text.empty(), (b) -> {
            UltmineClientConfig.toggleDropsToStorage();
            ClientPlayNetworking.send(new VeinMinerStorageDropToggleC2SPayload(UltmineClientConfig.isDropsToStorage()));
            refreshScreen();
        }).dimensions(toggleStartX + (toggleBtnW + toggleGap), toggleY, toggleBtnW, 20).build();
        this.addDrawableChild(storageDropsBtn);

        // XP Direct to Player toggle
        ButtonWidget xpDirectBtn = ButtonWidget.builder(Text.empty(), (b) -> {
            UltmineClientConfig.toggleXpDirectToPlayer();
            ClientPlayNetworking.send(new XpDirectToggleC2SPayload(UltmineClientConfig.isXpDirectToPlayer()));
            refreshScreen();
        }).dimensions(toggleStartX + 2 * (toggleBtnW + toggleGap), toggleY, toggleBtnW, 20).build();
        this.addDrawableChild(xpDirectBtn);

        // Same block only toggle
        ButtonWidget sameBlockBtn = ButtonWidget.builder(Text.empty(), (b) -> {
            UltmineClientConfig.toggleSameBlockOnly();
            refreshScreen();
        }).dimensions(toggleStartX + 3 * (toggleBtnW + toggleGap), toggleY, toggleBtnW, 20).build();
        this.addDrawableChild(sameBlockBtn);

        // === SHAPE SELECTOR BUTTONS ===
        UltmineShape[] shapes = UltmineShape.values();
        int shapeCount = shapes.length;
        int shapeBtnW = Math.min(55, (secW - (shapeCount - 1) * 4) / shapeCount);
        int totalShapesW = shapeCount * shapeBtnW + (shapeCount - 1) * 4;
        int shapeStartX = secCx - totalShapesW / 2;

        for (int i = 0; i < shapeCount; i++) {
            final UltmineShape shape = shapes[i];
            int x = shapeStartX + i * (shapeBtnW + 4);
            ButtonWidget shapeBtn = ButtonWidget.builder(Text.empty(), (b) -> {
                selectActiveShape(shape);
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
            int fieldX = secCx + 20;

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
            int fieldX = secCx + 20;

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
            int variantValueW = Math.min(100, secW - PANEL_PADDING - 4 - 2 * variantBtnW - 2 * ctrlGap);
            if (variantValueW < 40) variantValueW = 40;
            int variantStartX = secCx + 20;
            int variantEndX = variantStartX + variantBtnW + ctrlGap + variantValueW + ctrlGap + variantBtnW;
            int colRight = secX + secW - 2;
            if (variantEndX > colRight) {
                variantStartX -= (variantEndX - colRight);
            }
            if (variantStartX < secX + 2) {
                variantStartX = secX + 2;
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
        int magnetInnerW = secW - PANEL_PADDING * 2;
        int magnetToggleW = magnetInnerW / 2 - 2;
        int magnetLeftX = secCx - magnetInnerW / 2;

        ButtonWidget magnetToggleBtn = ButtonWidget.builder(Text.empty(), (b) -> {
            UltmineClientConfig.toggleMagnet();
            syncMagnetToServer();
            refreshScreen();
        }).dimensions(magnetLeftX, magnetSectionY + 16 + oY, magnetToggleW, 20).build();
        this.addDrawableChild(magnetToggleBtn);

        // Magnet range field
        int rangeFieldW = 36;
        int magnetRightAreaX = magnetLeftX + magnetToggleW + 4;
        magnetRangeField = new TextFieldWidget(textRenderer, magnetRightAreaX + 50, magnetSectionY + 17 + oY,
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

        // === RIGHT COLUMN ===
        setSectionAnchor(rightColX, rightColW);

        // === TRASH SECTION ===
        int browseBtnW = Math.min(70, secW / 4);
        int addBtnW = 22;
        int trashFieldW = secW - PANEL_PADDING * 2 - browseBtnW - addBtnW - 8;
        if (trashFieldW < 60) trashFieldW = Math.max(40, secW - PANEL_PADDING * 2 - browseBtnW - addBtnW - 8);
        int trashFieldX = secX + PANEL_PADDING;

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
        int maxTrashScroll = Math.max(0, trashItems.size() - MAX_VISIBLE_TRASH);
        trashScrollOffset = Math.max(0, Math.min(trashScrollOffset, maxTrashScroll));
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
                    }).dimensions(secX + secW - PANEL_PADDING - 18, itemY, 18, 14).build();
            this.addDrawableChild(removeBtn);
        }

        // Trash scroll buttons if needed
        if (trashItems.size() > MAX_VISIBLE_TRASH) {
            int countStart = trashScrollOffset + 1;
            int countEnd = Math.min(trashScrollOffset + MAX_VISIBLE_TRASH, trashItems.size());
            String countText = countStart + "-" + countEnd + " / " + trashItems.size();
            int countHalfW = textRenderer.getWidth(countText) / 2;

            int scrollBtnY = trashSectionY + 38 + MAX_VISIBLE_TRASH * 16 + oY + 1;
            int scrollBtnW = 14;
            int leftBtnX = secCx - countHalfW - 8 - scrollBtnW;
            int rightBtnX = secCx + countHalfW + 8;

            if (trashScrollOffset > 0) {
                ButtonWidget scrollUpBtn = ButtonWidget.builder(Text.literal("\u25B2"), (b) -> {
                    trashScrollOffset = Math.max(0, trashScrollOffset - 1);
                    refreshScreen();
                }).dimensions(leftBtnX, scrollBtnY, scrollBtnW, 14).build();
                this.addDrawableChild(scrollUpBtn);
            }

            if (trashScrollOffset + MAX_VISIBLE_TRASH < trashItems.size()) {
                ButtonWidget scrollDownBtn = ButtonWidget.builder(Text.literal("\u25BC"), (b) -> {
                    trashScrollOffset = Math.min(trashItems.size() - MAX_VISIBLE_TRASH, trashScrollOffset + 1);
                    refreshScreen();
                }).dimensions(rightBtnX, scrollBtnY, scrollBtnW, 14).build();
                this.addDrawableChild(scrollDownBtn);
            }
        }

        // === CLASSIC BLOCK LOCK SECTION (only when LEGACY shape selected) ===
        classicBlockField = null;
        if (showClassicBlockSection) {
        int classicFieldW = secW - PANEL_PADDING * 2 - browseBtnW - addBtnW - 8;
        int classicFieldX = secX + PANEL_PADDING;

        classicBlockField = new TextFieldWidget(textRenderer, classicFieldX, classicBlockSectionY + 16 + oY,
                classicFieldW, 18, Text.empty());
        classicBlockField.setMaxLength(120);
        classicBlockField.setPlaceholder(Text.literal("minecraft:stone").formatted(Formatting.DARK_GRAY));
        this.addDrawableChild(classicBlockField);

        ButtonWidget addClassicBlockBtn = ButtonWidget.builder(
                Text.literal("+").formatted(Formatting.GREEN),
                (b) -> {
                    String blockId = classicBlockField.getText().trim();
                    if (!blockId.isEmpty()) {
                        if (!blockId.contains(":")) {
                            blockId = "minecraft:" + blockId;
                        }
                        UltmineClientConfig.addLegacyBlockedBlock(blockId);
                        classicBlockField.setText("");
                        syncClassicBlockedBlocksToServer();
                        refreshScreen();
                    }
                }).dimensions(classicFieldX + classicFieldW + 4, classicBlockSectionY + 16 + oY, addBtnW, 18).build();
        this.addDrawableChild(addClassicBlockBtn);

        ButtonWidget browseClassicBlocksBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.ultmine_config.classic_block_lock.browse").formatted(Formatting.AQUA),
                (b) -> MinecraftClient.getInstance().setScreen(new UltmineClassicBlockPickerScreen(this)))
                .dimensions(classicFieldX + classicFieldW + 4 + addBtnW + 4, classicBlockSectionY + 16 + oY, browseBtnW, 18)
                .build();
        this.addDrawableChild(browseClassicBlocksBtn);

        List<String> blockedBlocks = UltmineClientConfig.getLegacyBlockedBlocks();
        int maxClassicScroll = Math.max(0, blockedBlocks.size() - MAX_VISIBLE_CLASSIC_BLOCKS);
        classicBlockScrollOffset = Math.max(0, Math.min(classicBlockScrollOffset, maxClassicScroll));
        int visibleClassicCount = Math.min(MAX_VISIBLE_CLASSIC_BLOCKS, blockedBlocks.size() - classicBlockScrollOffset);
        for (int i = 0; i < visibleClassicCount; i++) {
            int idx = i + classicBlockScrollOffset;
            if (idx >= blockedBlocks.size()) {
                break;
            }
            int itemY = classicBlockSectionY + 38 + i * 16 + oY;

            final String blockToRemove = blockedBlocks.get(idx);
            ButtonWidget removeBtn = ButtonWidget.builder(
                    Text.literal("\u2715").formatted(Formatting.RED),
                    (b) -> {
                        UltmineClientConfig.removeLegacyBlockedBlock(blockToRemove);
                        syncClassicBlockedBlocksToServer();
                        refreshScreen();
                    }).dimensions(secX + secW - PANEL_PADDING - 18, itemY, 18, 14).build();
            this.addDrawableChild(removeBtn);
        }

        if (blockedBlocks.size() > MAX_VISIBLE_CLASSIC_BLOCKS) {
            int countStart = classicBlockScrollOffset + 1;
            int countEnd = Math.min(classicBlockScrollOffset + MAX_VISIBLE_CLASSIC_BLOCKS, blockedBlocks.size());
            String countText = countStart + "-" + countEnd + " / " + blockedBlocks.size();
            int countHalfW = textRenderer.getWidth(countText) / 2;

            int scrollBtnY = classicBlockSectionY + 38 + MAX_VISIBLE_CLASSIC_BLOCKS * 16 + oY + 1;
            int scrollBtnW = 14;
            int leftBtnX = secCx - countHalfW - 8 - scrollBtnW;
            int rightBtnX = secCx + countHalfW + 8;

            if (classicBlockScrollOffset > 0) {
                ButtonWidget scrollUpBtn = ButtonWidget.builder(Text.literal("\u25B2"), (b) -> {
                    classicBlockScrollOffset = Math.max(0, classicBlockScrollOffset - 1);
                    refreshScreen();
                }).dimensions(leftBtnX, scrollBtnY, scrollBtnW, 14).build();
                this.addDrawableChild(scrollUpBtn);
            }

            if (classicBlockScrollOffset + MAX_VISIBLE_CLASSIC_BLOCKS < blockedBlocks.size()) {
                ButtonWidget scrollDownBtn = ButtonWidget.builder(Text.literal("\u25BC"), (b) -> {
                    classicBlockScrollOffset = Math.min(blockedBlocks.size() - MAX_VISIBLE_CLASSIC_BLOCKS,
                            classicBlockScrollOffset + 1);
                    refreshScreen();
                }).dimensions(rightBtnX, scrollBtnY, scrollBtnW, 14).build();
                this.addDrawableChild(scrollDownBtn);
            }
        }
        } // end if (showClassicBlockSection)

        // === BOTTOM BUTTONS ===
        int btnW = 90;
        int btnGap = 12;
        int panelCenterX = panelX + panelW / 2;

        ButtonWidget saveBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.ultmine_config.save").formatted(Formatting.GREEN),
                (b) -> { UltmineClientConfig.save(); close(); })
                .dimensions(panelCenterX - btnW - btnGap / 2, bottomY + oY, btnW, 20)
                .build();
        this.addDrawableChild(saveBtn);

        ButtonWidget resetBtn = ButtonWidget.builder(
                Text.translatable("murilloskills.ultmine_config.reset").formatted(Formatting.YELLOW),
                (b) -> { resetDefaults(); refreshScreen(); })
                .dimensions(panelCenterX + btnGap / 2, bottomY + oY, btnW, 20)
                .build();
        this.addDrawableChild(resetBtn);
    }

    private void calculateLayout() {
        showClassicBlockSection = (selectedShape == UltmineShape.LEGACY);

        // Section heights (depend on shape and dynamic list sizes)
        int maxDepth = SkillConfig.getUltmineShapeMaxDepth(selectedShape);
        int maxLength = SkillConfig.getUltmineShapeMaxLength(selectedShape);
        int variantCount = UltmineShape.getVariantCount(selectedShape);
        int shapeConfigH;
        if (selectedShape == UltmineShape.LEGACY) {
            int variantH = (variantCount > 1) ? 24 : 0;
            int infoCardH = 3 * 14 + 10;
            shapeConfigH = 30 + variantH + infoCardH + 8;
        } else {
            int configRows = 0;
            if (maxDepth > 1) configRows++;
            if (maxLength > 1) configRows++;
            if (variantCount > 1) configRows++;
            int rowsH = configRows * 24;
            if (configRows == 0) rowsH = 18;
            shapeConfigH = 30 + rowsH + 8;
        }

        int trashItems = UltmineClientConfig.getTrashItems().size();
        int visibleTrash = Math.min(MAX_VISIBLE_TRASH, trashItems);
        int trashH = 38 + Math.max(visibleTrash, 1) * 16
                + (trashItems > MAX_VISIBLE_TRASH ? 18 : 0) + 6;

        int blockedBlocks = UltmineClientConfig.getLegacyBlockedBlocks().size();
        int visibleBlocked = Math.min(MAX_VISIBLE_CLASSIC_BLOCKS, blockedBlocks);
        int classicBlocksH = 38 + Math.max(visibleBlocked, 1) * 16
                + (blockedBlocks > MAX_VISIBLE_CLASSIC_BLOCKS ? 18 : 0) + 6;

        // Decide layout mode: 2-col when there's enough horizontal AND vertical room
        int compactGap = 8;
        int toggleSectH = 38;
        int shapeSelH = 36;
        int magnetH = 42;
        int leftColContentH = toggleSectH + compactGap + shapeSelH + compactGap + shapeConfigH + compactGap + magnetH;
        int rightColContentH = trashH + (showClassicBlockSection ? compactGap + classicBlocksH : 0);
        int twoColContentH = HEADER_HEIGHT + 4 + Math.max(leftColContentH, rightColContentH) + SECTION_GAP + 26;

        // Lower thresholds so 2-col triggers on typical Minecraft GUI scale 3 (~640x360)
        twoColumn = (this.width >= 460) && (this.height >= Math.min(340, twoColContentH + 6));

        if (twoColumn) {
            panelW = Math.min(620, this.width - 16);
            panelX = (this.width - panelW) / 2;
            panelY = Math.max(4, (this.height - twoColContentH) / 2);

            int innerGap = 12;
            int innerW = panelW - PANEL_PADDING * 2 - innerGap;
            leftColW = innerW / 2;
            rightColW = innerW - leftColW;
            leftColX = panelX + PANEL_PADDING;
            rightColX = leftColX + leftColW + innerGap;

            int colTop = panelY + HEADER_HEIGHT + 4;

            // Left column stack
            toggleSectionY = colTop;
            shapeSelectorY = toggleSectionY + toggleSectH + compactGap;
            shapeConfigY = shapeSelectorY + shapeSelH + compactGap;
            magnetSectionY = shapeConfigY + shapeConfigH + compactGap;
            int leftBottom = magnetSectionY + magnetH;

            // Right column stack
            trashSectionY = colTop;
            classicBlockSectionY = showClassicBlockSection
                    ? trashSectionY + trashH + compactGap
                    : -9999;
            int rightBottom = showClassicBlockSection
                    ? classicBlockSectionY + classicBlocksH
                    : trashSectionY + trashH;

            int contentBottom = Math.max(leftBottom, rightBottom);
            bottomY = contentBottom + SECTION_GAP;
            contentHeight = bottomY + 26;
            panelH = contentHeight - panelY;
        } else {
            panelW = Math.min(380, this.width - 16);
            panelX = (this.width - panelW) / 2;
            panelY = 8;

            leftColX = rightColX = panelX;
            leftColW = rightColW = panelW;

            toggleSectionY = panelY + HEADER_HEIGHT + 4;
            shapeSelectorY = toggleSectionY + toggleSectH + compactGap;
            shapeConfigY = shapeSelectorY + shapeSelH + compactGap;

            magnetSectionY = shapeConfigY + shapeConfigH + compactGap;
            trashSectionY = magnetSectionY + magnetH + compactGap;
            classicBlockSectionY = showClassicBlockSection
                    ? trashSectionY + trashH + compactGap
                    : -9999;

            int contentBottom = showClassicBlockSection
                    ? classicBlockSectionY + classicBlocksH
                    : trashSectionY + trashH;
            bottomY = contentBottom + SECTION_GAP;
            contentHeight = bottomY + 26;
            panelH = contentHeight - panelY;
        }

        // Clamp scroll
        int maxScroll = Math.max(0, contentHeight - (this.height - 4));
        scrollOffset = Math.min(scrollOffset, maxScroll);
        if (maxScroll == 0) {
            scrollOffset = 0;
        }

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

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() == 0 && scrollbarTrackH > 0) {
            int barX = panelX + panelW - SCROLLBAR_WIDTH - 1;
            // Generous hit test (extra px each side for easier grabbing)
            if (mouseX >= barX - 2 && mouseX <= barX + SCROLLBAR_WIDTH + 2
                    && mouseY >= scrollbarTrackY && mouseY <= scrollbarTrackY + scrollbarTrackH) {
                if (mouseY >= scrollbarThumbY && mouseY <= scrollbarThumbY + scrollbarThumbH) {
                    // Grab thumb at click offset
                    draggingScrollbar = true;
                    scrollbarDragGrabOffset = mouseY - scrollbarThumbY;
                } else {
                    // Click outside thumb -> jump-scroll, then start drag from thumb middle
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
        int maxScroll = Math.max(0, contentHeight - (this.height - 4));
        if (maxScroll <= 0 || scrollbarTrackH <= scrollbarThumbH) {
            return;
        }
        double newThumbTop = mouseY - scrollbarDragGrabOffset;
        double trackRange = scrollbarTrackH - scrollbarThumbH;
        double rel = (newThumbTop - scrollbarTrackY) / trackRange;
        rel = Math.max(0.0, Math.min(1.0, rel));
        int newOffset = (int) Math.round(rel * maxScroll);
        if (newOffset != scrollOffset) {
            scrollOffset = newOffset;
            refreshScreen();
        }
    }

    private void refreshScreen() {
        clearChildren();
        init();
    }

    private void resetDefaults() {
        UltmineClientConfig.setDropsToInventory(true);
        UltmineClientConfig.setDropsToStorage(false);
        UltmineClientConfig.setSameBlockOnly(false);
        UltmineClientConfig.setXpDirectToPlayer(false);
        UltmineClientConfig.setMagnetEnabled(false);
        UltmineClientConfig.setMagnetRange(8);
        for (UltmineShape shape : UltmineShape.values()) {
            UltmineClientConfig.setShapeDepth(shape, -1);
            UltmineClientConfig.setShapeLength(shape, -1);
            UltmineClientConfig.setShapeVariant(shape, 0);
        }
        selectedShape = UltmineShape.S_3x3;
        UltmineClientConfig.setSelectedShape(selectedShape);
        UltmineClientState.applyShapeDefaults(selectedShape);
        UltmineClientConfig.save();
    }

    private void selectActiveShape(UltmineShape shape) {
        selectedShape = shape == null ? UltmineShape.S_3x3 : shape;
        UltmineClientState.applyShapeDefaults(selectedShape);
        UltmineClientConfig.save();
        syncActiveShapeToServer();
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

    private void syncClassicBlockedBlocksToServer() {
        ClientPlayNetworking.send(new UltmineClassicBlockListSyncC2SPayload(
                UltmineClientConfig.getLegacyBlockedBlocks()));
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

        // Left column dividers/labels
        setSectionAnchor(leftColX, leftColW);
        renderToggleSection(context, oY);
        renderShapeSelector(context, oY);
        renderShapeConfig(context, oY);
        renderMagnetSection(context, oY);

        // Right column dividers/labels
        setSectionAnchor(rightColX, rightColW);
        renderTrashSection(context, oY);
        if (showClassicBlockSection) {
            renderClassicBlockSection(context, oY);
        }

        // Widgets (must be rendered inside scissor too)
        super.render(context, mouseX, mouseY, delta);

        // Left column overlays (text + cards drawn over button surfaces)
        setSectionAnchor(leftColX, leftColW);
        renderToggleButtonContent(context, oY);
        renderShapeSelectorContent(context, oY);
        renderShapeConfigValues(context, oY);
        renderMagnetContent(context, oY);

        // Right column overlays
        setSectionAnchor(rightColX, rightColW);
        renderTrashContent(context, oY);
        if (showClassicBlockSection) {
            renderClassicBlockContent(context, oY);
        }

        context.disableScissor();

        // Scroll indicator + drag track
        int maxScroll = Math.max(0, contentHeight - (this.height - 4));
        if (maxScroll > 0) {
            scrollbarTrackY = panelY + 2;
            scrollbarTrackH = visiblePanelH - 4;
            scrollbarThumbH = Math.max(20, scrollbarTrackH * visiblePanelH / contentHeight);
            scrollbarThumbY = scrollbarTrackY
                    + (int) ((float) scrollOffset / maxScroll * (scrollbarTrackH - scrollbarThumbH));
            int barX = panelX + panelW - SCROLLBAR_WIDTH - 1;
            // Track background (rounded inset)
            context.fill(barX, scrollbarTrackY, barX + SCROLLBAR_WIDTH, scrollbarTrackY + scrollbarTrackH,
                    palette.scrollbarBg());
            // Thumb (brighter when dragging)
            int thumbColor = draggingScrollbar ? palette.scrollbarActive() : palette.scrollbarFg();
            context.fill(barX, scrollbarThumbY, barX + SCROLLBAR_WIDTH, scrollbarThumbY + scrollbarThumbH,
                    thumbColor);
            // Inner highlight on thumb for visibility
            context.fill(barX + 1, scrollbarThumbY + 1, barX + SCROLLBAR_WIDTH - 1, scrollbarThumbY + 2,
                    0x40FFFFFF);
        } else {
            scrollbarTrackH = 0;
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
                centerX, panelY + 5 + oY, palette.textGold());

        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("murilloskills.ultmine_config.subtitle").copy().formatted(Formatting.GRAY),
                centerX, panelY + 19 + oY, palette.textMuted());
    }

    private void renderToggleSection(DrawContext context, int oY) {
        renderSectionDivider(context, toggleSectionY + oY,
                Text.translatable("murilloskills.ultmine_config.section.toggles").getString());
    }

    private void renderToggleButtonContent(DrawContext context, int oY) {
        int toggleW = secW - PANEL_PADDING * 2;
        int toggleGap = 4;
        int toggleBtnW = (toggleW - toggleGap * 3) / 4;
        int toggleStartX = secCx - toggleW / 2;
        int btnY = toggleSectionY + 16 + oY;

        renderToggleCard(context, toggleStartX, btnY, toggleBtnW,
                Text.translatable("murilloskills.ultmine_config.drops_to_inventory").getString(),
                UltmineClientConfig.isDropsToInventory());

        renderToggleCard(context, toggleStartX + (toggleBtnW + toggleGap), btnY, toggleBtnW,
                Text.translatable("murilloskills.ultmine_config.drops_to_storage").getString(),
                UltmineClientConfig.isDropsToStorage());

        renderToggleCard(context, toggleStartX + 2 * (toggleBtnW + toggleGap), btnY, toggleBtnW,
                Text.translatable("murilloskills.ultmine_config.xp_direct").getString(),
                UltmineClientConfig.isXpDirectToPlayer());

        renderToggleCard(context, toggleStartX + 3 * (toggleBtnW + toggleGap), btnY, toggleBtnW,
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
        int shapeBtnW = Math.min(55, (secW - (shapeCount - 1) * 4) / shapeCount);
        int totalShapesW = shapeCount * shapeBtnW + (shapeCount - 1) * 4;
        int shapeStartX = secCx - totalShapesW / 2;
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
                secCx, shapeConfigY + 16 + oY, palette.textWhite());
    }

    private void renderShapeConfigValues(DrawContext context, int oY) {
        int maxDepth = SkillConfig.getUltmineShapeMaxDepth(selectedShape);
        int maxLength = SkillConfig.getUltmineShapeMaxLength(selectedShape);
        int variantCount = UltmineShape.getVariantCount(selectedShape);

        int ctrlBtnW = 20;
        int ctrlGap = 6;
        int fieldW = 50;
        int labelX = secX + PANEL_PADDING;

        int currentRow = 0;

        if (maxDepth > 1) {
            int rowY = shapeConfigY + 30 + currentRow * 24 + oY;
            String depthLabel = Text.translatable("murilloskills.ultmine_config.depth").getString();
            context.drawTextWithShadow(textRenderer, depthLabel, labelX, rowY + 5, palette.textLight());
            int hintX = secCx + 20 + fieldW + 4;
            context.drawTextWithShadow(textRenderer,
                    Text.literal("/ " + maxDepth).formatted(Formatting.GRAY),
                    hintX, rowY + 5, palette.textMuted());
            currentRow++;
        }

        if (maxLength > 1) {
            int rowY = shapeConfigY + 30 + currentRow * 24 + oY;
            String lengthLabel = Text.translatable("murilloskills.ultmine_config.length").getString();
            context.drawTextWithShadow(textRenderer, lengthLabel, labelX, rowY + 5, palette.textLight());
            int hintX = secCx + 20 + fieldW + 4;
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

            int variantValueW = Math.min(100, secW - PANEL_PADDING - 4 - 2 * ctrlBtnW - 2 * ctrlGap);
            if (variantValueW < 40) variantValueW = 40;
            int variantStartX = secCx + 20;
            int variantEndX = variantStartX + ctrlBtnW + ctrlGap + variantValueW + ctrlGap + ctrlBtnW;
            int colRight = secX + secW - 2;
            if (variantEndX > colRight) {
                variantStartX -= (variantEndX - colRight);
            }
            if (variantStartX < secX + 2) {
                variantStartX = secX + 2;
            }
            int valueCenterX = variantStartX + ctrlBtnW + ctrlGap + variantValueW / 2;

            int vBoxX = variantStartX + ctrlBtnW + ctrlGap;
            context.fill(vBoxX, rowY, vBoxX + variantValueW, rowY + 18, palette.sectionBg());
            RenderingHelper.drawPanelBorder(context, vBoxX, rowY, variantValueW, 18, palette.sectionBorder());

            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(varName).formatted(Formatting.AQUA),
                    valueCenterX, rowY + 5, palette.textAqua());
            currentRow++;
        }

        if (selectedShape == UltmineShape.LEGACY) {
            int cardX = secX + 4;
            int cardW = secW - 8;
            int cardY = shapeConfigY + 30 + currentRow * 24 + oY;
            int cardH = 3 * 14 + 8;

            // Subtle info-card background + border
            context.fill(cardX, cardY, cardX + cardW, cardY + cardH, palette.cardBgSubtle());
            RenderingHelper.drawPanelBorder(context, cardX, cardY, cardW, cardH, palette.infoBoxBorder());

            int infoLabelX = cardX + 8;
            int infoValueX = cardX + cardW - 8;
            int rowY = cardY + 5;

            String maxBlocksLabel = Text.translatable("murilloskills.ultmine_config.legacy.max_blocks").getString();
            String maxBlocksValue = String.valueOf(SkillConfig.getVeinMinerMaxBlocks());
            context.drawTextWithShadow(textRenderer, maxBlocksLabel, infoLabelX, rowY, palette.textLight());
            context.drawTextWithShadow(textRenderer,
                    Text.literal(maxBlocksValue).formatted(Formatting.AQUA),
                    infoValueX - textRenderer.getWidth(maxBlocksValue), rowY, palette.textAqua());

            rowY += 14;
            String deepslateLabel = Text.translatable("murilloskills.ultmine_config.legacy.deepslate").getString();
            boolean deepslate = SkillConfig.getVeinMinerMatchDeepslateVariants();
            String deepslateValue = deepslate ? "ON" : "OFF";
            context.drawTextWithShadow(textRenderer, deepslateLabel, infoLabelX, rowY, palette.textLight());
            context.drawTextWithShadow(textRenderer,
                    Text.literal(deepslateValue).formatted(deepslate ? Formatting.GREEN : Formatting.RED),
                    infoValueX - textRenderer.getWidth(deepslateValue), rowY,
                    deepslate ? palette.textGreen() : palette.statusCooldown());

            rowY += 14;
            String toolDmgLabel = Text.translatable("murilloskills.ultmine_config.legacy.tool_damage").getString();
            boolean toolDmg = SkillConfig.getVeinMinerDamageToolPerBlock();
            String toolDmgValue = toolDmg ? "ON" : "OFF";
            context.drawTextWithShadow(textRenderer, toolDmgLabel, infoLabelX, rowY, palette.textLight());
            context.drawTextWithShadow(textRenderer,
                    Text.literal(toolDmgValue).formatted(toolDmg ? Formatting.GREEN : Formatting.RED),
                    infoValueX - textRenderer.getWidth(toolDmgValue), rowY,
                    toolDmg ? palette.textGreen() : palette.statusCooldown());
        } else if (maxDepth <= 1 && maxLength <= 1 && variantCount <= 1) {
            int noConfigY = shapeConfigY + 32 + oY;
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("murilloskills.ultmine_config.no_options").formatted(Formatting.GRAY),
                    secCx, noConfigY, palette.textMuted());
        }
    }

    // ===== MAGNET SECTION =====

    private void renderMagnetSection(DrawContext context, int oY) {
        renderSectionDivider(context, magnetSectionY + oY,
                Text.translatable("murilloskills.ultmine_config.section.magnet").getString());
    }

    private void renderMagnetContent(DrawContext context, int oY) {
        boolean magnetOn = UltmineClientConfig.isMagnetEnabled();
        int magnetInnerW = secW - PANEL_PADDING * 2;
        int magnetToggleW = magnetInnerW / 2 - 2;
        int magnetLeftX = secCx - magnetInnerW / 2;
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
        int hintX = magnetRightAreaX + 50 + 36 + 3;
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
        int labelX = secX + PANEL_PADDING + 2;

        if (trashItems.isEmpty()) {
            context.drawTextWithShadow(textRenderer,
                    Text.translatable("murilloskills.ultmine_config.trash.empty").formatted(Formatting.GRAY),
                    labelX, trashSectionY + 40 + oY, palette.textMuted());
        } else {
            int visibleCount = Math.min(MAX_VISIBLE_TRASH, trashItems.size() - trashScrollOffset);
            int rowX = secX + PANEL_PADDING;
            int rowW = secW - PANEL_PADDING * 2;
            for (int i = 0; i < visibleCount; i++) {
                int idx = i + trashScrollOffset;
                if (idx >= trashItems.size()) break;
                int itemY = trashSectionY + 38 + i * 16 + oY;

                // Zebra stripe (every other row gets subtle bg)
                if (i % 2 == 1) {
                    context.fill(rowX, itemY - 1, rowX + rowW, itemY + 14, palette.alternatingRowBg());
                }

                String itemId = trashItems.get(idx);
                // Shorten display: remove "minecraft:" prefix for vanilla items
                String display = itemId.startsWith("minecraft:") ? itemId.substring(10) : itemId;
                int maxW = secW - PANEL_PADDING * 2 - 24;
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
                        secCx, countY + 4, palette.textMuted());
            }
        }
    }

    // ===== CLASSIC BLOCK LOCK SECTION =====

    private void renderClassicBlockSection(DrawContext context, int oY) {
        renderSectionDivider(context, classicBlockSectionY + oY,
                Text.translatable("murilloskills.ultmine_config.section.classic_block_lock").getString());
    }

    private void renderClassicBlockContent(DrawContext context, int oY) {
        List<String> blockedBlocks = UltmineClientConfig.getLegacyBlockedBlocks();
        int labelX = secX + PANEL_PADDING + 2;

        if (blockedBlocks.isEmpty()) {
            context.drawTextWithShadow(textRenderer,
                    Text.translatable("murilloskills.ultmine_config.classic_block_lock.empty")
                            .formatted(Formatting.GRAY),
                    labelX, classicBlockSectionY + 40 + oY, palette.textMuted());
        } else {
            int visibleCount = Math.min(MAX_VISIBLE_CLASSIC_BLOCKS, blockedBlocks.size() - classicBlockScrollOffset);
            int rowX = secX + PANEL_PADDING;
            int rowW = secW - PANEL_PADDING * 2;
            for (int i = 0; i < visibleCount; i++) {
                int idx = i + classicBlockScrollOffset;
                if (idx >= blockedBlocks.size()) {
                    break;
                }
                int itemY = classicBlockSectionY + 38 + i * 16 + oY;

                if (i % 2 == 1) {
                    context.fill(rowX, itemY - 1, rowX + rowW, itemY + 14, palette.alternatingRowBg());
                }

                String blockId = blockedBlocks.get(idx);
                String display = blockId.startsWith("minecraft:") ? blockId.substring(10) : blockId;
                int maxW = secW - PANEL_PADDING * 2 - 24;
                if (textRenderer.getWidth(display) > maxW) {
                    while (textRenderer.getWidth(display + "..") > maxW && display.length() > 1) {
                        display = display.substring(0, display.length() - 1);
                    }
                    display = display + "..";
                }

                int color = (idx % 2 == 0) ? palette.textLight() : palette.textGray();
                context.drawTextWithShadow(textRenderer, "\u2022 " + display, labelX, itemY + 3, color);
            }

            if (blockedBlocks.size() > MAX_VISIBLE_CLASSIC_BLOCKS) {
                int countY = classicBlockSectionY + 38 + MAX_VISIBLE_CLASSIC_BLOCKS * 16 + oY;
                String countText = (classicBlockScrollOffset + 1) + "-"
                        + Math.min(classicBlockScrollOffset + MAX_VISIBLE_CLASSIC_BLOCKS, blockedBlocks.size())
                        + " / " + blockedBlocks.size();
                context.drawCenteredTextWithShadow(textRenderer,
                        Text.literal(countText).formatted(Formatting.DARK_GRAY),
                        secCx, countY + 4, palette.textMuted());
            }
        }
    }

    private void renderSectionDivider(DrawContext context, int y, String title) {
        int titleW = textRenderer.getWidth(title);
        int sideMargin = 4;
        int lineLeftStart = secX + sideMargin;
        int lineLeftEnd = secCx - titleW / 2 - 8;
        int lineRightStart = secCx + titleW / 2 + 8;
        int lineRightEnd = secX + secW - sideMargin;

        if (lineLeftEnd > lineLeftStart) {
            context.fill(lineLeftStart, y + 3, lineLeftEnd, y + 4, palette.dividerColor());
        }
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(title).formatted(Formatting.GOLD),
                secCx, y - 1, palette.textGold());
        if (lineRightEnd > lineRightStart) {
            context.fill(lineRightStart, y + 3, lineRightEnd, y + 4, palette.dividerColor());
        }
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
        syncClassicBlockedBlocksToServer();
        MinecraftClient.getInstance().setScreen(parent);
    }

    private void syncActiveShapeToServer() {
        UltmineShape activeShape = UltmineClientConfig.getSelectedShape();
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
