package com.murilloskills.skills;

import com.murilloskills.data.ModAttachments;
import com.murilloskills.network.UltmineResultS2CPayload;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

    // Ultmine per-player shape preferences
    private static final Map<UUID, UltmineShape> ULTMINE_SHAPE = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ULTMINE_DEPTH = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ULTMINE_LENGTH = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ULTMINE_LAST_USE_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> ULTMINE_LAST_TARGET_POS = new ConcurrentHashMap<>();
    private static final Map<UUID, Direction> ULTMINE_LAST_TARGET_FACE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ULTMINE_LAST_TARGET_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, List<PendingOriginCollection>> PENDING_ORIGIN_COLLECTIONS = new ConcurrentHashMap<>();

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

    public static UltmineShape getUltmineShape(ServerPlayerEntity player) {
        return ULTMINE_SHAPE.getOrDefault(player.getUuid(), UltmineShape.S_3x3);
    }

    public static int getUltmineDepth(ServerPlayerEntity player) {
        UltmineShape shape = getUltmineShape(player);
        int defaultDepth = SkillConfig.getUltmineShapeDefaultDepth(shape);
        int maxDepth = SkillConfig.getUltmineShapeMaxDepth(shape);
        return Math.max(1, Math.min(ULTMINE_DEPTH.getOrDefault(player.getUuid(), defaultDepth), maxDepth));
    }

    public static int getUltmineLength(ServerPlayerEntity player) {
        UltmineShape shape = getUltmineShape(player);
        int defaultLength = SkillConfig.getUltmineShapeDefaultLength(shape);
        int maxLength = SkillConfig.getUltmineShapeMaxLength(shape);
        return Math.max(1, Math.min(ULTMINE_LENGTH.getOrDefault(player.getUuid(), defaultLength), maxLength));
    }

    public static void setUltmineSelection(ServerPlayerEntity player, UltmineShape shape, int depth, int length) {
        UltmineSelection normalized = normalizeSelection(shape, depth, length);
        ULTMINE_SHAPE.put(player.getUuid(), normalized.shape());
        ULTMINE_DEPTH.put(player.getUuid(), normalized.depth());
        ULTMINE_LENGTH.put(player.getUuid(), normalized.length());
    }

    public static void registerUltmineTarget(ServerPlayerEntity player, BlockPos targetPos, Direction face, long worldTime) {
        if (targetPos == null || face == null) {
            return;
        }

        UUID uuid = player.getUuid();
        ULTMINE_LAST_TARGET_POS.put(uuid, targetPos.toImmutable());
        ULTMINE_LAST_TARGET_FACE.put(uuid, face);
        ULTMINE_LAST_TARGET_TICK.put(uuid, worldTime);
    }

    /**
     * Cleanup player state on disconnect.
     */
    public static void cleanupPlayerState(UUID playerUuid) {
        HOLDING_KEY.remove(playerUuid);
        DROPS_TO_INVENTORY.remove(playerUuid);
        ULTMINE_SHAPE.remove(playerUuid);
        ULTMINE_DEPTH.remove(playerUuid);
        ULTMINE_LENGTH.remove(playerUuid);
        ULTMINE_LAST_USE_TICK.remove(playerUuid);
        ULTMINE_LAST_TARGET_POS.remove(playerUuid);
        ULTMINE_LAST_TARGET_FACE.remove(playerUuid);
        ULTMINE_LAST_TARGET_TICK.remove(playerUuid);
        PENDING_ORIGIN_COLLECTIONS.remove(playerUuid);
    }

    /**
     * Runs delayed pickup sweeps for the original broken block drops.
     * Called from the global server tick loop.
     */
    public static void tickPendingDropCollection(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        UUID uuid = player.getUuid();
        List<PendingOriginCollection> pending = PENDING_ORIGIN_COLLECTIONS.get(uuid);
        if (pending == null || pending.isEmpty()) {
            return;
        }

        long now = serverWorld.getTime();
        pending.removeIf(entry -> {
            collectOriginDrops(player, serverWorld, entry.pos());
            return now >= entry.expireTick();
        });

        if (pending.isEmpty()) {
            PENDING_ORIGIN_COLLECTIONS.remove(uuid);
        }
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

        ACTIVE_PLAYERS.add(player.getUuid());
        try {
            if (shouldUseUltmine(player)) {
                Direction direction = resolveMiningDirection(player, origin, world.getTime());
                executeUltmine(player, world, origin, originState, getUltmineShape(player), getUltmineDepth(player),
                        getUltmineLength(player), direction, true);
            } else {
                executeLegacyVeinMining(player, world, origin, originState, tool);
            }
        } finally {
            ACTIVE_PLAYERS.remove(player.getUuid());
        }
    }

    private static void executeLegacyVeinMining(ServerPlayerEntity player, World world, BlockPos origin, BlockState originState,
            ItemStack tool) {
        int maxBlocks = Math.max(1, SkillConfig.getVeinMinerMaxBlocks());
        Set<BlockPos> targets = collectConnectedBlocks(world, origin, originState, maxBlocks);
        boolean inventoryDrops = isDropsToInventory(player) && world instanceof ServerWorld;

        // The origin block is already broken by the player before this handler runs.
        // Collect its dropped item first, even if there are no extra connected targets.
        if (inventoryDrops) {
            collectOriginDrops(player, (ServerWorld) world, origin);
            scheduleOriginCollection(player, world, origin);
        }

        if (targets.isEmpty()) {
            return;
        }

        for (BlockPos pos : targets) {
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (inventoryDrops) {
                breakWithInventoryDrops(player, world, pos, state, tool);
            } else {
                world.breakBlock(pos, true, player);
            }
        }

        // Sweep again to catch drops that may spawn slightly later in the same tick.
        if (inventoryDrops) {
            collectOriginDrops(player, (ServerWorld) world, origin);
            scheduleOriginCollection(player, world, origin);
        }
    }

    /**
     * Preview helper used by preview request packet.
     * Always validated on server to prevent client-side spoofing.
     */
    public static List<BlockPos> getValidatedUltminePreview(ServerPlayerEntity player, World world, BlockPos origin) {
        return getValidatedUltminePreview(player, world, origin, resolveMiningDirection(player));
    }

    public static List<BlockPos> getValidatedUltminePreview(ServerPlayerEntity player, World world, BlockPos origin,
            Direction direction) {
        if (!shouldUseUltmine(player)) {
            return List.of();
        }

        UltmineSelection selection = getCurrentSelection(player);
        int maxBlocks = SkillConfig.getUltmineMaxBlocksPerUse();

        List<BlockPos> raw = getRawTargetsForShape(player, world, origin, world.getBlockState(origin), selection.shape(),
                selection.depth(), selection.length(), direction);
        if (raw.size() > maxBlocks) {
            return List.of();
        }

        ItemStack tool = player.getMainHandStack();
        List<BlockPos> valid = new ArrayList<>(raw.size());
        for (BlockPos pos : raw) {
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (state.getHardness(world, pos) < 0.0f) {
                continue;
            }
            if (!isUltmineBlockAllowed(state.getBlock())) {
                continue;
            }
            if (!canBreakWith(tool, state, player)) {
                continue;
            }
            if (!canPlayerModify(world, player, pos)) {
                continue;
            }
            valid.add(pos.toImmutable());
        }

        return valid;
    }

    public static List<BlockPos> getShapeBlocks(ServerPlayerEntity player, UltmineShape shape, int depth, int length,
            Direction dir) {
        return getShapeBlocks(player, player.getBlockPos(), shape, depth, length, dir);
    }

    public static List<BlockPos> getShapeBlocks(ServerPlayerEntity player, BlockPos origin, UltmineShape shape, int depth,
            int length, Direction dir) {
        return UltmineShapeCalculator.getShapeBlocks(origin, shape, depth, length, dir, player.getRotationVec(1.0f));
    }

    /**
     * Server-only execution for ultmine shape mining.
     */
    public static void executeUltmine(ServerPlayerEntity player, World world, BlockPos origin, BlockState originState,
            UltmineShape shape, int depth, int length, Direction dir, boolean sendResultPacket) {
        if (world.isClient()) {
            return;
        }
        if (!shouldUseUltmine(player)) {
            sendUltmineResult(player, false, 0, 0, "murilloskills.ultmine.result.not_allowed", sendResultPacket);
            return;
        }

        int cooldownTicks = SkillConfig.getUltmineCooldownTicks();
        long worldTime = world.getTime();
        if (cooldownTicks > 0) {
            long lastUse = ULTMINE_LAST_USE_TICK.getOrDefault(player.getUuid(), Long.MIN_VALUE);
            long elapsed = worldTime - lastUse;
            if (elapsed < cooldownTicks) {
                sendUltmineResult(player, false, 0, cooldownTicks - (int) elapsed,
                        "murilloskills.ultmine.result.cooldown", sendResultPacket);
                return;
            }
        }

        UltmineSelection selection = normalizeSelection(shape, depth, length);
        List<BlockPos> rawTargets = getRawTargetsForShape(player, world, origin, originState, selection.shape(),
                selection.depth(), selection.length(), dir);
        int requested = rawTargets.size();
        int maxBlocks = SkillConfig.getUltmineMaxBlocksPerUse();
        if (requested > maxBlocks) {
            sendUltmineResult(player, false, requested, maxBlocks, "murilloskills.ultmine.result.max_blocks",
                    sendResultPacket);
            return;
        }

        int xpCost = SkillConfig.getUltmineXpCostPerUse()
                + SkillConfig.getUltmineShapeCost(selection.shape(), selection.length());
        if (!player.isCreative() && xpCost > 0 && player.experienceLevel < xpCost) {
            sendUltmineResult(player, false, 0, xpCost, "murilloskills.ultmine.result.not_enough_xp", sendResultPacket);
            return;
        }

        ItemStack tool = player.getMainHandStack();
        int minedBlocks = 0;
        boolean inventoryDrops = isDropsToInventory(player) && world instanceof ServerWorld;
        if (inventoryDrops) {
            collectOriginDrops(player, (ServerWorld) world, origin);
            scheduleOriginCollection(player, world, origin);
        }

        for (BlockPos pos : new LinkedHashSet<>(rawTargets)) {
            if (minedBlocks >= maxBlocks) {
                break;
            }
            if (tool.isEmpty()) {
                break;
            }

            BlockState state = world.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (state.getHardness(world, pos) < 0.0f) {
                continue;
            }
            if (!isUltmineBlockAllowed(state.getBlock())) {
                continue;
            }
            if (!canBreakWith(tool, state, player)) {
                continue;
            }
            if (!canPlayerModify(world, player, pos)) {
                continue;
            }

            if (inventoryDrops) {
                breakWithInventoryDrops(player, world, pos, state, tool);
            } else {
                world.breakBlock(pos, true, player);
            }
            minedBlocks++;
        }

        if (inventoryDrops) {
            collectOriginDrops(player, (ServerWorld) world, origin);
            scheduleOriginCollection(player, world, origin);
        }

        if (minedBlocks <= 0) {
            sendUltmineResult(player, false, 0, requested, "murilloskills.ultmine.result.no_valid_blocks",
                    sendResultPacket);
            return;
        }

        if (!player.isCreative() && xpCost > 0) {
            player.addExperienceLevels(-xpCost);
        }

        ULTMINE_LAST_USE_TICK.put(player.getUuid(), worldTime);
        sendUltmineResult(player, true, minedBlocks, requested, "murilloskills.ultmine.result.success", sendResultPacket);
    }

    private static List<BlockPos> getRawTargetsForShape(ServerPlayerEntity player, World world, BlockPos origin,
            BlockState originState, UltmineShape shape, int depth, int length, Direction dir) {
        UltmineSelection selection = normalizeSelection(shape, depth, length);
        if (selection.shape() == UltmineShape.LEGACY) {
            int maxLegacyBlocks = getLegacyUltmineLimit();
            Set<BlockPos> connected = collectConnectedBlocks(world, origin, originState, maxLegacyBlocks);
            connected.add(origin.toImmutable());
            return new ArrayList<>(connected);
        }
        return getShapeBlocks(player, origin, selection.shape(), selection.depth(), selection.length(), dir);
    }

    private static int getLegacyUltmineLimit() {
        int baseLegacy = Math.max(1, SkillConfig.getVeinMinerMaxBlocks());
        int boostedLegacy = Math.max(1, Math.round(baseLegacy * 1.25f));
        int ultmineCap = Math.max(1, SkillConfig.getUltmineMaxBlocksPerUse());
        return Math.min(boostedLegacy, ultmineCap);
    }

    private static void sendUltmineResult(ServerPlayerEntity player, boolean success, int mined, int requestedOrValue,
            String messageKey, boolean sendResultPacket) {
        if (!sendResultPacket) {
            return;
        }
        ServerPlayNetworking.send(player, new UltmineResultS2CPayload(success, mined, requestedOrValue, messageKey));
    }

    private static Direction resolveMiningDirection(ServerPlayerEntity player) {
        float pitch = player.getPitch();
        if (pitch > 60.0f) {
            return Direction.DOWN;
        }
        if (pitch < -60.0f) {
            return Direction.UP;
        }
        return player.getHorizontalFacing();
    }

    private static Direction resolveMiningDirection(ServerPlayerEntity player, BlockPos origin, long worldTime) {
        UUID uuid = player.getUuid();
        BlockPos lastTarget = ULTMINE_LAST_TARGET_POS.get(uuid);
        Direction lastFace = ULTMINE_LAST_TARGET_FACE.get(uuid);
        long lastTargetTick = ULTMINE_LAST_TARGET_TICK.getOrDefault(uuid, Long.MIN_VALUE);

        if (lastTarget != null && lastFace != null && lastTarget.equals(origin) && (worldTime - lastTargetTick) <= 10) {
            return lastFace;
        }

        return resolveMiningDirection(player);
    }

    private static boolean shouldUseUltmine(ServerPlayerEntity player) {
        if (!SkillConfig.isUltmineEnabled()) {
            return false;
        }
        if (!player.hasPermissionLevel(SkillConfig.getUltminePermissionLevel())) {
            return false;
        }
        if (!SkillConfig.isUltmineRequireMinerMaster()) {
            return true;
        }
        try {
            var data = player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
            var minerStats = data.getSkill(MurilloSkillsList.MINER);
            return minerStats.level >= SkillConfig.getMinerMasterLevel() || minerStats.prestige > 0;
        } catch (Exception ignored) {
            return false;
        }
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

    private static boolean canPlayerModify(World world, ServerPlayerEntity player, BlockPos pos) {
        if (world instanceof ServerWorld serverWorld) {
            return player.canModifyAt(serverWorld, pos);
        }
        return true;
    }

    private static boolean canBreakWith(ItemStack tool, BlockState state, PlayerEntity player) {
        if (player.isCreative()) {
            return true;
        }
        if (tool.isEmpty()) {
            return false;
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

    private static void collectOriginDrops(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        Box searchBox = new Box(pos).expand(0.5);
        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, searchBox, entity -> !entity.isRemoved());
        for (ItemEntity itemEntity : items) {
            ItemStack stack = itemEntity.getStack();
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remaining = stack.copy();
            if (player.getInventory().insertStack(remaining) && remaining.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setStack(remaining);
            }
        }
    }

    private static void scheduleOriginCollection(ServerPlayerEntity player, World world, BlockPos pos) {
        long now = world.getTime();
        long expireTick = now + 8; // Keep sweeping for a short period after the break event.
        UUID uuid = player.getUuid();
        List<PendingOriginCollection> pending = PENDING_ORIGIN_COLLECTIONS.computeIfAbsent(uuid, ignored -> new ArrayList<>());
        pending.add(new PendingOriginCollection(pos.toImmutable(), expireTick));
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
                        if (dx == 0 && dy == 0 && dz == 0)
                            continue;
                        if (visited.size() >= maxBlocks)
                            return stripOrigin(visited, origin);

                        BlockPos neighbor = current.add(dx, dy, dz);
                        if (visited.contains(neighbor))
                            continue;

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

    private static UltmineSelection getCurrentSelection(ServerPlayerEntity player) {
        return normalizeSelection(getUltmineShape(player), getUltmineDepth(player), getUltmineLength(player));
    }

    private static UltmineSelection normalizeSelection(UltmineShape shape, int depth, int length) {
        UltmineShape safeShape = shape == null ? UltmineShape.S_3x3 : shape;
        int safeDepth = Math.max(1, Math.min(depth, SkillConfig.getUltmineShapeMaxDepth(safeShape)));
        int safeLength = Math.max(1, Math.min(length, SkillConfig.getUltmineShapeMaxLength(safeShape)));

        if (depth <= 0) {
            safeDepth = SkillConfig.getUltmineShapeDefaultDepth(safeShape);
        }
        if (length <= 0) {
            safeLength = SkillConfig.getUltmineShapeDefaultLength(safeShape);
        }

        return new UltmineSelection(safeShape, safeDepth, safeLength);
    }

    private record PendingOriginCollection(BlockPos pos, long expireTick) {
    }

    private record UltmineSelection(UltmineShape shape, int depth, int length) {
    }
}
