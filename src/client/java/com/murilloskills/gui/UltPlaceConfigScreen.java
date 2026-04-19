package com.murilloskills.gui;

import com.murilloskills.data.UltPlaceClientState;
import com.murilloskills.network.UltPlaceUndoC2SPayload;
import com.murilloskills.skills.UltPlaceShape;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;

public class UltPlaceConfigScreen extends Screen {
    private static final List<UltPlaceShape> TAB_SHAPES = Arrays.stream(UltPlaceShape.values())
            .filter(shape -> shape != UltPlaceShape.SINGLE)
            .toList();

    private final Screen parent;

    public UltPlaceConfigScreen(Screen parent) {
        super(Text.translatable("murilloskills.ultplace.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int topY = 24;

        this.addDrawableChild(ButtonWidget.builder(getToggleLabel(), button -> {
            UltPlaceClientState.toggleEnabled();
            UltPlaceClientState.clearPreview();
            syncToServer();
            rebuild();
        }).dimensions(centerX - 70, topY, 140, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("murilloskills.ultplace.undo"), button -> {
            ClientPlayNetworking.send(new UltPlaceUndoC2SPayload());
        }).dimensions(20, topY, 110, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(this.width - 90, topY, 70, 20)
                .build());

        int tabWidth = 92;
        int gap = 4;
        int columns = 4;
        int startX = centerX - ((tabWidth * columns) + (gap * (columns - 1))) / 2;
        int tabsY = topY + 34;

        for (int i = 0; i < TAB_SHAPES.size(); i++) {
            final UltPlaceShape shape = TAB_SHAPES.get(i);
            int row = i / columns;
            int column = i % columns;
            int x = startX + column * (tabWidth + gap);
            int y = tabsY + row * 24;

            this.addDrawableChild(ButtonWidget.builder(getShapeLabel(shape), button -> {
                UltPlaceClientState.selectShape(shape);
                syncToServer();
                rebuild();
            }).dimensions(x, y, tabWidth, 20).build());
        }

        UltPlaceShape currentShape = UltPlaceClientState.getSelectedShape();
        int controlsY = tabsY + ((TAB_SHAPES.size() + columns - 1) / columns) * 24 + 18;

        if (SkillConfig.getUltPlaceShapeMaxSize(currentShape) > 1) {
            addStepperRow(centerX, controlsY,
                    () -> UltPlaceClientState.adjustSize(-1),
                    () -> UltPlaceClientState.adjustSize(1));
        }

        if (SkillConfig.getUltPlaceShapeMaxLength(currentShape) > 1) {
            addStepperRow(centerX, controlsY + 28,
                    () -> UltPlaceClientState.adjustLength(-1),
                    () -> UltPlaceClientState.adjustLength(1));
        }

        if (UltPlaceShape.getVariantCount(currentShape) > 1) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> {
                UltPlaceClientState.adjustVariant(-1);
                syncToServer();
                rebuild();
            }).dimensions(centerX - 96, controlsY + 56, 20, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> {
                UltPlaceClientState.adjustVariant(1);
                syncToServer();
                rebuild();
            }).dimensions(centerX + 76, controlsY + 56, 20, 20).build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 8, 0xFFFFFF);

        UltPlaceShape currentShape = UltPlaceClientState.getSelectedShape();
        Text currentShapeLabel = Text.translatable(currentShape.getTranslationKey()).formatted(Formatting.AQUA);
        context.drawCenteredTextWithShadow(this.textRenderer, currentShapeLabel, centerX, 132, 0xFFFFFF);

        drawValueLine(context, 164, Text.translatable("murilloskills.ultplace.size"),
                String.valueOf(UltPlaceClientState.getSize()));
        drawValueLine(context, 192, Text.translatable("murilloskills.ultplace.length"),
                String.valueOf(UltPlaceClientState.getLength()));

        if (UltPlaceShape.getVariantCount(currentShape) > 1) {
            String variantText = Text.translatable(UltPlaceShape.getVariantTranslationKey(
                    currentShape, UltPlaceClientState.getVariant())).getString();
            drawValueLine(context, 220, Text.translatable("murilloskills.ultplace.variant"), variantText);
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    private void addStepperRow(int centerX, int y, Runnable onDecrease, Runnable onIncrease) {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> {
            onDecrease.run();
            syncToServer();
            rebuild();
        }).dimensions(centerX - 96, y, 20, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> {
            onIncrease.run();
            syncToServer();
            rebuild();
        }).dimensions(centerX + 76, y, 20, 20).build());
    }

    private void drawValueLine(DrawContext context, int y, Text label, String value) {
        int centerX = this.width / 2;
        Text line = Text.literal(label.getString() + ": " + value);
        context.drawCenteredTextWithShadow(this.textRenderer, line, centerX, y, 0xD8EFFF);
    }

    private Text getShapeLabel(UltPlaceShape shape) {
        Text text = Text.translatable(shape.getTranslationKey());
        if (shape == UltPlaceClientState.getSelectedShape()) {
            return text.copy().formatted(Formatting.AQUA, Formatting.BOLD);
        }
        return text;
    }

    private Text getToggleLabel() {
        String key = UltPlaceClientState.isEnabled()
                ? "murilloskills.ultplace.enabled.on"
                : "murilloskills.ultplace.enabled.off";
        return Text.translatable(key).formatted(UltPlaceClientState.isEnabled() ? Formatting.GREEN : Formatting.GRAY);
    }

    private void syncToServer() {
        ClientPlayNetworking.send(UltPlaceClientState.toPayload());
    }

    private void rebuild() {
        if (this.client != null) {
            this.clearAndInit();
        }
    }
}
