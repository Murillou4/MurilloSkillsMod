package com.murilloskills.skills;

import com.murilloskills.data.ModAttachments;
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
    private static final String TOGGLE_KEY = "global.veinMiner";
    private static final Set<UUID> ACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();

    private VeinMinerHandler() {
        // Utility class - prevent instantiation
    }

    public static boolean toggleVeinMiner(ServerPlayerEntity player) {
        var data = player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
        boolean enabled = data.skillToggles.getOrDefault(TOGGLE_KEY, true);
        boolean nowEnabled = !enabled;
        data.skillToggles.put(TOGGLE_KEY, nowEnabled);
        return nowEnabled;
    }

    public static boolean isVeinMinerEnabled(ServerPlayerEntity player) {
        var data = player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
        return data.skillToggles.getOrDefault(TOGGLE_KEY, true);
    }

    public static void handle(ServerPlayerEntity player, World world, BlockPos origin, BlockState originState) {
        if (world.isClient()) {
            return;
        }

        if (!isVeinMinerEnabled(player)) {
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

            for (BlockPos neighbor : new BlockPos[] {
                    current.north(), current.south(), current.east(), current.west(), current.up(), current.down()
            }) {
                if (visited.size() >= maxBlocks) {
                    break;
                }
                if (visited.contains(neighbor)) {
                    continue;
                }
                BlockState neighborState = world.getBlockState(neighbor);
                if (neighborState.equals(originState)) {
                    queue.add(neighbor);
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
