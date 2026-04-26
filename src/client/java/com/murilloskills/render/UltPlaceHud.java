package com.murilloskills.render;

import com.murilloskills.data.ClientSkillData;
import com.murilloskills.data.UltPlaceClientState;
import com.murilloskills.gui.ColorPalette;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.skills.UltPlaceAnchorMode;
import com.murilloskills.skills.UltPlaceRotationMode;
import com.murilloskills.skills.UltPlaceShape;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * HUD indicator for Builder UltPlace.
 * Shows when the toggle is active so the player never forgets the mode is on.
 */
public class UltPlaceHud {

    private static final ColorPalette PALETTE = ColorPalette.premium();
    private static final int PADDING_H = 8;
    private static final int PADDING_V = 4;
    private static final int MARGIN = 10;
    private static final int HOTBAR_OFFSET = 54;
    private static final int STACK_SPACING = 4;

    private UltPlaceHud() {
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!UltPlaceClientState.isEnabled()) {
            return;
        }
        if (!ClientSkillData.isSkillSelected(MurilloSkillsList.BUILDER)) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        UltPlaceShape shape = UltPlaceClientState.getSelectedShape();
        String shapeName = Text.translatable(shape.getTranslationKey()).getString();
        String dims = formatDimensions(shape);
        String anchor = formatAnchor(shape);
        String rotation = formatRotation(shape);

        Text icon = Text.literal("\u25A3 ").formatted(Formatting.GOLD);
        Text body = Text.translatable("murilloskills.hud.ultplace", shapeName, dims, anchor, rotation)
                .copy().formatted(Formatting.GOLD, Formatting.BOLD);
        Text fullText = icon.copy().append(body);
        if (shape.supportsSpacing() && UltPlaceClientState.getSpacing() > 1) {
            fullText = fullText.copy()
                    .append(Text.literal(" /").formatted(Formatting.GRAY))
                    .append(Text.literal(String.valueOf(UltPlaceClientState.getSpacing()))
                            .formatted(Formatting.AQUA, Formatting.BOLD));
        }

        int textWidth = client.textRenderer.getWidth(fullText);
        int textHeight = client.textRenderer.fontHeight;

        int panelW = textWidth + PADDING_H * 2;
        int panelH = textHeight + PADDING_V * 2;
        int x = MARGIN;
        int y = context.getScaledWindowHeight() - HOTBAR_OFFSET - panelH;
        if (AreaPlantingHud.isEnabled()) {
            y -= panelH + STACK_SPACING;
        }

        context.fill(x, y, x + panelW, y + panelH, PALETTE.hudIndicatorBg());

        context.fill(x, y, x + panelW, y + 1, PALETTE.hudIndicatorBorder());
        context.fill(x, y + panelH - 1, x + panelW, y + panelH, PALETTE.hudIndicatorBorder());
        context.fill(x, y, x + 1, y + panelH, PALETTE.hudIndicatorBorder());
        context.fill(x + panelW - 1, y, x + panelW, y + panelH, PALETTE.hudIndicatorBorder());

        context.fill(x + 1, y + 1, x + 3, y + panelH - 1, PALETTE.accentGold());

        context.drawTextWithShadow(client.textRenderer, fullText, x + PADDING_H, y + PADDING_V, PALETTE.textWhite());
    }

    private static String formatDimensions(UltPlaceShape shape) {
        int size = UltPlaceClientState.getSize();
        int length = UltPlaceClientState.getLength();
        int height = UltPlaceClientState.getHeight();
        boolean hasSize = SkillConfig.getUltPlaceShapeMaxSize(shape) > 1;
        boolean hasLength = SkillConfig.getUltPlaceShapeMaxLength(shape) > 1;
        boolean hasHeight = SkillConfig.getUltPlaceShapeMaxHeight(shape) > 1;

        if (hasSize && hasHeight && hasLength) {
            return size + "x" + height + "x" + length;
        }
        if (hasSize && hasLength) {
            return size + "x" + length;
        }
        if (hasHeight && hasLength) {
            return height + "x" + length;
        }
        if (hasSize) {
            return String.valueOf(size);
        }
        if (hasHeight) {
            return String.valueOf(height);
        }
        if (hasLength) {
            return String.valueOf(length);
        }
        return "1";
    }

    private static String formatAnchor(UltPlaceShape shape) {
        if (!shape.supportsAnchorMode()) {
            return Text.translatable(UltPlaceAnchorMode.CENTER.getTranslationKey()).getString();
        }
        return Text.translatable(UltPlaceClientState.getAnchorMode().getTranslationKey()).getString();
    }

    private static String formatRotation(UltPlaceShape shape) {
        if (!shape.supportsRotationMode()) {
            return Text.translatable(UltPlaceRotationMode.AUTO.getTranslationKey()).getString();
        }
        return Text.translatable(UltPlaceClientState.getRotationMode().getTranslationKey()).getString();
    }
}
