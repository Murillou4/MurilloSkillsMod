package com.murilloskills.skills;

import com.murilloskills.utils.InventoryBlockFinder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executes right-click actions across the currently selected Ultmine shape.
 */
public final class UltmineUseHandler {
    private static final Set<UUID> ACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> SYNTHETIC_PLACEMENTS = ConcurrentHashMap.newKeySet();

    private UltmineUseHandler() {
    }

    public static boolean isSyntheticPlacementActive(ServerPlayerEntity player) {
        return player != null && SYNTHETIC_PLACEMENTS.contains(player.getUuid());
    }

    public static boolean handleBoneMealUse(ItemUsageContext context) {
        World world = context.getWorld();
        if (!(world instanceof ServerWorld serverWorld)
                || !(context.getPlayer() instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }
        if (!shouldHandleActiveUltmineUse(serverPlayer)) {
            return false;
        }

        return handleBoneMealUse(serverPlayer, serverWorld, context.getStack(), context.getBlockPos());
    }

    public static boolean handleUseRequest(ServerPlayerEntity player, BlockPos targetPos, Direction face, Hand hand,
            Vec3d hitPos) {
        return handleUseRequest(player, targetPos, face, hand, hitPos, null, 0, 0, 0);
    }

    public static boolean handleUseRequest(ServerPlayerEntity player, BlockPos targetPos, Direction face, Hand hand,
            Vec3d hitPos, UltmineShape requestedShape, int requestedDepth, int requestedLength, int requestedVariant) {
        if (player == null || targetPos == null || face == null || hand == null
                || !(player.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        if (!canUseUltmine(player)) {
            return false;
        }
        if (player.getEyePos().squaredDistanceTo(targetPos.toCenterPos()) > 81.0) {
            return false;
        }
        applyRequestedSelection(player, requestedShape, requestedDepth, requestedLength, requestedVariant);

        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() instanceof BoneMealItem) {
            return handleBoneMealUse(player, world, stack, targetPos);
        }
        if (stack.getItem() instanceof BlockItem blockItem) {
            return handleBlockPlacementFromTarget(player, world, targetPos, face, hand, stack, hitPos, blockItem);
        }

        return false;
    }

    private static void applyRequestedSelection(ServerPlayerEntity player, UltmineShape requestedShape,
            int requestedDepth, int requestedLength, int requestedVariant) {
        if (requestedShape != null) {
            VeinMinerHandler.setUltmineSelection(player, requestedShape, requestedDepth, requestedLength,
                    requestedVariant);
        }
    }

    private static boolean handleBoneMealUse(ServerPlayerEntity serverPlayer, ServerWorld world, ItemStack stack,
            BlockPos targetPos) {
        if (!ACTIVE_PLAYERS.add(serverPlayer.getUuid())) {
            return false;
        }

        try {
            int applications = 0;

            for (BlockPos pos : getBoneMealTargets(serverPlayer, world, targetPos)) {
                if (stack.isEmpty()) {
                    break;
                }
                if (!canPlayerModify(world, serverPlayer, pos)) {
                    continue;
                }
                if (BoneMealItem.useOnFertilizable(stack, world, pos)
                        || BoneMealItem.useOnGround(stack, world, pos, Direction.UP)) {
                    world.syncWorldEvent(WorldEvents.BONE_MEAL_USED, pos, 15);
                    applications++;
                }
            }

            return applications > 0;
        } finally {
            ACTIVE_PLAYERS.remove(serverPlayer.getUuid());
        }
    }

    public static boolean handleBlockPlacement(ServerPlayerEntity player, ServerWorld world, BlockPos origin,
            Direction face, Hand hand, ItemStack sourceStack, Vec3d hitPos) {
        if (player == null || world == null || origin == null || face == null || sourceStack == null) {
            return false;
        }
        if (isSyntheticPlacementActive(player) || !shouldHandleActiveUltmineUse(player)) {
            return false;
        }
        if (!(sourceStack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        if (!ACTIVE_PLAYERS.add(player.getUuid())) {
            return false;
        }

        try {
            int placed = 0;
            Set<BlockPos> targets = getPlacementTargets(player, world, origin, face);
            if (targets.size() <= 1) {
                return false;
            }

            SYNTHETIC_PLACEMENTS.add(player.getUuid());
            try {
                for (BlockPos targetPos : targets) {
                    if (targetPos.equals(origin)) {
                        continue;
                    }
                    if (!canPlayerModify(world, player, targetPos)) {
                        continue;
                    }

                    ItemStack liveStack = InventoryBlockFinder.pullMatchingBlockIntoHand(player, sourceStack, hand);
                    if (liveStack == null || liveStack.isEmpty()) {
                        break;
                    }

                    ActionResult result = blockItem.place(createPlacementContext(player, hand, liveStack, targetPos,
                            face, hitPos));
                    if (result != null && result.isAccepted()) {
                        placed++;
                    }
                }
            } finally {
                SYNTHETIC_PLACEMENTS.remove(player.getUuid());
            }
            return placed > 0;
        } finally {
            ACTIVE_PLAYERS.remove(player.getUuid());
        }
    }

    private static boolean handleBlockPlacementFromTarget(ServerPlayerEntity player, ServerWorld world,
            BlockPos targetPos, Direction face, Hand hand, ItemStack sourceStack, Vec3d hitPos, BlockItem blockItem) {
        if (!ACTIVE_PLAYERS.add(player.getUuid())) {
            return false;
        }

        try {
            int placed = 0;

            SYNTHETIC_PLACEMENTS.add(player.getUuid());
            try {
                for (BlockPos pos : getPlacementTargets(player, world, targetPos, face)) {
                    if (!canPlayerModify(world, player, pos)) {
                        continue;
                    }

                    ItemStack liveStack = InventoryBlockFinder.pullMatchingBlockIntoHand(player, sourceStack, hand);
                    if (liveStack == null || liveStack.isEmpty()) {
                        break;
                    }

                    ActionResult result = blockItem.place(createPlacementContext(player, hand, liveStack, pos,
                            face, hitPos));
                    if (result != null && result.isAccepted()) {
                        placed++;
                    }
                }
            } finally {
                SYNTHETIC_PLACEMENTS.remove(player.getUuid());
            }
            return placed > 0;
        } finally {
            ACTIVE_PLAYERS.remove(player.getUuid());
        }
    }

    private static boolean shouldHandleActiveUltmineUse(ServerPlayerEntity player) {
        return player != null
                && VeinMinerHandler.isVeinMinerActive(player)
                && canUseUltmine(player);
    }

    private static boolean canUseUltmine(ServerPlayerEntity player) {
        return VeinMinerHandler.shouldUseUltmine(player);
    }

    private static Set<BlockPos> getTargets(ServerPlayerEntity player, World world, BlockPos origin, Direction face) {
        LinkedHashSet<BlockPos> targets = new LinkedHashSet<>();
        if (origin == null || face == null) {
            return targets;
        }

        UltmineShape shape = VeinMinerHandler.getUltmineShape(player);
        int depth = VeinMinerHandler.getUltmineDepth(player);
        int length = VeinMinerHandler.getUltmineLength(player);
        for (BlockPos pos : VeinMinerHandler.getShapeBlocks(player, origin, shape, depth, length, face)) {
            if (pos != null && world.isInBuildLimit(pos)) {
                targets.add(pos.toImmutable());
            }
        }
        return targets;
    }

    private static Set<BlockPos> getBoneMealTargets(ServerPlayerEntity player, World world, BlockPos origin) {
        UltmineShape shape = VeinMinerHandler.getUltmineShape(player);
        if (shape == UltmineShape.LEGACY) {
            return getLegacyConnectedTargets(world, origin, VeinMinerHandler.getLegacyUltmineLimit(player));
        }

        return getTargets(player, world, origin, resolveBoneMealLayoutFace(player, shape));
    }

    private static Set<BlockPos> getPlacementTargets(ServerPlayerEntity player, World world, BlockPos origin,
            Direction clickedFace) {
        UltmineShape shape = VeinMinerHandler.getUltmineShape(player);
        if (shape == UltmineShape.LEGACY) {
            return getLegacyConnectedTargets(world, origin, VeinMinerHandler.getLegacyUltmineLimit(player));
        }

        return getTargets(player, world, origin, resolvePlacementLayoutFace(player, shape, clickedFace));
    }

    private static Direction resolveBoneMealLayoutFace(ServerPlayerEntity player, UltmineShape shape) {
        return switch (shape) {
            case LINE, STAIRS -> player.getHorizontalFacing();
            case SQUARE_20x20_D1 -> VeinMinerHandler.getUltmineVariant(player) == 0
                    ? Direction.UP
                    : player.getHorizontalFacing();
            default -> Direction.UP;
        };
    }

    private static Direction resolvePlacementLayoutFace(ServerPlayerEntity player, UltmineShape shape,
            Direction clickedFace) {
        Direction safeFace = clickedFace == null ? Direction.UP : clickedFace;
        return switch (shape) {
            case LINE, STAIRS -> safeFace.getAxis() == Direction.Axis.Y ? player.getHorizontalFacing() : safeFace;
            case SQUARE_20x20_D1 -> {
                int variant = VeinMinerHandler.getUltmineVariant(player);
                if (variant == 0) {
                    yield Direction.UP;
                }
                yield safeFace.getAxis() == Direction.Axis.Y ? player.getHorizontalFacing() : safeFace;
            }
            default -> safeFace;
        };
    }

    private static Set<BlockPos> getLegacyConnectedTargets(World world, BlockPos origin, int maxTargets) {
        LinkedHashSet<BlockPos> targets = new LinkedHashSet<>();
        if (origin == null || !world.isInBuildLimit(origin) || maxTargets <= 0) {
            return targets;
        }

        BlockState originState = world.getBlockState(origin);
        Block originBlock = originState.getBlock();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        LinkedHashSet<BlockPos> visited = new LinkedHashSet<>();
        BlockPos start = origin.toImmutable();
        queue.add(start);
        visited.add(start);

        int max = Math.max(1, maxTargets);
        while (!queue.isEmpty() && targets.size() < max) {
            BlockPos current = queue.poll();
            targets.add(current.toImmutable());

            for (int dx = -1; dx <= 1 && targets.size() < max; dx++) {
                for (int dz = -1; dz <= 1 && targets.size() < max; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos next = current.add(dx, 0, dz).toImmutable();
                    if (visited.contains(next) || !world.isInBuildLimit(next)) {
                        continue;
                    }
                    if (world.getBlockState(next).getBlock() != originBlock) {
                        continue;
                    }
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return targets;
    }

    private static ItemPlacementContext createPlacementContext(ServerPlayerEntity player, Hand hand, ItemStack stack,
            BlockPos pos, Direction face, Vec3d baseHitPos) {
        Vec3d safeHitPos = baseHitPos == null ? pos.toCenterPos() : baseHitPos;
        Vec3d localOffset = safeHitPos.subtract(
                Math.floor(safeHitPos.x),
                Math.floor(safeHitPos.y),
                Math.floor(safeHitPos.z));
        Vec3d hitPos = new Vec3d(pos.getX() + localOffset.x, pos.getY() + localOffset.y, pos.getZ() + localOffset.z);
        return new ItemPlacementContext(player, hand, stack, new BlockHitResult(hitPos, face, pos, false));
    }

    private static boolean canPlayerModify(ServerWorld world, ServerPlayerEntity player, BlockPos pos) {
        return player.canModifyAt(world, pos);
    }
}
