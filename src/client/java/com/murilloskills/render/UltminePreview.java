package com.murilloskills.render;

import com.murilloskills.MurilloSkillsClient;
import com.murilloskills.data.UltmineClientState;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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

        HitResult hit = client.crosshairTarget;
        BlockPos primary = null;
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos candidate = blockHit.getBlockPos();
            if (isRenderableTarget(client.world, candidate)) {
                primary = candidate;
            }
        }

        var preview = UltmineClientState.getPreview();
        LinkedHashSet<BlockPos> blocks = new LinkedHashSet<>();
        for (BlockPos pos : preview) {
            if (isRenderableTarget(client.world, pos)) {
                blocks.add(pos.toImmutable());
            }
        }

        if (primary != null && !blocks.isEmpty() && !blocks.contains(primary)) {
            // Ignore stale server preview from a previous target.
            blocks.clear();
        }

        if (blocks.isEmpty() && primary != null) {
            // Keep one-block outline visible while waiting for server preview update.
            blocks.add(primary.toImmutable());
        } else if (primary == null && !preview.isEmpty()) {
            primary = blocks.isEmpty() ? null : blocks.getFirst();
        }
        if (blocks.isEmpty()) {
            return;
        }

        VeinMinerPreview.renderOutlines(context, blocks, primary, 1.0f, 1.0f, 1.0f, 0.90f);
    }

    private static boolean isRenderableTarget(World world, BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        return state.getHardness(world, pos) >= 0.0f;
    }
}
