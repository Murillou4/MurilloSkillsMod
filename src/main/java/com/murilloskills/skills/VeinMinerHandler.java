package com.murilloskills.skills;

import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VeinMinerHandler {
    // Players currently holding the vein miner key
    private static final Set<UUID> HOLDING_KEY = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> ACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();
    // Per-player drops-to-inventory preference (overrides global config)
    private static final Map<UUID, Boolean> DROPS_TO_INVENTORY = new ConcurrentHashMap<>();

    /**
     * Map of blocks that should be considered equivalent for vein mining.
     * For example, deepslate variants match their regular counterparts.
     */
    private static final Map<Block, Block> BLOCK_EQUIVALENTS = buildBlockEquivalents();

    private static Map<Block, Block> buildBlockEquivalents() {
        Map<Block, Block> map = new java.util.HashMap<>();
        // Deepslate ore variants -> regular ore (bidirectional)
        addEquivalent(map, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
        addEquivalent(map, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        addEquivalent(map, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
        addEquivalent(map, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE);
        addEquivalent(map, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        addEquivalent(map, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        addEquivalent(map, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        addEquivalent(map, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        return Map.copyOf(map);
    }

    private static void addEquivalent(Map<Block, Block> map, Block a, Block b) {
        map.put(a, b);
        map.put(b, a);
    }

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

    /**
     * Toggle drops-to-inventory for a player.
     * Returns the new state (true = drops go to inventory).
     */
    public static boolean toggleDropsToInventory(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean current = DROPS_TO_INVENTORY.getOrDefault(uuid, SkillConfig.getVeinMinerDropsToInventory());
        boolean newState = !current;
        DROPS_TO_INVENTORY.put(uuid, newState);
        return newState;
    }

    /**
     * Check if drops-to-inventory is enabled for a player.
     * Falls back to global config if not set per-player.
     */
    public static boolean isDropsToInventory(ServerPlayerEntity player) {
        return DROPS_TO_INVENTORY.getOrDefault(player.getUuid(), SkillConfig.getVeinMinerDropsToInventory());
    }

    /**
     * Cleanup player state on disconnect.
     */
    public static void cleanupPlayerState(UUID playerUuid) {
        HOLDING_KEY.remove(playerUuid);
        DROPS_TO_INVENTORY.remove(playerUuid);
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
        Set<BlockPos> targets = collectConnectedBlocks(world, origin, originState, maxBlocks);

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
                if (isDropsToInventory(player)) {
                    breakWithInventoryDrops(player, world, pos, state, tool);
                } else {
                    world.breakBlock(pos, true, player);
                }
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

        return true;
    }

    private static void breakWithInventoryDrops(ServerPlayerEntity player, World world, BlockPos pos,
            BlockState state, ItemStack tool) {
        if (!(world instanceof ServerWorld serverWorld)) {
            world.breakBlock(pos, true, player);
            return;
        }

        List<ItemStack> drops = Block.getDroppedStacks(state, serverWorld, pos, world.getBlockEntity(pos), player, tool);
        for (ItemStack drop : drops) {
            ItemStack remaining = drop.copy();
            if (!player.getInventory().insertStack(remaining) && !remaining.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(serverWorld,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, remaining);
                serverWorld.spawnEntity(itemEntity);
            }
        }

        world.breakBlock(pos, false, player);
        state.onStacksDropped(serverWorld, pos, tool, true);
    }

    private static Set<BlockPos> collectConnectedBlocks(World world, BlockPos origin, BlockState originState,
            int maxBlocks) {
        Block originBlock = originState.getBlock();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty() && visited.size() < maxBlocks) {
            BlockPos current = queue.poll();

            // Check all 26 neighbors (including diagonals)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        if (visited.size() >= maxBlocks) return stripOrigin(visited, origin);

                        BlockPos neighbor = current.add(dx, dy, dz);
                        if (visited.contains(neighbor)) continue;

                        BlockState neighborState = world.getBlockState(neighbor);
                        if (isSameVeinBlock(originBlock, neighborState.getBlock())) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        return stripOrigin(visited, origin);
    }

    private static Set<BlockPos> stripOrigin(Set<BlockPos> visited, BlockPos origin) {
        visited.remove(origin);
        return visited;
    }

    /**
     * Check if two blocks should be considered the same for vein mining.
     * Handles: same block identity, deepslate/regular ore equivalents.
     * Redstone ore lit/unlit are already the same Block object in MC 1.21+.
     */
    public static boolean isSameVeinBlock(Block origin, Block candidate) {
        if (origin == candidate) {
            return true;
        }
        // Check if they are equivalent variants (e.g. deepslate_iron_ore <-> iron_ore)
        Block equivalent = BLOCK_EQUIVALENTS.get(origin);
        return equivalent != null && equivalent == candidate;
    }

    public static boolean isProcessing(ServerPlayerEntity player) {
        return ACTIVE_PLAYERS.contains(player.getUuid());
    }
}
