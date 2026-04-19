package com.murilloskills.skills;

import com.murilloskills.data.ModAttachments;
import com.murilloskills.network.UltmineResultS2CPayload;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
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
    // Per-player XP direct-to-player preference
    private static final Map<UUID, Boolean> XP_DIRECT_TO_PLAYER = new ConcurrentHashMap<>();

    // Ultmine per-player shape preferences
    private static final Map<UUID, UltmineShape> ULTMINE_SHAPE = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ULTMINE_DEPTH = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ULTMINE_LENGTH = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ULTMINE_VARIANT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ULTMINE_LAST_USE_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> ULTMINE_LAST_TARGET_POS = new ConcurrentHashMap<>();
    private static final Map<UUID, Direction> ULTMINE_LAST_TARGET_FACE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> ULTMINE_LAST_TARGET_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, List<PendingOriginCollection>> PENDING_ORIGIN_COLLECTIONS = new ConcurrentHashMap<>();

    // Magnet per-player state
    private static final Map<UUID, Boolean> MAGNET_ENABLED = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> MAGNET_RANGE = new ConcurrentHashMap<>();

    // Trash per-player item list
    private static final Map<UUID, List<String>> TRASH_LISTS = new ConcurrentHashMap<>();

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
     * Toggle XP direct-to-player for a player.
     * Returns the new state (true = XP goes directly to player).
     */
    public static boolean toggleXpDirectToPlayer(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean current = XP_DIRECT_TO_PLAYER.getOrDefault(uuid, false);
        boolean newState = !current;
        XP_DIRECT_TO_PLAYER.put(uuid, newState);
        return newState;
    }

    /**
     * Set XP direct-to-player for a player.
     */
    public static void setXpDirectToPlayer(ServerPlayerEntity player, boolean enabled) {
        XP_DIRECT_TO_PLAYER.put(player.getUuid(), enabled);
    }

    /**
     * Check if XP direct-to-player is enabled for a player.
     * Falls back to server config default if not set per-player.
     */
    public static boolean isXpDirectToPlayer(ServerPlayerEntity player) {
        return XP_DIRECT_TO_PLAYER.getOrDefault(player.getUuid(), SkillConfig.getVeinMinerXpDirectToPlayer());
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
        setUltmineSelection(player, shape, depth, length, 0);
    }

    public static void setUltmineSelection(ServerPlayerEntity player, UltmineShape shape, int depth, int length,
            int variant) {
        UltmineSelection normalized = normalizeSelection(shape, depth, length);
        ULTMINE_SHAPE.put(player.getUuid(), normalized.shape());
        ULTMINE_DEPTH.put(player.getUuid(), normalized.depth());
        ULTMINE_LENGTH.put(player.getUuid(), normalized.length());
        int maxVariant = UltmineShape.getVariantCount(normalized.shape()) - 1;
        ULTMINE_VARIANT.put(player.getUuid(), Math.max(0, Math.min(variant, maxVariant)));
    }

    public static int getUltmineVariant(ServerPlayerEntity player) {
        UltmineShape shape = getUltmineShape(player);
        int maxVariant = UltmineShape.getVariantCount(shape) - 1;
        return Math.max(0, Math.min(ULTMINE_VARIANT.getOrDefault(player.getUuid(), 0), maxVariant));
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
        XP_DIRECT_TO_PLAYER.remove(playerUuid);
        ULTMINE_SHAPE.remove(playerUuid);
        ULTMINE_DEPTH.remove(playerUuid);
        ULTMINE_LENGTH.remove(playerUuid);
        ULTMINE_VARIANT.remove(playerUuid);
        ULTMINE_LAST_USE_TICK.remove(playerUuid);
        ULTMINE_LAST_TARGET_POS.remove(playerUuid);
        ULTMINE_LAST_TARGET_FACE.remove(playerUuid);
        ULTMINE_LAST_TARGET_TICK.remove(playerUuid);
        PENDING_ORIGIN_COLLECTIONS.remove(playerUuid);
        MAGNET_ENABLED.remove(playerUuid);
        MAGNET_RANGE.remove(playerUuid);
        TRASH_LISTS.remove(playerUuid);
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
        boolean xpDirect = isXpDirectToPlayer(player) && world instanceof ServerWorld;

        // The origin block is already broken by the player before this handler runs.
        // Collect its dropped item first, even if there are no extra connected targets.
        if (inventoryDrops) {
            collectOriginDrops(player, (ServerWorld) world, origin);
            scheduleOriginCollection(player, world, origin);
        }
        if (xpDirect) {
            collectNearbyXp(player, (ServerWorld) world, origin);
        }

        if (targets.isEmpty()) {
            return;
        }

        for (BlockPos pos : targets) {
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            tool = player.getMainHandStack();
            if (!canBreakWith(tool, state, player)) {
                continue;
            }
            if (inventoryDrops) {
                breakWithInventoryDrops(player, world, pos, state, tool, xpDirect);
            } else if (xpDirect && world instanceof ServerWorld serverWorld) {
                breakWithGroundDropsAndDirectXp(player, serverWorld, pos, state, tool);
            } else {
                world.breakBlock(pos, true, player);
            }

            // Apply tool durability damage
            if (SkillConfig.getVeinMinerDamageToolPerBlock() && !player.isCreative() && !tool.isEmpty()) {
                tool.damage(1, player, EquipmentSlot.MAINHAND);
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
            if (!state.getFluidState().isEmpty()) {
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
        int variant = getUltmineVariant(player);
        return UltmineShapeCalculator.getShapeBlocks(origin, shape, depth, length, dir, player.getRotationVec(1.0f),
                variant);
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
        boolean xpDirect = isXpDirectToPlayer(player) && world instanceof ServerWorld;
        if (inventoryDrops) {
            collectOriginDrops(player, (ServerWorld) world, origin);
            scheduleOriginCollection(player, world, origin);
        }
        if (xpDirect) {
            collectNearbyXp(player, (ServerWorld) world, origin);
        }

        for (BlockPos pos : new LinkedHashSet<>(rawTargets)) {
            if (minedBlocks >= maxBlocks) {
                break;
            }
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (!state.getFluidState().isEmpty()) {
                continue;
            }
            if (state.getHardness(world, pos) < 0.0f) {
                continue;
            }
            if (!isUltmineBlockAllowed(state.getBlock())) {
                continue;
            }
            tool = player.getMainHandStack();
            if (!canBreakWith(tool, state, player)) {
                continue;
            }
            if (!canPlayerModify(world, player, pos)) {
                continue;
            }

            if (inventoryDrops) {
                breakWithInventoryDrops(player, world, pos, state, tool, xpDirect);
            } else if (xpDirect && world instanceof ServerWorld serverWorld) {
                breakWithGroundDropsAndDirectXp(player, serverWorld, pos, state, tool);
            } else {
                world.breakBlock(pos, true, player);
            }
            minedBlocks++;
            applyPerBlockSkillHandlers(player, world, pos, state);

            // Apply tool durability damage
            if (SkillConfig.getVeinMinerDamageToolPerBlock() && !player.isCreative() && !tool.isEmpty()) {
                tool.damage(1, player, EquipmentSlot.MAINHAND);
            }
        }

        if (inventoryDrops) {
            collectOriginDrops(player, (ServerWorld) world, origin);
            scheduleOriginCollection(player, world, origin);
        }

        if (minedBlocks <= 0) {
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
        float multiplier = SkillConfig.getVeinMinerLegacyUltmineMultiplier();
        int boostedLegacy = Math.max(1, Math.round(baseLegacy * multiplier));
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

    public static boolean shouldUseUltmine(ServerPlayerEntity player) {
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

        if (state.isToolRequired()) {
            return !tool.isEmpty() && tool.isSuitableFor(state);
        }

        return true;
    }

    private static void breakWithInventoryDrops(ServerPlayerEntity player, World world, BlockPos pos,
            BlockState state, ItemStack tool) {
        breakWithInventoryDrops(player, world, pos, state, tool, false);
    }

    private static void breakWithInventoryDrops(ServerPlayerEntity player, World world, BlockPos pos,
            BlockState state, ItemStack tool, boolean directXp) {
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
        if (directXp) {
            // Award vanilla XP directly without spawning orbs (avoids entity-query timing issues)
            state.onStacksDropped(serverWorld, pos, tool, false);
            int vanillaXp = getVanillaBlockXpDrop(serverWorld, state, tool);
            if (vanillaXp > 0) {
                player.addExperience(vanillaXp);
            }
        } else {
            state.onStacksDropped(serverWorld, pos, tool, true);
        }
    }

    /**
     * Breaks a block, drops items on the ground normally, but gives XP directly to the player.
     */
    private static void breakWithGroundDropsAndDirectXp(ServerPlayerEntity player, ServerWorld serverWorld,
            BlockPos pos, BlockState state, ItemStack tool) {
        List<ItemStack> drops = Block.getDroppedStacks(state, serverWorld, pos, serverWorld.getBlockEntity(pos), player, tool);
        serverWorld.breakBlock(pos, false, player);
        for (ItemStack drop : drops) {
            Block.dropStack(serverWorld, pos, drop);
        }
        // Award vanilla XP directly without spawning orbs
        state.onStacksDropped(serverWorld, pos, tool, false);
        int vanillaXp = getVanillaBlockXpDrop(serverWorld, state, tool);
        if (vanillaXp > 0) {
            player.addExperience(vanillaXp);
        }
    }

    /**
     * Collects nearby XP orbs (spawned by the game's normal block break) and grants them to the player.
     * Used only for the origin block which is broken by the game before our handler runs.
     */
    private static void collectNearbyXp(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        Box searchBox = new Box(pos).expand(2.0);
        List<ExperienceOrbEntity> orbs = world.getEntitiesByClass(
                ExperienceOrbEntity.class, searchBox, entity -> !entity.isRemoved());
        for (ExperienceOrbEntity orb : orbs) {
            player.addExperience(orb.getValue());
            orb.discard();
        }
    }

    /**
     * Calculates the vanilla XP amount a block would drop (matching Minecraft's ore XP tables).
     * Used when xpDirect is enabled to award XP without spawning orb entities.
     */
    private static int getVanillaBlockXpDrop(ServerWorld world, BlockState state, ItemStack tool) {
        if (BlockBreakHandler.hasSilkTouch(tool.getEnchantments())) return 0;

        Block block = state.getBlock();
        var random = world.getRandom();

        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE)
            return random.nextBetween(0, 2);
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE)
            return random.nextBetween(2, 5);
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE)
            return random.nextBetween(1, 5);
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE)
            return random.nextBetween(3, 7);
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE)
            return random.nextBetween(3, 7);
        if (block == Blocks.NETHER_QUARTZ_ORE)
            return random.nextBetween(2, 5);
        if (block == Blocks.NETHER_GOLD_ORE)
            return random.nextBetween(0, 1);
        if (block == Blocks.SCULK)
            return 1;

        return 0;
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
     * Reuses the same per-block skill flow as a normal manual break so ultmine
     * applies farmer passives, miner XP, streaks, and other side-effects
     * consistently for every extra block it breaks.
     */
    private static void applyPerBlockSkillHandlers(ServerPlayerEntity player, World world, BlockPos pos, BlockState state) {
        BlockBreakHandler.handle(player, world, pos, state);
        CropHarvestHandler.handle(player, world, pos, state);
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
        if (!SkillConfig.getVeinMinerMatchDeepslateVariants()) {
            return false;
        }
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

        return new UltmineSelection(safeShape, safeDepth, safeLength, 0);
    }

    // ===== MAGNET =====

    public static void setMagnetEnabled(ServerPlayerEntity player, boolean enabled) {
        MAGNET_ENABLED.put(player.getUuid(), enabled);
    }

    public static boolean isMagnetEnabled(ServerPlayerEntity player) {
        return MAGNET_ENABLED.getOrDefault(player.getUuid(), false);
    }

    public static void setMagnetRange(ServerPlayerEntity player, int range) {
        MAGNET_RANGE.put(player.getUuid(), Math.max(1, Math.min(range, 32)));
    }

    public static int getMagnetRange(ServerPlayerEntity player) {
        return MAGNET_RANGE.getOrDefault(player.getUuid(), 8);
    }

    /**
     * Pulls nearby items and XP orbs toward the player.
     * Called every tick from the server tick loop.
     */
    public static void tickMagnet(ServerPlayerEntity player) {
        if (!isMagnetEnabled(player)) {
            return;
        }
        if (!(player.getEntityWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        if (player.isSpectator() || player.isDead()) {
            return;
        }

        int range = getMagnetRange(player);
        Vec3d playerPos = player.getEntityPos();
        Box magnetBox = new Box(
                playerPos.x - range, playerPos.y - range, playerPos.z - range,
                playerPos.x + range, playerPos.y + range, playerPos.z + range);

        // Pull items
        List<ItemEntity> items = serverWorld.getEntitiesByClass(
                ItemEntity.class, magnetBox, entity -> !entity.isRemoved() && entity.isAlive());
        for (ItemEntity item : items) {
            // Skip items with pickup delay (freshly dropped by player)
            if (item.cannotPickup()) {
                continue;
            }
            Vec3d itemPos = item.getEntityPos();
            double dist = itemPos.distanceTo(playerPos);
            if (dist < 1.5) {
                // Close enough - teleport directly to player
                item.setPosition(playerPos.x, playerPos.y, playerPos.z);
            } else {
                // Pull toward player with velocity
                Vec3d direction = playerPos.subtract(itemPos).normalize();
                double speed = Math.min(0.6, 1.0 / dist * 2.0);
                item.setVelocity(direction.x * speed, direction.y * speed + 0.04, direction.z * speed);
                item.velocityModified = true;
            }
        }

        // Pull XP orbs
        List<ExperienceOrbEntity> orbs = serverWorld.getEntitiesByClass(
                ExperienceOrbEntity.class, magnetBox, entity -> !entity.isRemoved());
        for (ExperienceOrbEntity orb : orbs) {
            Vec3d orbPos = orb.getEntityPos();
            double dist = orbPos.distanceTo(playerPos);
            if (dist < 1.5) {
                orb.setPosition(playerPos.x, playerPos.y, playerPos.z);
            } else {
                Vec3d direction = playerPos.subtract(orbPos).normalize();
                double speed = Math.min(0.6, 1.0 / dist * 2.0);
                orb.setVelocity(direction.x * speed, direction.y * speed + 0.04, direction.z * speed);
                orb.velocityModified = true;
            }
        }
    }

    // ===== TRASH =====

    public static void setTrashList(ServerPlayerEntity player, List<String> trashItems) {
        TRASH_LISTS.put(player.getUuid(), new ArrayList<>(trashItems));
    }

    public static List<String> getTrashList(ServerPlayerEntity player) {
        return TRASH_LISTS.getOrDefault(player.getUuid(), List.of());
    }

    /**
     * Removes trash items from the player's inventory.
     * Called every 10 ticks to avoid excessive scanning.
     */
    public static void tickTrash(ServerPlayerEntity player) {
        List<String> trashList = TRASH_LISTS.get(player.getUuid());
        if (trashList == null || trashList.isEmpty()) {
            return;
        }
        if (player.isSpectator() || player.isDead()) {
            return;
        }
        // Only run every 10 ticks (0.5 seconds)
        if (player.age % 10 != 0) {
            return;
        }

        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            if (trashList.contains(itemId)) {
                inventory.setStack(i, ItemStack.EMPTY);
            }
        }
    }

    private record PendingOriginCollection(BlockPos pos, long expireTick) {
    }

    private record UltmineSelection(UltmineShape shape, int depth, int length, int variant) {
    }
}
