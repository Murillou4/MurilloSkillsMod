package com.murilloskills.render;

import com.murilloskills.data.ClientSkillData;
import com.murilloskills.gui.ColorPalette;
import com.murilloskills.impl.FarmerSkill;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * HUD overlay that shows crop growth boost info when a Farmer player
 * looks at a crop block. Similar to Jade/WTHIT tooltip but built-in.
 * Shows the current Fertile Ground growth bonus from the Farmer skill.
 */
public class FarmerCropHud {

    private static final ColorPalette PALETTE = ColorPalette.premium();
    private static final int PADDING_H = 8;
    private static final int PADDING_V = 4;

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) {
            return;
        }

        // Check if player has Farmer selected with Fertile Ground perk
        if (!ClientSkillData.isSkillSelected(MurilloSkillsList.FARMER)) {
            return;
        }

        int farmerLevel = ClientSkillData.get(MurilloSkillsList.FARMER).level;
        if (farmerLevel < SkillConfig.FARMER_FERTILE_GROUND_LEVEL) {
            return;
        }

        // Check if player is looking at a crop
        HitResult hitResult = client.crosshairTarget;
        if (!(hitResult instanceof BlockHitResult blockHit) || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockState state = client.world.getBlockState(blockHit.getBlockPos());
        if (!(state.getBlock() instanceof CropBlock cropBlock)) {
            return;
        }

        // Calculate growth info
        int currentAge = cropBlock.getAge(state);
        int maxAge = cropBlock.getMaxAge();
        boolean isMature = cropBlock.isMature(state);

        int growthBoost = FarmerSkill.getFertileGroundGrowthPercent(farmerLevel);

        // Build display text
        int screenWidth = context.getScaledWindowWidth();
        int centerX = screenWidth / 2;
        int y = context.getScaledWindowHeight() / 2 + 16; // Below crosshair

        // Line 1: Growth stage
        String stageText = isMature ? "Mature" : (currentAge + "/" + maxAge);
        Text line1 = Text.literal("\uD83C\uDF3E ")
                .append(Text.translatable(state.getBlock().getTranslationKey()))
                .append(Text.literal(" [" + stageText + "]"))
                .formatted(Formatting.GREEN);

        // Line 2: Boost info
        Text line2 = Text.literal("  \u2B06 ")
                .append(Text.translatable("murilloskills.hud.crop_boost", Integer.toString(growthBoost)))
                .formatted(Formatting.YELLOW);

        int line1Width = client.textRenderer.getWidth(line1);
        int line2Width = client.textRenderer.getWidth(line2);
        int maxWidth = Math.max(line1Width, line2Width);

        int panelW = maxWidth + PADDING_H * 2;
        int panelH = client.textRenderer.fontHeight * 2 + PADDING_V * 3;
        int panelX = centerX - panelW / 2;

        // Background panel
        context.fill(panelX, y, panelX + panelW, y + panelH, 0xCC1A1A2E);

        // Border (green for farmer)
        context.fill(panelX, y, panelX + panelW, y + 1, 0xFF44BB44);
        context.fill(panelX, y + panelH - 1, panelX + panelW, y + panelH, 0xFF44BB44);
        context.fill(panelX, y, panelX + 1, y + panelH, 0xFF44BB44);
        context.fill(panelX + panelW - 1, y, panelX + panelW, y + panelH, 0xFF44BB44);

        // Draw text
        context.drawTextWithShadow(client.textRenderer, line1,
                panelX + PADDING_H, y + PADDING_V, 0xFFFFFFFF);
        context.drawTextWithShadow(client.textRenderer, line2,
                panelX + PADDING_H, y + PADDING_V + client.textRenderer.fontHeight + PADDING_V, 0xFFFFFFFF);
    }
}
