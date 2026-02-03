package com.murilloskills.skills;

import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VeinMinerHandler {
    // Players currently holding the vein miner key
    private static final Set<UUID> HOLDING_KEY = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> ACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();

    private VeinMinerHandler() {
        // Utility class - prevent instantiation
    }

    /**
     * Set vein miner state (key held or released).
     *
     * @param player    the player
     * @param activated true if key is being held, false if released
     */
    public static void setVeinMinerActive(ServerPlayerEntity player, boolean activated) {
        if (activated) {
            HOLDING_KEY.add(player.getUuid());
        } else {
            HOLDING_KEY.remove(player.getUuid());
        }
    }

    /**
     * Check if player is holding the vein miner key.
     */
    public static boolean isVeinMinerActive(ServerPlayerEntity player) {
        return HOLDING_KEY.contains(player.getUuid());
    }

    /**
     * Check if player is holding the vein miner key (by UUID).
     */
    public static boolean isVeinMinerActive(UUID playerUuid) {
        return HOLDING_KEY.contains(playerUuid);
    }

    public static void handle(ServerPlayerEntity player, World world, BlockPos origin, BlockState originState) {
        if (world.isClient()) {
            return;
        }

        if (!isVeinMinerActive(player)) {
            return;
        }

        if (ACTIVE_PLAYERS.contains(player.getUuid())) {
            return;
        }

        if (originState.getHardness(world, origin) < 0.0f) {
            return;
        }

        ItemStack tool = player.getMainHandStack();
        if (!canBreakWith(tool, originState, player)) {
            return;
        }

        int maxBlocks = Math.max(1, SkillConfig.getVeinMinerMaxBlocks());
        Set<BlockPos> targets = collectConnectedBlocks(world, origin, originState, maxBlocks + 1);

        if (targets.isEmpty()) {
            return;
        }

        ACTIVE_PLAYERS.add(player.getUuid());
        try {
            for (BlockPos pos : targets) {
                BlockState state = world.getBlockState(pos);
                if (state.isAir()) {
                    continue;
                }
                world.breakBlock(pos, true, player);
            }
        } finally {
            ACTIVE_PLAYERS.remove(player.getUuid());
        }
    }

    private static boolean canBreakWith(ItemStack tool, BlockState state, PlayerEntity player) {
        if (player.isCreative()) {
            return true;
        }

        if (state.isToolRequired()) {
            return tool.isSuitableFor(state);
        }

        return tool.isSuitableFor(state) || tool.isEmpty();
    }

    private static Set<BlockPos> collectConnectedBlocks(World world, BlockPos origin, BlockState originState,
            int maxBlocks) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        queue.add(origin);

        while (!queue.isEmpty() && visited.size() < maxBlocks) {
            BlockPos current = queue.poll();
            if (!visited.add(current)) {
                continue;
            }

            // Check all 26 neighbors (including diagonals)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        if (visited.size() >= maxBlocks) break;

                        BlockPos neighbor = current.add(dx, dy, dz);
                        if (visited.contains(neighbor)) continue;

                        BlockState neighborState = world.getBlockState(neighbor);
                        if (neighborState.equals(originState)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        visited.remove(origin);
        return visited;
    }

    public static boolean isProcessing(ServerPlayerEntity player) {
        return ACTIVE_PLAYERS.contains(player.getUuid());
    }
}
