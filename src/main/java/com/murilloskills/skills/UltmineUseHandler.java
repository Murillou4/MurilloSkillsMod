package com.murilloskills.skills;

import com.murilloskills.utils.InventoryBlockFinder;
import com.murilloskills.utils.SkillConfig;
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
        if (!shouldHandleUltmineUse(serverPlayer)) {
            return false;
        }
        if (!ACTIVE_PLAYERS.add(serverPlayer.getUuid())) {
            return false;
        }

        try {
            ItemStack stack = context.getStack();
            int applications = 0;
            int maxApplications = SkillConfig.getUltmineMaxBlocksPerUse();

            for (BlockPos targetPos : getTargets(serverPlayer, world, context.getBlockPos(), context.getSide())) {
                if (applications >= maxApplications || stack.isEmpty()) {
                    break;
                }
                if (!canPlayerModify(serverWorld, serverPlayer, targetPos)) {
                    continue;
                }
                if (BoneMealItem.useOnFertilizable(stack, world, targetPos)) {
                    world.syncWorldEvent(WorldEvents.BONE_MEAL_USED, targetPos, 15);
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
        if (isSyntheticPlacementActive(player) || !shouldHandleUltmineUse(player)) {
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
            int maxExtraPlacements = Math.max(0, SkillConfig.getUltmineMaxBlocksPerUse() - 1);
            if (maxExtraPlacements <= 0) {
                return false;
            }

            SYNTHETIC_PLACEMENTS.add(player.getUuid());
            try {
                for (BlockPos targetPos : getTargets(player, world, origin, face)) {
                    if (targetPos.equals(origin)) {
                        continue;
                    }
                    if (placed >= maxExtraPlacements) {
                        break;
                    }
                    if (!canPlayerModify(world, player, targetPos)) {
                        continue;
                    }

                    ItemStack liveStack = InventoryBlockFinder.findMatchingBlock(player, sourceStack, hand);
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

    private static boolean shouldHandleUltmineUse(ServerPlayerEntity player) {
        return player != null
                && VeinMinerHandler.isVeinMinerActive(player)
                && VeinMinerHandler.shouldUseUltmine(player);
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
