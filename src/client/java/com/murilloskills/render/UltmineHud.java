package com.murilloskills.render;

import com.murilloskills.MurilloSkillsClient;
import com.murilloskills.data.UltmineClientState;
import com.murilloskills.gui.ColorPalette;
import com.murilloskills.skills.UltmineShape;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * Bottom-right HUD shown while Ultmine is being aimed (key held, valid target).
 * Displays the targeted block name + count and the active shape/variant.
 */
public final class UltmineHud {

    private static final ColorPalette PALETTE = ColorPalette.premium();
    private static final int PADDING_H = 8;
    private static final int PADDING_V = 5;
    private static final int LINE_GAP = 2;
    private static final int MARGIN_X = 10;
    private static final int MARGIN_Y = 10;
    private static final int ULTMINE_GOLD = 0xFFFFAA00;
    private static final int ULTMINE_GOLD_LIGHT = 0xFFFFDD55;

    private UltmineHud() {
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!SkillConfig.isUltmineEnabled()) {
            return;
        }
        if (!MurilloSkillsClient.isVeinMinerKeyHeld()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) {
            return;
        }

        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos targetPos = blockHit.getBlockPos();
        BlockState targetState = client.world.getBlockState(targetPos);
        if (!isRenderableTarget(client.world, targetPos, targetState)) {
            return;
        }

        List<BlockPos> preview = UltmineClientState.getPreview();
        int count = countRenderable(client.world, preview);
        boolean previewMatches = !preview.isEmpty() && preview.contains(targetPos);
        if (count <= 0 || !previewMatches) {
            count = 1;
        }

        UltmineShape shape = UltmineClientState.getSelectedShape();
        int variant = UltmineClientState.getVariant();

        Text blockName = targetState.getBlock().getName().copy().formatted(Formatting.WHITE);
        Text countText = Text.literal(" x" + count).formatted(Formatting.GOLD, Formatting.BOLD);
        Text line1 = Text.literal("[Mine] ").formatted(Formatting.GOLD)
                .copy()
                .append(blockName)
                .append(countText);

        Text shapeName = Text.translatable(shape.getTranslationKey())
                .copy()
                .formatted(Formatting.GOLD, Formatting.BOLD);
        Text line2 = Text.literal("[Shape] ").formatted(Formatting.GOLD)
                .copy()
                .append(shapeName);
        if (UltmineShape.getVariantCount(shape) > 1) {
            Text variantText = Text.translatable(UltmineShape.getVariantTranslationKey(shape, variant))
                    .copy()
                    .formatted(Formatting.AQUA);
            line2 = line2.copy()
                    .append(Text.literal(" - ").formatted(Formatting.DARK_GRAY))
                    .append(variantText);
        }

        var tr = client.textRenderer;
        int line1W = tr.getWidth(line1);
        int line2W = tr.getWidth(line2);
        int textW = Math.max(line1W, line2W);
        int textH = tr.fontHeight;

        int panelW = textW + PADDING_H * 2;
        int panelH = textH * 2 + LINE_GAP + PADDING_V * 2;
        int x = context.getScaledWindowWidth() - MARGIN_X - panelW;
        int y = HudAnchorStack.claimBottomRight(context, panelH, MARGIN_Y, 4);

        // Subtle drop shadow behind the panel.
        context.fill(x + 2, y + 2, x + panelW + 2, y + panelH + 2, 0x66000000);

        // Body.
        context.fill(x, y, x + panelW, y + panelH, PALETTE.hudIndicatorBg());

        // Border.
        context.fill(x, y, x + panelW, y + 1, ULTMINE_GOLD);
        context.fill(x, y + panelH - 1, x + panelW, y + panelH, ULTMINE_GOLD);
        context.fill(x, y, x + 1, y + panelH, ULTMINE_GOLD);
        context.fill(x + panelW - 1, y, x + panelW, y + panelH, ULTMINE_GOLD);

        // Left accent stripe.
        context.fill(x + 1, y + 1, x + 3, y + panelH - 1, ULTMINE_GOLD_LIGHT);

        // Faint divider between the two lines.
        int dividerY = y + PADDING_V + textH + LINE_GAP / 2;
        context.fill(x + PADDING_H, dividerY, x + panelW - PADDING_H, dividerY + 1, 0x33FFAA00);

        int textX = x + PADDING_H;
        int line1Y = y + PADDING_V;
        int line2Y = line1Y + textH + LINE_GAP;
        context.drawTextWithShadow(tr, line1, textX, line1Y, PALETTE.textWhite());
        context.drawTextWithShadow(tr, line2, textX, line2Y, PALETTE.textWhite());
    }

    private static int countRenderable(World world, List<BlockPos> positions) {
        if (positions == null || positions.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (BlockPos pos : positions) {
            if (isRenderableTarget(world, pos, world.getBlockState(pos))) {
                count++;
            }
        }
        return count;
    }

    private static boolean isRenderableTarget(World world, BlockPos pos, BlockState state) {
        if (world == null || pos == null || state == null) {
            return false;
        }
        if (state.isAir()) {
            return false;
        }
        return state.getHardness(world, pos) >= 0.0f;
    }
}
