package com.murilloskills.render;

import com.murilloskills.MurilloSkillsClient;
import com.murilloskills.data.UltmineClientState;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashSet;

/**
 * Renders server-validated Ultmine preview blocks.
 */
public final class UltminePreview {
    private UltminePreview() {
    }

    public static void render(WorldRenderContext context) {
        if (!SkillConfig.isUltmineEnabled()) {
            return;
        }
        if (!MurilloSkillsClient.isVeinMinerKeyHeld()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }

        var preview = UltmineClientState.getPreview();
        if (preview.isEmpty()) {
            return;
        }

        HitResult hit = client.crosshairTarget;
        BlockPos primary = null;
        if (hit instanceof BlockHitResult blockHit) {
            primary = blockHit.getBlockPos();
        } else if (!preview.isEmpty()) {
            primary = preview.getFirst();
        }

        VeinMinerPreview.renderOutlines(context, new LinkedHashSet<>(preview), primary, 1.0f, 1.0f, 1.0f, 0.90f);
    }
}
