package com.murilloskills.forge112.client.gui;

import com.murilloskills.forge112.client.config.ClientUltmineConfig;
import com.murilloskills.forge112.network.ModNetwork112;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.List;

import static com.murilloskills.forge112.client.gui.Forge112UiSupport.*;

public final class UltmineConfigGui112 extends GuiScreen {
    private static final int SAVE = 28001;
    private static final int RESET = 28002;
    private static final int SHAPE_BASE = 28100;
    private static final int DROPS = 28200;
    private static final int STORAGE = 28201;
    private static final int XP = 28202;
    private static final int SAME = 28203;
    private static final int MAGNET = 28204;
    private static final int VARIANT_LEFT = 28304;
    private static final int VARIANT_RIGHT = 28305;
    private static final int TRASH_ADD = 28400;
    private static final int TRASH_BROWSE = 28401;
    private static final int TRASH_REMOVE_BASE = 28500;
    private static final int CLASSIC_ADD = 28600;
    private static final int CLASSIC_BROWSE = 28601;
    private static final int CLASSIC_REMOVE_BASE = 28700;
    private static final int STORAGE_WHITELIST = 28800;

    private static final int HEADER_HEIGHT = 38;
    private static final int SECTION_GAP = 10;
    private static final int PANEL_PADDING = 10;
    private static final int MAX_VISIBLE_TRASH = 4;
    private static final int MAX_VISIBLE_CLASSIC_BLOCKS = 4;
    private static final int SCROLLBAR_WIDTH = 7;

    private final GuiScreen parent;
    private UltmineShape112 selectedShape = UltmineShape112.S_3x3;
    private GuiTextField depthField;
    private GuiTextField lengthField;
    private GuiTextField classicMaxBlocksField;
    private GuiTextField magnetRangeField;
    private GuiTextField trashField;
    private GuiTextField classicField;

    private int trashScrollOffset;
    private int classicBlockScrollOffset;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int contentHeight;
    private int scrollOffset;
    private int toggleSectionY;
    private int shapeSelectorY;
    private int shapeConfigY;
    private int magnetSectionY;
    private int trashSectionY;
    private int classicBlockSectionY;
    private int bottomY;
    private int leftColX;
    private int leftColW;
    private int rightColX;
    private int rightColW;
    private boolean showClassicBlockSection;
    private int secX;
    private int secW;
    private int secCx;
    private boolean draggingScrollbar;
    private int scrollbarThumbY;
    private int scrollbarThumbH;
    private int scrollbarTrackY;
    private int scrollbarTrackH;
    private int scrollbarDragGrabOffset;

    public UltmineConfigGui112(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        ClientUltmineConfig.load();
        selectedShape = ClientUltmineConfig.getSelectedShape();
        buttonList.clear();
        calculateLayout();
        int oY = -scrollOffset;

        setSectionAnchor(leftColX, leftColW);
        int toggleW = secW - PANEL_PADDING * 2;
        int toggleGap = 4;
        int toggleBtnW = (toggleW - toggleGap * 3) / 4;
        int toggleStartX = secCx - toggleW / 2;
        int toggleY = toggleSectionY + 16 + oY;
        buttonList.add(flatButton(DROPS, toggleStartX, toggleY, toggleBtnW, 20, ""));
        buttonList.add(flatButton(STORAGE, toggleStartX + toggleBtnW + toggleGap, toggleY, toggleBtnW, 20, ""));
        buttonList.add(flatButton(XP, toggleStartX + 2 * (toggleBtnW + toggleGap), toggleY, toggleBtnW, 20, ""));
        buttonList.add(flatButton(SAME, toggleStartX + 3 * (toggleBtnW + toggleGap), toggleY, toggleBtnW, 20, ""));
        buttonList.add(flatButton(STORAGE_WHITELIST, toggleStartX, toggleY + 22, toggleW, 18,
                tr("murilloskills.ultmine_config.storage_filter.button", ClientUltmineConfig.getStorageWhitelist().size())));

        UltmineShape112[] shapes = UltmineShape112.values();
        int shapeBtnW = Math.min(55, (secW - (shapes.length - 1) * 4) / shapes.length);
        int totalShapesW = shapes.length * shapeBtnW + (shapes.length - 1) * 4;
        int shapeStartX = secCx - totalShapesW / 2;
        for (int i = 0; i < shapes.length; i++) {
            buttonList.add(flatButton(SHAPE_BASE + i, shapeStartX + i * (shapeBtnW + 4),
                    shapeSelectorY + 16 + oY, shapeBtnW, 18, ""));
        }

        int maxDepth = maxDepth(selectedShape);
        int maxLength = maxLength(selectedShape);
        int variantCount = variantCount(selectedShape);
        int fieldW = 50;
        depthField = null;
        lengthField = null;
        classicMaxBlocksField = null;
        if (maxDepth > 1) {
            depthField = numericField(11, secCx + 20, shapeConfigY + 30 + oY, fieldW, 18,
                    ClientUltmineConfig.getDepth(selectedShape), 4);
        }
        if (maxLength > 1) {
            int lengthRowY = shapeConfigY + (maxDepth > 1 ? 54 : 30) + oY;
            lengthField = numericField(12, secCx + 20, lengthRowY, fieldW, 18,
                    ClientUltmineConfig.getLength(selectedShape), 4);
        }
        if (variantCount > 1) {
            int variantRowY = shapeConfigY + 30 + oY;
            if (maxDepth > 1) {
                variantRowY += 24;
            }
            if (maxLength > 1) {
                variantRowY += 24;
            }
            int variantBtnW = 20;
            int ctrlGap = 6;
            int variantValueW = Math.min(100, secW - PANEL_PADDING - 4 - 2 * variantBtnW - 2 * ctrlGap);
            if (variantValueW < 40) {
                variantValueW = 40;
            }
            int variantStartX = secCx + 20;
            int variantEndX = variantStartX + variantBtnW + ctrlGap + variantValueW + ctrlGap + variantBtnW;
            int colRight = secX + secW - 2;
            if (variantEndX > colRight) {
                variantStartX -= variantEndX - colRight;
            }
            if (variantStartX < secX + 2) {
                variantStartX = secX + 2;
            }
            buttonList.add(flatButton(VARIANT_LEFT, variantStartX, variantRowY, variantBtnW, 18, "<"));
            buttonList.add(flatButton(VARIANT_RIGHT, variantStartX + variantBtnW + ctrlGap + variantValueW + ctrlGap,
                    variantRowY, variantBtnW, 18, ">"));
        }
        if (selectedShape == UltmineShape112.LEGACY) {
            int rowY = shapeConfigY + 30 + oY;
            if (maxDepth > 1) {
                rowY += 24;
            }
            if (maxLength > 1) {
                rowY += 24;
            }
            if (variantCount > 1) {
                rowY += 24;
            }
            classicMaxBlocksField = numericField(13, secCx + 20, rowY, fieldW, 18,
                    ClientUltmineConfig.getLegacyMaxBlocks(), 4);
        }

        int magnetInnerW = secW - PANEL_PADDING * 2;
        int magnetToggleW = magnetInnerW / 2 - 2;
        int magnetLeftX = secCx - magnetInnerW / 2;
        buttonList.add(flatButton(MAGNET, magnetLeftX, magnetSectionY + 16 + oY, magnetToggleW, 20, ""));
        magnetRangeField = numericField(14, magnetLeftX + magnetToggleW + 54,
                magnetSectionY + 17 + oY, 36, 18, ClientUltmineConfig.getMagnetRange(), 2);

        setSectionAnchor(rightColX, rightColW);
        int browseBtnW = Math.min(70, secW / 4);
        int addBtnW = 22;
        int trashFieldW = secW - PANEL_PADDING * 2 - browseBtnW - addBtnW - 8;
        if (trashFieldW < 60) {
            trashFieldW = Math.max(40, secW - PANEL_PADDING * 2 - browseBtnW - addBtnW - 8);
        }
        int trashFieldX = secX + PANEL_PADDING;
        trashField = new GuiTextField(21, fontRenderer, trashFieldX, trashSectionY + 16 + oY, trashFieldW, 18);
        trashField.setMaxStringLength(100);
        buttonList.add(flatButton(TRASH_ADD, trashFieldX + trashFieldW + 4, trashSectionY + 16 + oY, addBtnW, 18, "+"));
        buttonList.add(flatButton(TRASH_BROWSE, trashFieldX + trashFieldW + addBtnW + 8,
                trashSectionY + 16 + oY, browseBtnW, 18, tr("murilloskills.ultmine_config.trash.browse")));
        addListControls(false, ClientUltmineConfig.getTrashItems(), trashSectionY, trashScrollOffset, TRASH_REMOVE_BASE,
                MAX_VISIBLE_TRASH);

        classicField = null;
        if (showClassicBlockSection) {
            int classicFieldW = secW - PANEL_PADDING * 2 - browseBtnW - addBtnW - 8;
            int classicFieldX = secX + PANEL_PADDING;
            classicField = new GuiTextField(22, fontRenderer, classicFieldX, classicBlockSectionY + 16 + oY,
                    classicFieldW, 18);
            classicField.setMaxStringLength(120);
            buttonList.add(flatButton(CLASSIC_ADD, classicFieldX + classicFieldW + 4,
                    classicBlockSectionY + 16 + oY, addBtnW, 18, "+"));
            buttonList.add(flatButton(CLASSIC_BROWSE, classicFieldX + classicFieldW + addBtnW + 8,
                    classicBlockSectionY + 16 + oY, browseBtnW, 18,
                    tr("murilloskills.ultmine_config.classic_block_lock.browse")));
            addListControls(true, ClientUltmineConfig.getLegacyBlockedBlocks(), classicBlockSectionY,
                    classicBlockScrollOffset, CLASSIC_REMOVE_BASE, MAX_VISIBLE_CLASSIC_BLOCKS);
        }

        int btnW = 90;
        int btnGap = 12;
        int panelCenterX = panelX + panelW / 2;
        buttonList.add(flatButton(SAVE, panelCenterX - btnW - btnGap / 2, bottomY + oY, btnW, 20,
                tr("murilloskills.ultmine_config.save")));
        buttonList.add(flatButton(RESET, panelCenterX + btnGap / 2, bottomY + oY, btnW, 20,
                tr("murilloskills.ultmine_config.reset")));
    }

    private GuiTextField numericField(int id, int x, int y, int w, int h, int value, int maxLength) {
        GuiTextField field = new GuiTextField(id, fontRenderer, x, y, w, h);
        field.setMaxStringLength(maxLength);
        field.setText(String.valueOf(value));
        return field;
    }

    private void addListControls(boolean classic, List<String> values, int sectionY, int offset, int removeBase,
            int maxVisible) {
        int maxScroll = Math.max(0, values.size() - maxVisible);
        if (classic) {
            classicBlockScrollOffset = clamp(classicBlockScrollOffset, 0, maxScroll);
            offset = classicBlockScrollOffset;
        } else {
            trashScrollOffset = clamp(trashScrollOffset, 0, maxScroll);
            offset = trashScrollOffset;
        }
        int oY = -scrollOffset;
        int count = Math.min(maxVisible, Math.max(0, values.size() - offset));
        for (int i = 0; i < count; i++) {
            int y = sectionY + 38 + i * 16 + oY;
            buttonList.add(flatButton(removeBase + i, secX + secW - PANEL_PADDING - 18, y, 18, 14, "x"));
        }
    }

    private void calculateLayout() {
        showClassicBlockSection = selectedShape == UltmineShape112.LEGACY;
        int maxDepth = maxDepth(selectedShape);
        int maxLength = maxLength(selectedShape);
        int variantCount = variantCount(selectedShape);
        int shapeConfigH;
        if (selectedShape == UltmineShape112.LEGACY) {
            int variantH = variantCount > 1 ? 24 : 0;
            int maxBlocksH = 24;
            int infoCardH = 2 * 14 + 10;
            shapeConfigH = 30 + variantH + maxBlocksH + infoCardH + 8;
        } else {
            int rows = 0;
            if (maxDepth > 1) {
                rows++;
            }
            if (maxLength > 1) {
                rows++;
            }
            if (variantCount > 1) {
                rows++;
            }
            int rowsH = rows == 0 ? 18 : rows * 24;
            shapeConfigH = 30 + rowsH + 8;
        }

        int trashCount = ClientUltmineConfig.getTrashItems().size();
        int trashH = 38 + Math.max(Math.min(MAX_VISIBLE_TRASH, trashCount), 1) * 16
                + (trashCount > MAX_VISIBLE_TRASH ? 18 : 0) + 6;
        int classicCount = ClientUltmineConfig.getLegacyBlockedBlocks().size();
        int classicH = 38 + Math.max(Math.min(MAX_VISIBLE_CLASSIC_BLOCKS, classicCount), 1) * 16
                + (classicCount > MAX_VISIBLE_CLASSIC_BLOCKS ? 18 : 0) + 6;

        int compactGap = 8;
        int toggleSectH = 62;
        int shapeSelH = 36;
        int magnetH = 42;
        int leftContentH = toggleSectH + compactGap + shapeSelH + compactGap + shapeConfigH + compactGap + magnetH;
        int rightContentH = trashH + (showClassicBlockSection ? compactGap + classicH : 0);
        int twoColContentH = HEADER_HEIGHT + 4 + Math.max(leftContentH, rightContentH) + SECTION_GAP + 26;
        boolean twoColumn = width >= 460 && height >= Math.min(340, twoColContentH + 6);

        if (twoColumn) {
            panelW = Math.min(620, width - 16);
            panelX = (width - panelW) / 2;
            panelY = Math.max(4, (height - twoColContentH) / 2);
            int innerGap = 12;
            int innerW = panelW - PANEL_PADDING * 2 - innerGap;
            leftColW = innerW / 2;
            rightColW = innerW - leftColW;
            leftColX = panelX + PANEL_PADDING;
            rightColX = leftColX + leftColW + innerGap;
            int colTop = panelY + HEADER_HEIGHT + 4;
            toggleSectionY = colTop;
            shapeSelectorY = toggleSectionY + toggleSectH + compactGap;
            shapeConfigY = shapeSelectorY + shapeSelH + compactGap;
            magnetSectionY = shapeConfigY + shapeConfigH + compactGap;
            int leftBottom = magnetSectionY + magnetH;
            trashSectionY = colTop;
            classicBlockSectionY = showClassicBlockSection ? trashSectionY + trashH + compactGap : -9999;
            int rightBottom = showClassicBlockSection ? classicBlockSectionY + classicH : trashSectionY + trashH;
            int contentBottom = Math.max(leftBottom, rightBottom);
            bottomY = contentBottom + SECTION_GAP;
            contentHeight = bottomY + 26;
            panelH = contentHeight - panelY;
        } else {
            panelW = Math.min(380, width - 16);
            panelX = (width - panelW) / 2;
            panelY = 8;
            leftColX = panelX;
            rightColX = panelX;
            leftColW = panelW;
            rightColW = panelW;
            toggleSectionY = panelY + HEADER_HEIGHT + 4;
            shapeSelectorY = toggleSectionY + toggleSectH + compactGap;
            shapeConfigY = shapeSelectorY + shapeSelH + compactGap;
            magnetSectionY = shapeConfigY + shapeConfigH + compactGap;
            trashSectionY = magnetSectionY + magnetH + compactGap;
            classicBlockSectionY = showClassicBlockSection ? trashSectionY + trashH + compactGap : -9999;
            int contentBottom = showClassicBlockSection ? classicBlockSectionY + classicH : trashSectionY + trashH;
            bottomY = contentBottom + SECTION_GAP;
            contentHeight = bottomY + 26;
            panelH = contentHeight - panelY;
        }
        scrollOffset = clamp(scrollOffset, 0, maxMainScroll());
        if (panelY + panelH - scrollOffset > height - 4) {
            panelH = height - 4 - panelY + scrollOffset;
        }
    }

    private int maxMainScroll() {
        return Math.max(0, contentHeight - (height - 4));
    }

    private void setSectionAnchor(int x, int w) {
        secX = x;
        secW = w;
        secCx = x + w / 2;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        applyFieldValues();
        if (button.id == SAVE) {
            saveAndClose();
            return;
        }
        if (button.id == RESET) {
            ClientUltmineConfig.resetDefaults();
            selectedShape = ClientUltmineConfig.getSelectedShape();
            trashScrollOffset = 0;
            classicBlockScrollOffset = 0;
            scrollOffset = 0;
            syncConfigToServer();
            initGui();
            return;
        }
        int shapeIndex = button.id - SHAPE_BASE;
        if (shapeIndex >= 0 && shapeIndex < UltmineShape112.values().length) {
            selectedShape = UltmineShape112.values()[shapeIndex];
            ClientUltmineConfig.setSelectedShape(selectedShape);
            ClientUltmineConfig.save();
            syncConfigToServer();
            initGui();
            return;
        }
        if (button.id == DROPS) {
            ClientUltmineConfig.toggleDropsToInventory();
        } else if (button.id == STORAGE) {
            ClientUltmineConfig.toggleDropsToStorage();
        } else if (button.id == XP) {
            ClientUltmineConfig.toggleXpDirectToPlayer();
        } else if (button.id == SAME) {
            ClientUltmineConfig.toggleSameBlockOnly();
        } else if (button.id == MAGNET) {
            ClientUltmineConfig.toggleMagnet();
        } else if (button.id == VARIANT_LEFT || button.id == VARIANT_RIGHT) {
            int count = variantCount(selectedShape);
            int current = ClientUltmineConfig.getVariant(selectedShape);
            int next = button.id == VARIANT_RIGHT ? current + 1 : current - 1;
            if (next < 0) {
                next = count - 1;
            }
            ClientUltmineConfig.setVariant(selectedShape, next);
        } else if (button.id == TRASH_ADD) {
            addFromField(trashField, false);
            return;
        } else if (button.id == CLASSIC_ADD) {
            addFromField(classicField, true);
            return;
        } else if (button.id == TRASH_BROWSE) {
            ClientUltmineConfig.save();
            syncConfigToServer();
            mc.displayGuiScreen(new TrashItemPickerGui112(this));
            return;
        } else if (button.id == CLASSIC_BROWSE) {
            ClientUltmineConfig.save();
            syncConfigToServer();
            mc.displayGuiScreen(new UltmineClassicBlockPickerGui112(this));
            return;
        } else if (button.id == STORAGE_WHITELIST) {
            ClientUltmineConfig.save();
            syncConfigToServer();
            mc.displayGuiScreen(new StorageWhitelistPickerGui112(this));
            return;
        } else if (button.id >= TRASH_REMOVE_BASE && button.id < TRASH_REMOVE_BASE + 100) {
            int index = trashScrollOffset + button.id - TRASH_REMOVE_BASE;
            List<String> values = ClientUltmineConfig.getTrashItems();
            if (index >= 0 && index < values.size()) {
                ClientUltmineConfig.removeTrashItem(values.get(index));
            }
        } else if (button.id >= CLASSIC_REMOVE_BASE && button.id < CLASSIC_REMOVE_BASE + 100) {
            int index = classicBlockScrollOffset + button.id - CLASSIC_REMOVE_BASE;
            List<String> values = ClientUltmineConfig.getLegacyBlockedBlocks();
            if (index >= 0 && index < values.size()) {
                ClientUltmineConfig.removeLegacyBlockedBlock(values.get(index));
            }
        }
        ClientUltmineConfig.save();
        syncConfigToServer();
        initGui();
    }

    private void addFromField(GuiTextField field, boolean classic) {
        if (field == null) {
            return;
        }
        String value = normalizeId(field.getText());
        if (value.length() == 0) {
            return;
        }
        if (classic) {
            ClientUltmineConfig.addLegacyBlockedBlock(value);
        } else {
            ClientUltmineConfig.addTrashItem(value);
        }
        field.setText("");
        ClientUltmineConfig.save();
        syncConfigToServer();
        initGui();
    }

    private void applyFieldValues() {
        if (depthField != null) {
            ClientUltmineConfig.setDepth(selectedShape, parseField(depthField, ClientUltmineConfig.getDepth(selectedShape)));
        }
        if (lengthField != null) {
            ClientUltmineConfig.setLength(selectedShape, parseField(lengthField, ClientUltmineConfig.getLength(selectedShape)));
        }
        if (classicMaxBlocksField != null) {
            ClientUltmineConfig.setLegacyMaxBlocks(parseField(classicMaxBlocksField, ClientUltmineConfig.getLegacyMaxBlocks()));
        }
        if (magnetRangeField != null) {
            ClientUltmineConfig.setMagnetRange(parseField(magnetRangeField, ClientUltmineConfig.getMagnetRange()));
        }
    }

    private int parseField(GuiTextField field, int fallback) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @Override
    public void updateScreen() {
        tickField(depthField);
        tickField(lengthField);
        tickField(classicMaxBlocksField);
        tickField(magnetRangeField);
        tickField(trashField);
        tickField(classicField);
    }

    private void tickField(GuiTextField field) {
        if (field != null) {
            field.updateCursorCounter();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            saveAndClose();
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            if (trashField != null && trashField.isFocused()) {
                addFromField(trashField, false);
                return;
            }
            if (classicField != null && classicField.isFocused()) {
                addFromField(classicField, true);
                return;
            }
        }
        if (typeField(depthField, typedChar, keyCode)) return;
        if (typeField(lengthField, typedChar, keyCode)) return;
        if (typeField(classicMaxBlocksField, typedChar, keyCode)) return;
        if (typeField(magnetRangeField, typedChar, keyCode)) return;
        if (typeField(trashField, typedChar, keyCode)) return;
        if (typeField(classicField, typedChar, keyCode)) return;
        super.keyTyped(typedChar, keyCode);
    }

    private boolean typeField(GuiTextField field, char typedChar, int keyCode) {
        return field != null && field.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && handleMainScrollbarClick(mouseX, mouseY)) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
        clickField(depthField, mouseX, mouseY, mouseButton);
        clickField(lengthField, mouseX, mouseY, mouseButton);
        clickField(classicMaxBlocksField, mouseX, mouseY, mouseButton);
        clickField(magnetRangeField, mouseX, mouseY, mouseButton);
        clickField(trashField, mouseX, mouseY, mouseButton);
        clickField(classicField, mouseX, mouseY, mouseButton);
    }

    private void clickField(GuiTextField field, int mouseX, int mouseY, int mouseButton) {
        if (field != null) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingScrollbar) {
            updateMainScrollFromMouse(mouseY);
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingScrollbar = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (inside(mouseX, mouseY, rightColX, trashSectionY - scrollOffset, rightColW, listSectionH(false))) {
            setListScroll(false, trashScrollOffset + (delta > 0 ? -1 : 1));
            return;
        }
        if (showClassicBlockSection && inside(mouseX, mouseY, rightColX, classicBlockSectionY - scrollOffset,
                rightColW, listSectionH(true))) {
            setListScroll(true, classicBlockScrollOffset + (delta > 0 ? -1 : 1));
            return;
        }
        if (maxMainScroll() > 0) {
            scrollOffset = clamp(scrollOffset + (delta > 0 ? -12 : 12), 0, maxMainScroll());
            refreshKeepingText();
        }
    }

    private boolean handleMainScrollbarClick(int mouseX, int mouseY) {
        if (scrollbarTrackH <= 0) {
            return false;
        }
        int barX = panelX + panelW - SCROLLBAR_WIDTH - 1;
        if (!inside(mouseX, mouseY, barX - 2, scrollbarTrackY, SCROLLBAR_WIDTH + 4, scrollbarTrackH)) {
            return false;
        }
        draggingScrollbar = true;
        scrollbarDragGrabOffset = inside(mouseX, mouseY, barX, scrollbarThumbY, SCROLLBAR_WIDTH, scrollbarThumbH)
                ? mouseY - scrollbarThumbY : scrollbarThumbH / 2;
        updateMainScrollFromMouse(mouseY);
        return true;
    }

    private void updateMainScrollFromMouse(int mouseY) {
        int maxScroll = maxMainScroll();
        if (maxScroll <= 0 || scrollbarTrackH <= scrollbarThumbH) {
            return;
        }
        int top = mouseY - scrollbarDragGrabOffset;
        int range = scrollbarTrackH - scrollbarThumbH;
        int next = clamp((top - scrollbarTrackY) * maxScroll / Math.max(1, range), 0, maxScroll);
        if (next != scrollOffset) {
            scrollOffset = next;
            refreshKeepingText();
        }
    }

    private int listSectionH(boolean classic) {
        List<String> values = classic ? ClientUltmineConfig.getLegacyBlockedBlocks() : ClientUltmineConfig.getTrashItems();
        int maxVisible = classic ? MAX_VISIBLE_CLASSIC_BLOCKS : MAX_VISIBLE_TRASH;
        return 38 + Math.max(Math.min(maxVisible, values.size()), 1) * 16
                + (values.size() > maxVisible ? 18 : 0) + 6;
    }

    private int maxListScroll(boolean classic) {
        List<String> values = classic ? ClientUltmineConfig.getLegacyBlockedBlocks() : ClientUltmineConfig.getTrashItems();
        int maxVisible = classic ? MAX_VISIBLE_CLASSIC_BLOCKS : MAX_VISIBLE_TRASH;
        return Math.max(0, values.size() - maxVisible);
    }

    private void setListScroll(boolean classic, int value) {
        if (classic) {
            int next = clamp(value, 0, maxListScroll(true));
            if (next != classicBlockScrollOffset) {
                classicBlockScrollOffset = next;
                refreshKeepingText();
            }
        } else {
            int next = clamp(value, 0, maxListScroll(false));
            if (next != trashScrollOffset) {
                trashScrollOffset = next;
                refreshKeepingText();
            }
        }
    }

    private void refreshKeepingText() {
        String trashText = trashField == null ? "" : trashField.getText();
        String classicText = classicField == null ? "" : classicField.getText();
        initGui();
        if (trashField != null) {
            trashField.setText(trashText);
        }
        if (classicField != null) {
            classicField.setText(classicText);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        renderGradientBackground();
        int visiblePanelH = Math.min(panelH, height - 4 - panelY);
        drawRect(panelX, panelY, panelX + panelW, panelY + visiblePanelH, Palette.PANEL_BG);
        drawPanelBorder(panelX, panelY, panelW, visiblePanelH, Palette.SECTION_BORDER);
        renderCornerAccents(panelX, panelY, panelW, visiblePanelH, 6, Palette.ACCENT_GOLD);

        enableScissor(panelX, panelY, panelW, visiblePanelH);
        int oY = -scrollOffset;
        renderHeader(oY);
        setSectionAnchor(leftColX, leftColW);
        renderToggleSection(oY);
        renderShapeSelector(oY);
        renderShapeConfig(oY);
        renderMagnetSection(oY);
        setSectionAnchor(rightColX, rightColW);
        renderTrashSection(oY);
        if (showClassicBlockSection) {
            renderClassicBlockSection(oY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawField(depthField);
        drawField(lengthField);
        drawField(classicMaxBlocksField);
        drawField(magnetRangeField);
        drawField(trashField);
        drawField(classicField);

        setSectionAnchor(leftColX, leftColW);
        renderToggleButtonContent(oY);
        renderShapeSelectorContent(oY);
        renderShapeConfigValues(oY);
        renderMagnetContent(oY);
        setSectionAnchor(rightColX, rightColW);
        renderTrashContent(oY);
        if (showClassicBlockSection) {
            renderClassicBlockContent(oY);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        renderMainScrollbar(visiblePanelH);
    }

    private void drawField(GuiTextField field) {
        if (field != null) {
            field.drawTextBox();
        }
    }

    private void renderGradientBackground() {
        for (int y = 0; y < height; y++) {
            float ratio = (float) y / Math.max(1, height);
            int r = (int) (8 + ratio * 6);
            int g = (int) (8 + ratio * 4);
            int b = (int) (16 + ratio * 10);
            drawRect(0, y, width, y + 1, Palette.BG_OVERLAY | (r << 16) | (g << 8) | b);
        }
    }

    private void renderHeader(int oY) {
        int centerX = width / 2;
        int headerBottom = panelY + HEADER_HEIGHT + oY;
        drawRect(panelX + 1, panelY + 1 + oY, panelX + panelW - 1, headerBottom, Palette.PANEL_BG_HEADER);
        int lineW = panelW - PANEL_PADDING * 4;
        drawRect(centerX - lineW / 2, headerBottom - 1, centerX + lineW / 2, headerBottom, Palette.ACCENT_GOLD);
        drawCenteredString(fontRenderer, tr("murilloskills.ultmine_config.title"), centerX, panelY + 5 + oY,
                Palette.TEXT_GOLD);
        drawCenteredString(fontRenderer, tr("murilloskills.ultmine_config.subtitle"), centerX, panelY + 19 + oY,
                Palette.TEXT_MUTED);
    }

    private void renderToggleSection(int oY) {
        renderSectionDivider(toggleSectionY + oY, tr("murilloskills.ultmine_config.section.toggles"));
    }

    private void renderToggleButtonContent(int oY) {
        int toggleW = secW - PANEL_PADDING * 2;
        int toggleGap = 4;
        int toggleBtnW = (toggleW - toggleGap * 3) / 4;
        int toggleStartX = secCx - toggleW / 2;
        int btnY = toggleSectionY + 16 + oY;
        renderToggleCard(toggleStartX, btnY, toggleBtnW,
                tr("murilloskills.ultmine_config.drops_to_inventory"), ClientUltmineConfig.isDropsToInventory());
        renderToggleCard(toggleStartX + toggleBtnW + toggleGap, btnY, toggleBtnW,
                tr("murilloskills.ultmine_config.drops_to_storage"), ClientUltmineConfig.isDropsToStorage());
        renderToggleCard(toggleStartX + 2 * (toggleBtnW + toggleGap), btnY, toggleBtnW,
                tr("murilloskills.ultmine_config.xp_direct"), ClientUltmineConfig.isXpDirectToPlayer());
        renderToggleCard(toggleStartX + 3 * (toggleBtnW + toggleGap), btnY, toggleBtnW,
                tr("murilloskills.ultmine_config.same_block_only"), ClientUltmineConfig.isSameBlockOnly());
    }

    private void renderToggleCard(int x, int y, int w, String label, boolean enabled) {
        drawRect(x + 1, y + 1, x + w - 1, y + 19, enabled ? Palette.SECTION_BG_ACTIVE : Palette.SECTION_BG);
        if (enabled) {
            drawRect(x + 1, y + 18, x + w - 1, y + 20, Palette.ACCENT_GREEN);
        }
        String prefix = enabled ? "\u25CF " : "\u25CB ";
        int color = enabled ? Palette.TEXT_GREEN : Palette.TEXT_GRAY;
        drawCenteredString(fontRenderer, fitForWidth(prefix + label, w - 8), x + w / 2, y + 6, color);
    }

    private void renderShapeSelector(int oY) {
        renderSectionDivider(shapeSelectorY + oY, tr("murilloskills.ultmine_config.section.shape"));
    }

    private void renderShapeSelectorContent(int oY) {
        UltmineShape112[] shapes = UltmineShape112.values();
        int shapeBtnW = Math.min(55, (secW - (shapes.length - 1) * 4) / shapes.length);
        int totalShapesW = shapes.length * shapeBtnW + (shapes.length - 1) * 4;
        int startX = secCx - totalShapesW / 2;
        int btnY = shapeSelectorY + 16 + oY;
        for (int i = 0; i < shapes.length; i++) {
            UltmineShape112 shape = shapes[i];
            boolean active = shape == selectedShape;
            int x = startX + i * (shapeBtnW + 4);
            drawRect(x + 1, btnY + 1, x + shapeBtnW - 1, btnY + 17,
                    active ? Palette.SECTION_BG_ACTIVE : Palette.SECTION_BG);
            if (active) {
                drawRect(x + 1, btnY + 16, x + shapeBtnW - 1, btnY + 18, Palette.TEXT_AQUA);
            }
            drawCenteredString(fontRenderer, fitForWidth(shortShapeLabel(shape), shapeBtnW - 4),
                    x + shapeBtnW / 2, btnY + 5, active ? Palette.TEXT_AQUA : Palette.TEXT_GRAY);
        }
    }

    private void renderShapeConfig(int oY) {
        renderSectionDivider(shapeConfigY + oY, tr("murilloskills.ultmine_config.section.shape_config"));
        drawCenteredString(fontRenderer, shapeLabel(selectedShape), secCx, shapeConfigY + 16 + oY,
                Palette.TEXT_WHITE);
    }

    private void renderShapeConfigValues(int oY) {
        int maxDepth = maxDepth(selectedShape);
        int maxLength = maxLength(selectedShape);
        int variantCount = variantCount(selectedShape);
        int labelX = secX + PANEL_PADDING;
        int fieldW = 50;
        int row = 0;
        if (maxDepth > 1) {
            int y = shapeConfigY + 30 + row * 24 + oY;
            drawString(fontRenderer, tr("murilloskills.ultmine_config.depth"), labelX, y + 5, Palette.TEXT_LIGHT);
            drawString(fontRenderer, "/ " + maxDepth, secCx + 20 + fieldW + 4, y + 5, Palette.TEXT_MUTED);
            row++;
        }
        if (maxLength > 1) {
            int y = shapeConfigY + 30 + row * 24 + oY;
            drawString(fontRenderer, tr("murilloskills.ultmine_config.length"), labelX, y + 5, Palette.TEXT_LIGHT);
            drawString(fontRenderer, "/ " + maxLength, secCx + 20 + fieldW + 4, y + 5, Palette.TEXT_MUTED);
            row++;
        }
        if (variantCount > 1) {
            int y = shapeConfigY + 30 + row * 24 + oY;
            drawString(fontRenderer, tr("murilloskills.ultmine_config.variant"), labelX, y + 5, Palette.TEXT_LIGHT);
            int variantBtnW = 20;
            int ctrlGap = 6;
            int variantValueW = Math.min(100, secW - PANEL_PADDING - 4 - 2 * variantBtnW - 2 * ctrlGap);
            if (variantValueW < 40) {
                variantValueW = 40;
            }
            int variantStartX = secCx + 20;
            int variantEndX = variantStartX + variantBtnW + ctrlGap + variantValueW + ctrlGap + variantBtnW;
            int colRight = secX + secW - 2;
            if (variantEndX > colRight) {
                variantStartX -= variantEndX - colRight;
            }
            if (variantStartX < secX + 2) {
                variantStartX = secX + 2;
            }
            int boxX = variantStartX + variantBtnW + ctrlGap;
            drawRect(boxX, y, boxX + variantValueW, y + 18, Palette.SECTION_BG);
            drawPanelBorder(boxX, y, variantValueW, 18, Palette.SECTION_BORDER);
            drawCenteredString(fontRenderer, fitForWidth(variantLabel(), variantValueW - 4),
                    boxX + variantValueW / 2, y + 5, Palette.TEXT_AQUA);
            row++;
        }
        if (selectedShape == UltmineShape112.LEGACY) {
            int y = shapeConfigY + 30 + row * 24 + oY;
            drawString(fontRenderer, tr("murilloskills.ultmine_config.legacy.max_blocks"), labelX, y + 5,
                    Palette.TEXT_LIGHT);
            drawString(fontRenderer, "/ 4096", secCx + 20 + fieldW + 4, y + 5, Palette.TEXT_MUTED);
            row++;
            int cardX = secX + 4;
            int cardW = secW - 8;
            int cardY = shapeConfigY + 30 + row * 24 + oY;
            int cardH = 2 * 14 + 8;
            drawRect(cardX, cardY, cardX + cardW, cardY + cardH, Palette.CARD_BG_SUBTLE);
            drawPanelBorder(cardX, cardY, cardW, cardH, Palette.INFO_BOX_BORDER);
            int infoLabelX = cardX + 8;
            int infoValueX = cardX + cardW - 8;
            drawInfoRow(infoLabelX, infoValueX, cardY + 5,
                    tr("murilloskills.ultmine_config.legacy.deepslate"), true);
            drawInfoRow(infoLabelX, infoValueX, cardY + 19,
                    tr("murilloskills.ultmine_config.legacy.tool_damage"), true);
        } else if (maxDepth <= 1 && maxLength <= 1 && variantCount <= 1) {
            drawCenteredString(fontRenderer, tr("murilloskills.ultmine_config.no_options"), secCx,
                    shapeConfigY + 32 + oY, Palette.TEXT_MUTED);
        }
    }

    private void drawInfoRow(int labelX, int valueX, int y, String label, boolean enabled) {
        String value = enabled ? "ON" : "OFF";
        drawString(fontRenderer, label, labelX, y, Palette.TEXT_LIGHT);
        drawString(fontRenderer, value, valueX - fontRenderer.getStringWidth(value), y,
                enabled ? Palette.TEXT_GREEN : Palette.STATUS_INACTIVE);
    }

    private void renderMagnetSection(int oY) {
        renderSectionDivider(magnetSectionY + oY, tr("murilloskills.ultmine_config.section.magnet"));
    }

    private void renderMagnetContent(int oY) {
        int magnetInnerW = secW - PANEL_PADDING * 2;
        int magnetToggleW = magnetInnerW / 2 - 2;
        int magnetLeftX = secCx - magnetInnerW / 2;
        int y = magnetSectionY + 16 + oY;
        renderToggleCard(magnetLeftX, y, magnetToggleW,
                tr("murilloskills.ultmine_config.magnet.toggle"), ClientUltmineConfig.isMagnetEnabled());
        int rightAreaX = magnetLeftX + magnetToggleW + 4;
        drawString(fontRenderer, tr("murilloskills.ultmine_config.magnet.range"), rightAreaX, y + 6,
                Palette.TEXT_LIGHT);
        drawString(fontRenderer, "/ 32", rightAreaX + 50 + 36 + 3, y + 6, Palette.TEXT_MUTED);
    }

    private void renderTrashSection(int oY) {
        renderSectionDivider(trashSectionY + oY, tr("murilloskills.ultmine_config.section.trash"));
    }

    private void renderTrashContent(int oY) {
        renderListContent(false, ClientUltmineConfig.getTrashItems(), trashSectionY, trashScrollOffset,
                MAX_VISIBLE_TRASH, tr("murilloskills.ultmine_config.trash.empty"), oY);
    }

    private void renderClassicBlockSection(int oY) {
        renderSectionDivider(classicBlockSectionY + oY,
                tr("murilloskills.ultmine_config.section.classic_block_lock"));
    }

    private void renderClassicBlockContent(int oY) {
        renderListContent(true, ClientUltmineConfig.getLegacyBlockedBlocks(), classicBlockSectionY,
                classicBlockScrollOffset, MAX_VISIBLE_CLASSIC_BLOCKS,
                tr("murilloskills.ultmine_config.classic_block_lock.empty"), oY);
    }

    private void renderListContent(boolean classic, List<String> values, int sectionY, int offset, int maxVisible,
            String empty, int oY) {
        drawPlaceholder(classic ? classicField : trashField, classic ? "minecraft:stone" : "minecraft:cobblestone");
        int labelX = secX + PANEL_PADDING + 2;
        if (values.isEmpty()) {
            drawString(fontRenderer, empty, labelX, sectionY + 40 + oY, Palette.TEXT_MUTED);
            return;
        }
        int visibleCount = Math.min(maxVisible, values.size() - offset);
        int rowX = secX + PANEL_PADDING;
        int rowW = secW - PANEL_PADDING * 2;
        for (int i = 0; i < visibleCount; i++) {
            int idx = offset + i;
            if (idx >= values.size()) {
                break;
            }
            int y = sectionY + 38 + i * 16 + oY;
            if (i % 2 == 1) {
                drawRect(rowX, y - 1, rowX + rowW, y + 14, Palette.ALTERNATING_ROW_BG);
            }
            String value = values.get(idx);
            String display = value.startsWith("minecraft:") ? value.substring(10) : value;
            drawString(fontRenderer, "\u2022 " + fitForWidth(display, secW - PANEL_PADDING * 2 - 24),
                    labelX, y + 3, idx % 2 == 0 ? Palette.TEXT_LIGHT : Palette.TEXT_GRAY);
        }
        if (values.size() > maxVisible) {
            int countY = sectionY + 38 + maxVisible * 16 + oY;
            String countText = (offset + 1) + "-" + Math.min(offset + maxVisible, values.size()) + " / " + values.size();
            drawCenteredString(fontRenderer, countText, secCx, countY + 4, Palette.TEXT_MUTED);
        }
    }

    private void drawPlaceholder(GuiTextField field, String placeholder) {
        if (field != null && field.getText().length() == 0 && !field.isFocused()) {
            drawString(fontRenderer, placeholder, field.x + 4, field.y + 5, Palette.TEXT_MUTED);
        }
    }

    private void renderSectionDivider(int y, String title) {
        int titleW = fontRenderer.getStringWidth(title);
        int sideMargin = 4;
        int leftStart = secX + sideMargin;
        int leftEnd = secCx - titleW / 2 - 8;
        int rightStart = secCx + titleW / 2 + 8;
        int rightEnd = secX + secW - sideMargin;
        if (leftEnd > leftStart) {
            drawRect(leftStart, y + 3, leftEnd, y + 4, Palette.DIVIDER_COLOR);
        }
        drawCenteredString(fontRenderer, title, secCx, y - 1, Palette.TEXT_GOLD);
        if (rightEnd > rightStart) {
            drawRect(rightStart, y + 3, rightEnd, y + 4, Palette.DIVIDER_COLOR);
        }
    }

    private void renderMainScrollbar(int visiblePanelH) {
        int maxScroll = maxMainScroll();
        if (maxScroll <= 0) {
            scrollbarTrackH = 0;
            return;
        }
        scrollbarTrackY = panelY + 2;
        scrollbarTrackH = visiblePanelH - 4;
        scrollbarThumbH = Math.max(20, scrollbarTrackH * visiblePanelH / Math.max(1, contentHeight));
        scrollbarThumbY = scrollbarTrackY + scrollOffset * (scrollbarTrackH - scrollbarThumbH) / Math.max(1, maxScroll);
        int barX = panelX + panelW - SCROLLBAR_WIDTH - 1;
        drawRect(barX, scrollbarTrackY, barX + SCROLLBAR_WIDTH, scrollbarTrackY + scrollbarTrackH,
                Palette.PROGRESS_BAR_EMPTY);
        drawRect(barX, scrollbarThumbY, barX + SCROLLBAR_WIDTH, scrollbarThumbY + scrollbarThumbH,
                draggingScrollbar ? Palette.TEXT_AQUA : Palette.ACCENT_GOLD);
        drawRect(barX + 1, scrollbarThumbY + 1, barX + SCROLLBAR_WIDTH - 1, scrollbarThumbY + 2, 0x40FFFFFF);
    }

    private void enableScissor(int x, int y, int w, int h) {
        int factor = new ScaledResolution(mc).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * factor, mc.displayHeight - (y + h) * factor, w * factor, h * factor);
    }

    private String fitForWidth(String text, int maxWidth) {
        String out = text == null ? "" : text;
        if (fontRenderer.getStringWidth(out) <= maxWidth) {
            return out;
        }
        while (out.length() > 2 && fontRenderer.getStringWidth(out + "..") > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out.length() > 0 ? out + ".." : "";
    }

    private String shortShapeLabel(UltmineShape112 shape) {
        if (shape == UltmineShape112.S_3x3) return "3x3";
        if (shape == UltmineShape112.R_2x1) return "2x1";
        if (shape == UltmineShape112.LINE) return "Line";
        if (shape == UltmineShape112.STAIRS) return "Stair";
        if (shape == UltmineShape112.SQUARE_20x20_D1) return "20x20";
        if (shape == UltmineShape112.LEGACY) return "Vein";
        return shape.name();
    }

    private String shapeLabel(UltmineShape112 shape) {
        if (shape == UltmineShape112.S_3x3) return "3x3";
        if (shape == UltmineShape112.R_2x1) return "2x1";
        if (shape == UltmineShape112.LINE) return "Line";
        if (shape == UltmineShape112.STAIRS) return "Stairs";
        if (shape == UltmineShape112.SQUARE_20x20_D1) return "20x20";
        if (shape == UltmineShape112.LEGACY) return "Classic";
        return shape.name();
    }

    private String variantLabel() {
        int variant = ClientUltmineConfig.getVariant(selectedShape);
        if (selectedShape == UltmineShape112.STAIRS) {
            return variant == 1 ? "Down" : "Up";
        }
        if (selectedShape == UltmineShape112.SQUARE_20x20_D1) {
            return variant == 1 ? "Vertical NS" : variant == 2 ? "Vertical EW" : "Horizontal";
        }
        if (selectedShape == UltmineShape112.R_2x1) {
            return variant == 1 ? "Tall" : "Wide";
        }
        if (selectedShape == UltmineShape112.LEGACY) {
            return variant == 1 ? "Ores" : "Same Block";
        }
        return "Default";
    }

    private int maxDepth(UltmineShape112 shape) {
        if (shape == UltmineShape112.LINE || shape == UltmineShape112.LEGACY) return 1;
        if (shape == UltmineShape112.STAIRS) return 64;
        if (shape == UltmineShape112.SQUARE_20x20_D1) return 4;
        return 16;
    }

    private int maxLength(UltmineShape112 shape) {
        if (shape == UltmineShape112.LINE) return 128;
        if (shape == UltmineShape112.SQUARE_20x20_D1) return 20;
        if (shape == UltmineShape112.R_2x1) return 2;
        if (shape == UltmineShape112.S_3x3) return 3;
        return 1;
    }

    private int variantCount(UltmineShape112 shape) {
        if (shape == UltmineShape112.STAIRS || shape == UltmineShape112.R_2x1 || shape == UltmineShape112.LEGACY) {
            return 2;
        }
        if (shape == UltmineShape112.SQUARE_20x20_D1) {
            return 3;
        }
        return 1;
    }

    private String tr(String key, Object... args) {
        String value = net.minecraft.client.resources.I18n.format(key, args);
        return value == null || value.equals(key) ? fallback(key, args) : value;
    }

    private String fallback(String key, Object... args) {
        if ("murilloskills.ultmine_config.title".equals(key)) return "Ultmine Config";
        if ("murilloskills.ultmine_config.subtitle".equals(key)) return "Configure Ultmine mining preferences";
        if ("murilloskills.ultmine_config.section.toggles".equals(key)) return "Options";
        if ("murilloskills.ultmine_config.section.shape".equals(key)) return "Shape";
        if ("murilloskills.ultmine_config.section.shape_config".equals(key)) return "Shape Settings";
        if ("murilloskills.ultmine_config.drops_to_inventory".equals(key)) return "Drops to Inv.";
        if ("murilloskills.ultmine_config.drops_to_storage".equals(key)) return "To Storage";
        if ("murilloskills.ultmine_config.xp_direct".equals(key)) return "XP Direct";
        if ("murilloskills.ultmine_config.same_block_only".equals(key)) return "Same Block";
        if ("murilloskills.ultmine_config.depth".equals(key)) return "Depth:";
        if ("murilloskills.ultmine_config.length".equals(key)) return "Length:";
        if ("murilloskills.ultmine_config.variant".equals(key)) return "Variant:";
        if ("murilloskills.ultmine_config.no_options".equals(key)) return "No configurable options for this shape";
        if ("murilloskills.ultmine_config.legacy.max_blocks".equals(key)) return "Max Blocks";
        if ("murilloskills.ultmine_config.legacy.deepslate".equals(key)) return "Deepslate = Normal";
        if ("murilloskills.ultmine_config.legacy.tool_damage".equals(key)) return "Tool Damage";
        if ("murilloskills.ultmine_config.save".equals(key)) return "Save";
        if ("murilloskills.ultmine_config.reset".equals(key)) return "Reset";
        if ("murilloskills.ultmine_config.section.magnet".equals(key)) return "Magnet";
        if ("murilloskills.ultmine_config.magnet.toggle".equals(key)) return "Magnet";
        if ("murilloskills.ultmine_config.magnet.range".equals(key)) return "Range:";
        if ("murilloskills.ultmine_config.section.trash".equals(key)) return "Auto Trash";
        if ("murilloskills.ultmine_config.trash.empty".equals(key)) return "No items in trash list";
        if ("murilloskills.ultmine_config.trash.browse".equals(key)) return "Browse";
        if ("murilloskills.ultmine_config.section.classic_block_lock".equals(key)) return "Classic Block Lock";
        if ("murilloskills.ultmine_config.classic_block_lock.empty".equals(key)) return "No blocks locked for classic mode";
        if ("murilloskills.ultmine_config.classic_block_lock.browse".equals(key)) return "Browse";
        if ("murilloskills.ultmine_config.storage_filter.button".equals(key)) {
            int count = args != null && args.length > 0 && args[0] instanceof Number ? ((Number) args[0]).intValue() : 0;
            return "Storage Filter (" + count + " items)";
        }
        return key;
    }

    private void saveAndClose() {
        applyFieldValues();
        ClientUltmineConfig.save();
        syncConfigToServer();
        mc.displayGuiScreen(parent);
    }

    private void syncConfigToServer() {
        ModNetwork112.sendUltmineConfigToServer();
        UltmineShape112 shape = ClientUltmineConfig.getSelectedShape();
        ModNetwork112.sendUltmineSelection(shape, ClientUltmineConfig.getDepth(shape),
                ClientUltmineConfig.getLength(shape), ClientUltmineConfig.getVariant(shape),
                ClientUltmineConfig.getLegacyMaxBlocks());
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
