package com.murilloskills.render;

import com.murilloskills.data.TerminalMachineTargetClientState;
import com.murilloskills.gui.ColorPalette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class TerminalMachineTargetHud {
    private static final ColorPalette PALETTE = ColorPalette.premium();
    private static final int PADDING_H = 8;
    private static final int PADDING_V = 4;
    private static final int MARGIN = 10;
    private static final int HOTBAR_OFFSET = 78;

    private TerminalMachineTargetHud() {
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!TerminalMachineTargetClientState.hasTarget()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }
        BlockPos target = TerminalMachineTargetClientState.getTargetPos();
        if (target == null) {
            return;
        }

        Text fullText = Text.literal("* ").formatted(Formatting.AQUA)
                .append(Text.translatable("murilloskills.hud.terminal_machine_target",
                        target.getX(), target.getY(), target.getZ())
                        .formatted(Formatting.AQUA, Formatting.BOLD));

        int textWidth = client.textRenderer.getWidth(fullText);
        int textHeight = client.textRenderer.fontHeight;
        int panelW = textWidth + PADDING_H * 2;
        int panelH = textHeight + PADDING_V * 2;
        int x = MARGIN;
        int y = context.getScaledWindowHeight() - HOTBAR_OFFSET - panelH;

        context.fill(x, y, x + panelW, y + panelH, PALETTE.hudIndicatorBg());
        context.fill(x, y, x + panelW, y + 1, 0xFF27D7FF);
        context.fill(x, y + panelH - 1, x + panelW, y + panelH, 0xFF27D7FF);
        context.fill(x, y, x + 1, y + panelH, 0xFF27D7FF);
        context.fill(x + panelW - 1, y, x + panelW, y + panelH, 0xFF27D7FF);
        context.drawTextWithShadow(client.textRenderer, fullText, x + PADDING_H, y + PADDING_V,
                PALETTE.textWhite());
    }
}
