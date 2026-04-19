package com.murilloskills.gui;

import com.murilloskills.data.UltPlaceClientState;
import com.murilloskills.gui.renderer.RenderingHelper;
import com.murilloskills.network.UltPlaceUndoC2SPayload;
import com.murilloskills.skills.UltPlaceShape;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;

/**
 * Builder UltPlace configuration screen.
 * Mirrors the visual language of {@link UltmineConfigScreen} (premium palette,
 * sectioned panel, toggle cards, stepper rows with value boxes).
 */
public class UltPlaceConfigScreen extends Screen {

    private static final List<UltPlaceShape> SELECTABLE_SHAPES = Arrays.stream(UltPlaceShape.values())
            .filter(shape -> shape != UltPlaceShape.SINGLE)
            .toList();

    private static final int HEADER_HEIGHT = 50;
    private static final int SECTION_GAP = 14;
    private static final int PANEL_PADDING = 10;
    private static final int ROW_HEIGHT = 22;
    private static final int VALUE_BOX_W = 80;
    private static final int STEPPER_W = 20;
    private static final int STEPPER_GAP = 4;

    private final Screen parent;
    private final ColorPalette palette = ColorPalette.premium();

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    private int toggleSectionY;
    private int shapeSectionY;
    private int configSectionY;
    private int actionsSectionY;
    private int hintsY;
    private int doneY;

    private int shapeRows;
    private int configRows;

    public UltPlaceConfigScreen(Screen parent) {
        super(Text.translatable("murilloskills.ultplace.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();

        int centerX = this.width / 2;

        addToggleButton(centerX);
        addShapeSelectorButtons(centerX);
        addConfigButtons(centerX);
        addActionButtons(centerX);
        addDoneButton(centerX);
    }

    private void calculateLayout() {
        panelW = Math.min(360, this.width - 20);
        panelX = (this.width - panelW) / 2;
        panelY = Math.max(10, (this.height - estimatedHeight()) / 2);

        toggleSectionY = panelY + HEADER_HEIGHT + 4;
        shapeSectionY = toggleSectionY + 16 + 22 + SECTION_GAP;

        shapeRows = (SELECTABLE_SHAPES.size() + 3) / 4;
        configSectionY = shapeSectionY + 16 + shapeRows * (ROW_HEIGHT) + SECTION_GAP;

        UltPlaceShape current = UltPlaceClientState.getSelectedShape();
        configRows = countConfigRows(current);
        int configHeight = 16 + 14 + (configRows == 0 ? 14 : configRows * ROW_HEIGHT);

        actionsSectionY = configSectionY + configHeight + SECTION_GAP;
        hintsY = actionsSectionY + 16 + 22 + 8;
        doneY = hintsY + 14;

        panelH = doneY + 10 - panelY;
    }

    private int estimatedHeight() {
        UltPlaceShape current = UltPlaceClientState.getSelectedShape();
        int sRows = (SELECTABLE_SHAPES.size() + 3) / 4;
        int cRows = countConfigRows(current);
        int configH = 16 + 14 + (cRows == 0 ? 14 : cRows * ROW_HEIGHT);
        return HEADER_HEIGHT + 4
                + 16 + 22 + SECTION_GAP
                + 16 + sRows * ROW_HEIGHT + SECTION_GAP
                + configH + SECTION_GAP
                + 16 + 22 + 8
                + 14 + 20;
    }

    private static int countConfigRows(UltPlaceShape shape) {
        int rows = 0;
        if (SkillConfig.getUltPlaceShapeMaxSize(shape) > 1) rows++;
        if (SkillConfig.getUltPlaceShapeMaxLength(shape) > 1) rows++;
        if (UltPlaceShape.getVariantCount(shape) > 1) rows++;
        return rows;
    }

    // ===== BUTTON BUILDERS =====

    private void addToggleButton(int centerX) {
        int w = 200;
        ButtonWidget toggle = ButtonWidget.builder(Text.empty(), b -> {
            UltPlaceClientState.toggleEnabled();
            UltPlaceClientState.clearPreview();
            syncToServer();
            refresh();
        }).dimensions(centerX - w / 2, toggleSectionY + 16, w, 20).build();
        this.addDrawableChild(toggle);
    }

    private void addShapeSelectorButtons(int centerX) {
        int columns = 4;
        int btnW = (panelW - PANEL_PADDING * 2 - (columns - 1) * 4) / columns;
        int totalW = btnW * columns + (columns - 1) * 4;
        int startX = centerX - totalW / 2;
        int top = shapeSectionY + 16;

        for (int i = 0; i < SELECTABLE_SHAPES.size(); i++) {
            final UltPlaceShape shape = SELECTABLE_SHAPES.get(i);
            int row = i / columns;
            int col = i % columns;
            int x = startX + col * (btnW + 4);
            int y = top + row * ROW_HEIGHT;

            ButtonWidget btn = ButtonWidget.builder(Text.empty(), b -> {
                UltPlaceClientState.selectShape(shape);
                syncToServer();
                refresh();
            }).dimensions(x, y, btnW, 18).build();
            this.addDrawableChild(btn);
        }
    }

    private void addConfigButtons(int centerX) {
        UltPlaceShape current = UltPlaceClientState.getSelectedShape();
        int rowY = configSectionY + 16 + 14;
        int row = 0;

        if (SkillConfig.getUltPlaceShapeMaxSize(current) > 1) {
            addStepperRow(centerX, rowY + row * ROW_HEIGHT,
                    () -> {
                        UltPlaceClientState.adjustSize(-1);
                        syncToServer();
                        refresh();
                    },
                    () -> {
                        UltPlaceClientState.adjustSize(1);
                        syncToServer();
                        refresh();
                    });
            row++;
        }

        if (SkillConfig.getUltPlaceShapeMaxLength(current) > 1) {
            addStepperRow(centerX, rowY + row * ROW_HEIGHT,
                    () -> {
                        UltPlaceClientState.adjustLength(-1);
                        syncToServer();
                        refresh();
                    },
                    () -> {
                        UltPlaceClientState.adjustLength(1);
                        syncToServer();
                        refresh();
                    });
            row++;
        }

        if (UltPlaceShape.getVariantCount(current) > 1) {
            addStepperRow(centerX, rowY + row * ROW_HEIGHT,
                    () -> {
                        UltPlaceClientState.adjustVariant(-1);
                        syncToServer();
                        refresh();
                    },
                    () -> {
                        UltPlaceClientState.adjustVariant(1);
                        syncToServer();
                        refresh();
                    });
        }
    }

    private void addStepperRow(int centerX, int y, Runnable onMinus, Runnable onPlus) {
        int totalW = STEPPER_W + STEPPER_GAP + VALUE_BOX_W + STEPPER_GAP + STEPPER_W;
        int rowStartX = centerX - totalW / 2 + 30;

        ButtonWidget minus = ButtonWidget.builder(Text.literal("-"), b -> onMinus.run())
                .dimensions(rowStartX, y, STEPPER_W, 18).build();
        this.addDrawableChild(minus);

        ButtonWidget plus = ButtonWidget.builder(Text.literal("+"), b -> onPlus.run())
                .dimensions(rowStartX + STEPPER_W + STEPPER_GAP + VALUE_BOX_W + STEPPER_GAP, y, STEPPER_W, 18).build();
        this.addDrawableChild(plus);
    }

    private void addActionButtons(int centerX) {
        int btnW = 130;
        int gap = 10;
        int totalW = btnW * 2 + gap;
        int startX = centerX - totalW / 2;
        int y = actionsSectionY + 16;

        ButtonWidget undo = ButtonWidget.builder(
                Text.translatable("murilloskills.ultplace.undo").formatted(Formatting.AQUA),
                b -> ClientPlayNetworking.send(new UltPlaceUndoC2SPayload()))
                .dimensions(startX, y, btnW, 20).build();
        this.addDrawableChild(undo);

        ButtonWidget reset = ButtonWidget.builder(
                Text.translatable("murilloskills.ultplace.reset").formatted(Formatting.YELLOW),
                b -> {
                    UltPlaceClientState.resetSelections();
                    syncToServer();
                    refresh();
                })
                .dimensions(startX + btnW + gap, y, btnW, 20).build();
        this.addDrawableChild(reset);
    }

    private void addDoneButton(int centerX) {
        int w = 100;
        ButtonWidget done = ButtonWidget.builder(
                Text.translatable("gui.done").formatted(Formatting.GREEN),
                b -> close())
                .dimensions(centerX - w / 2, doneY, w, 20).build();
        this.addDrawableChild(done);
    }

    // ===== RENDERING =====

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderGradientBackground(context);

        // Panel
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, palette.panelBg());
        RenderingHelper.drawPanelBorder(context, panelX, panelY, panelW, panelH, palette.sectionBorder());
        RenderingHelper.renderCornerAccents(context, panelX, panelY, panelW, panelH, 6, palette.accentGold());

        renderHeader(context);
        renderSectionDivider(context, toggleSectionY,
                Text.translatable("murilloskills.ultplace.section.toggle").getString());
        renderSectionDivider(context, shapeSectionY,
                Text.translatable("murilloskills.ultplace.section.shape").getString());
        renderSectionDivider(context, configSectionY,
                Text.translatable("murilloskills.ultplace.section.config").getString());
        renderSectionDivider(context, actionsSectionY,
                Text.translatable("murilloskills.ultplace.section.actions").getString());

        super.render(context, mouseX, mouseY, delta);

        renderToggleContent(context);
        renderShapeContent(context);
        renderConfigContent(context);
        renderHints(context);
    }

    private void renderGradientBackground(DrawContext context) {
        for (int y = 0; y < this.height; y++) {
            float ratio = (float) y / Math.max(1, this.height);
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
                Text.translatable("murilloskills.ultplace.subtitle").copy().formatted(Formatting.GRAY),
                centerX, panelY + 26, palette.textMuted());
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

    private void renderToggleContent(DrawContext context) {
        boolean enabled = UltPlaceClientState.isEnabled();
        int w = 200;
        int x = this.width / 2 - w / 2;
        int y = toggleSectionY + 16;

        renderToggleCard(context, x, y, w,
                Text.translatable("murilloskills.ultplace.toggle").getString(),
                enabled);
    }

    private void renderToggleCard(DrawContext context, int x, int y, int w, String label, boolean enabled) {
        int bg = enabled ? palette.sectionBgActive() : palette.sectionBg();
        context.fill(x + 1, y + 1, x + w - 1, y + 19, bg);

        if (enabled) {
            context.fill(x + 1, y + 18, x + w - 1, y + 20, palette.accentGreen());
        }

        String prefix = enabled ? "\u25CF " : "\u25CB ";
        String stateText = enabled
                ? Text.translatable("murilloskills.ultplace.state.on").getString()
                : Text.translatable("murilloskills.ultplace.state.off").getString();
        String full = prefix + label + " · " + stateText;
        int color = enabled ? palette.textGreen() : palette.textGray();

        context.drawCenteredTextWithShadow(textRenderer, full, x + w / 2, y + 6, color);
    }

    private void renderShapeContent(DrawContext context) {
        UltPlaceShape activeShape = UltPlaceClientState.getSelectedShape();
        int columns = 4;
        int btnW = (panelW - PANEL_PADDING * 2 - (columns - 1) * 4) / columns;
        int totalW = btnW * columns + (columns - 1) * 4;
        int startX = this.width / 2 - totalW / 2;
        int top = shapeSectionY + 16;

        for (int i = 0; i < SELECTABLE_SHAPES.size(); i++) {
            UltPlaceShape shape = SELECTABLE_SHAPES.get(i);
            boolean active = shape == activeShape;
            int row = i / columns;
            int col = i % columns;
            int x = startX + col * (btnW + 4);
            int y = top + row * ROW_HEIGHT;

            int bg = active ? palette.sectionBgActive() : palette.sectionBg();
            context.fill(x + 1, y + 1, x + btnW - 1, y + 17, bg);

            if (active) {
                context.fill(x + 1, y + 16, x + btnW - 1, y + 18, palette.textAqua());
            }

            String name = Text.translatable(shape.getTranslationKey()).getString();
            int color = active ? palette.textAqua() : palette.textGray();

            int maxW = btnW - 4;
            if (textRenderer.getWidth(name) > maxW) {
                while (textRenderer.getWidth(name + ".") > maxW && name.length() > 2) {
                    name = name.substring(0, name.length() - 1);
                }
                name = name + ".";
            }
            context.drawCenteredTextWithShadow(textRenderer, name, x + btnW / 2, y + 5, color);
        }
    }

    private void renderConfigContent(DrawContext context) {
        UltPlaceShape current = UltPlaceClientState.getSelectedShape();
        int centerX = this.width / 2;

        // Shape name title
        String shapeName = Text.translatable(current.getTranslationKey()).getString();
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(shapeName).formatted(Formatting.WHITE, Formatting.BOLD),
                centerX, configSectionY + 16, palette.textWhite());

        int rowY = configSectionY + 16 + 14;
        int row = 0;
        int labelX = panelX + PANEL_PADDING * 2;

        if (SkillConfig.getUltPlaceShapeMaxSize(current) > 1) {
            int y = rowY + row * ROW_HEIGHT;
            context.drawTextWithShadow(textRenderer,
                    Text.translatable("murilloskills.ultplace.size").getString(),
                    labelX, y + 5, palette.textLight());
            renderValueBox(context, centerX, y, String.valueOf(UltPlaceClientState.getSize()));
            renderMaxHint(context, centerX, y, SkillConfig.getUltPlaceShapeMaxSize(current));
            row++;
        }

        if (SkillConfig.getUltPlaceShapeMaxLength(current) > 1) {
            int y = rowY + row * ROW_HEIGHT;
            context.drawTextWithShadow(textRenderer,
                    Text.translatable("murilloskills.ultplace.length").getString(),
                    labelX, y + 5, palette.textLight());
            renderValueBox(context, centerX, y, String.valueOf(UltPlaceClientState.getLength()));
            renderMaxHint(context, centerX, y, SkillConfig.getUltPlaceShapeMaxLength(current));
            row++;
        }

        if (UltPlaceShape.getVariantCount(current) > 1) {
            int y = rowY + row * ROW_HEIGHT;
            context.drawTextWithShadow(textRenderer,
                    Text.translatable("murilloskills.ultplace.variant").getString(),
                    labelX, y + 5, palette.textLight());
            String variantText = Text.translatable(UltPlaceShape.getVariantTranslationKey(
                    current, UltPlaceClientState.getVariant())).getString();
            renderValueBox(context, centerX, y, variantText);
        }

        if (countConfigRows(current) == 0) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("murilloskills.ultplace.no_options").formatted(Formatting.GRAY),
                    centerX, rowY + 4, palette.textMuted());
        }
    }

    private void renderValueBox(DrawContext context, int centerX, int y, String value) {
        int totalW = STEPPER_W + STEPPER_GAP + VALUE_BOX_W + STEPPER_GAP + STEPPER_W;
        int rowStartX = centerX - totalW / 2 + 30;
        int boxX = rowStartX + STEPPER_W + STEPPER_GAP;

        context.fill(boxX, y, boxX + VALUE_BOX_W, y + 18, palette.sectionBg());
        RenderingHelper.drawPanelBorder(context, boxX, y, VALUE_BOX_W, 18, palette.sectionBorder());

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(value).formatted(Formatting.AQUA),
                boxX + VALUE_BOX_W / 2, y + 5, palette.textAqua());
    }

    private void renderMaxHint(DrawContext context, int centerX, int y, int max) {
        int totalW = STEPPER_W + STEPPER_GAP + VALUE_BOX_W + STEPPER_GAP + STEPPER_W;
        int rowStartX = centerX - totalW / 2 + 30;
        int hintX = rowStartX + totalW + 6;
        context.drawTextWithShadow(textRenderer,
                Text.literal("/ " + max).formatted(Formatting.DARK_GRAY),
                hintX, y + 5, palette.textMuted());
    }

    private void renderHints(DrawContext context) {
        int centerX = this.width / 2;
        String hint = Text.translatable("murilloskills.ultplace.hints").getString();
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(hint).formatted(Formatting.DARK_GRAY),
                centerX, hintsY, palette.textMuted());
    }

    // ===== UTIL =====

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void syncToServer() {
        ClientPlayNetworking.send(UltPlaceClientState.toPayload());
    }

    private void refresh() {
        if (this.client != null) {
            clearChildren();
            init(this.client, this.width, this.height);
        }
    }

    @SuppressWarnings("unused")
    private MinecraftClient ensureClient() {
        return MinecraftClient.getInstance();
    }
}
