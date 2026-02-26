package com.murilloskills.render;

import com.murilloskills.MurilloSkillsClient;
import com.murilloskills.data.UltmineClientState;
import com.murilloskills.skills.UltmineShape;
import com.murilloskills.skills.UltmineShapeCalculator;
import com.murilloskills.skills.VeinMinerHandler;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
            primary = blockHit.getBlockPos();
        }

        var preview = UltmineClientState.getPreview();
        LinkedHashSet<BlockPos> blocks = new LinkedHashSet<>(preview);
        if (blocks.isEmpty() && primary != null) {
            blocks.addAll(buildClientSidePreview(client, primary));
            if (blocks.isEmpty()) {
                // Keep one-block outline visible while waiting for server preview update.
                blocks.add(primary.toImmutable());
            }
        } else if (primary == null && !preview.isEmpty()) {
            primary = preview.getFirst();
        }
        if (blocks.isEmpty()) {
            return;
        }

        VeinMinerPreview.renderOutlines(context, blocks, primary, 1.0f, 1.0f, 1.0f, 0.90f);
    }

    private static Set<BlockPos> buildClientSidePreview(MinecraftClient client, BlockPos origin) {
        if (client.world == null || client.player == null) {
            return Set.of();
        }

        UltmineShape shape = UltmineClientState.getSelectedShape();
        int depth = UltmineClientState.getDepth();
        int length = UltmineClientState.getLength();
        int maxBlocks = SkillConfig.getUltmineMaxBlocksPerUse();

        List<BlockPos> raw = getRawTargetsForShape(client, origin, shape, depth, length);
        if (raw.size() > maxBlocks) {
            return Set.of();
        }

        ItemStack tool = client.player.getMainHandStack();
        LinkedHashSet<BlockPos> valid = new LinkedHashSet<>(raw.size());
        for (BlockPos pos : raw) {
            BlockState state = client.world.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (state.getHardness(client.world, pos) < 0.0f) {
                continue;
            }
            if (!isUltmineBlockAllowed(state.getBlock())) {
                continue;
            }
            if (!canBreakWith(tool, state, client)) {
                continue;
            }
            valid.add(pos.toImmutable());
        }

        return valid;
    }

    private static List<BlockPos> getRawTargetsForShape(MinecraftClient client, BlockPos origin, UltmineShape shape, int depth,
            int length) {
        if (shape == UltmineShape.LEGACY) {
            BlockState originState = client.world.getBlockState(origin);
            int maxLegacyBlocks = getLegacyUltmineLimit();
            Set<BlockPos> connected = collectConnectedBlocks(client.world, origin, originState, maxLegacyBlocks);
            connected.add(origin.toImmutable());
            return new ArrayList<>(connected);
        }

        Direction direction = resolveMiningDirection(client);
        return UltmineShapeCalculator.getShapeBlocks(origin, shape, depth, length, direction, client.player.getRotationVec(1.0f));
    }

    private static int getLegacyUltmineLimit() {
        int baseLegacyBlocks = Math.max(1, SkillConfig.getVeinMinerMaxBlocks());
        int boostedLegacyBlocks = Math.max(1, Math.round(baseLegacyBlocks * 1.25f));
        int ultmineCap = Math.max(1, SkillConfig.getUltmineMaxBlocksPerUse());
        return Math.min(boostedLegacyBlocks, ultmineCap);
    }

    private static Set<BlockPos> collectConnectedBlocks(net.minecraft.world.World world, BlockPos origin, BlockState originState,
            int maxBlocks) {
        Block originBlock = originState.getBlock();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty() && visited.size() < maxBlocks) {
            BlockPos current = queue.poll();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        if (visited.size() >= maxBlocks) {
                            visited.remove(origin);
                            return visited;
                        }

                        BlockPos neighbor = current.add(dx, dy, dz);
                        if (visited.contains(neighbor)) {
                            continue;
                        }

                        BlockState neighborState = world.getBlockState(neighbor);
                        if (VeinMinerHandler.isSameVeinBlock(originBlock, neighborState.getBlock())) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        visited.remove(origin);
        return visited;
    }

    private static Direction resolveMiningDirection(MinecraftClient client) {
        float pitch = client.player.getPitch();
        if (pitch > 60.0f) {
            return Direction.DOWN;
        }
        if (pitch < -60.0f) {
            return Direction.UP;
        }
        return client.player.getHorizontalFacing();
    }

    private static boolean isUltmineBlockAllowed(Block block) {
        String blockId = Registries.BLOCK.getId(block).toString();
        List<String> whitelist = SkillConfig.getUltmineBlockWhitelist();
        List<String> blacklist = SkillConfig.getUltmineBlockBlacklist();

        if (!whitelist.isEmpty() && !whitelist.contains(blockId)) {
            return false;
        }
        return !blacklist.contains(blockId);
    }

    private static boolean canBreakWith(ItemStack tool, BlockState state, MinecraftClient client) {
        if (client.player.isCreative()) {
            return true;
        }

        if (state.isToolRequired()) {
            return tool.isSuitableFor(state);
        }

        return true;
    }
}
