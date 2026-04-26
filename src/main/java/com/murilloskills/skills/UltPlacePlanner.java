package com.murilloskills.skills;

import com.murilloskills.mixin.BlockItemInvoker;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AutomaticItemPlacementContext;
import net.minecraft.item.BedItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TallBlockItem;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared UltPlace planner used by client preview and server execution.
 */
public final class UltPlacePlanner {
    public static final String COMPLEX_FALLBACK_KEY = "murilloskills.ultplace.fallback.complex_item";

    private UltPlacePlanner() {
    }

    public static UltPlacePlan planPreview(World world, PlayerEntity player, Hand hand, ItemStack stack,
            BlockPos targetPos, Direction face, Vec3d hitPos, UltPlaceSelection selection, int maxPlacements) {
        if (!(stack.getItem() instanceof BlockItem blockItem) || world == null || player == null || targetPos == null
                || face == null || maxPlacements <= 0) {
            return UltPlacePlan.empty(targetPos);
        }

        ItemPlacementContext baseContext = new ItemPlacementContext(player, hand, stack, createHitResult(targetPos, face, hitPos));
        ItemPlacementContext resolvedContext = blockItem.getPlacementContext(baseContext);
        if (resolvedContext == null) {
            return UltPlacePlan.empty(targetPos);
        }

        BlockPos origin = resolvedContext.getBlockPos().toImmutable();
        Vec3d localHitOffset = resolvedContext.getHitPos().subtract(origin.getX(), origin.getY(), origin.getZ());
        return buildPlan(world, player, hand, stack, blockItem, origin, face, localHitOffset, selection,
                maxPlacements, true);
    }

    public static UltPlacePlan planFromOrigin(World world, PlayerEntity player, Hand hand, ItemStack stack,
            BlockPos origin, Direction face, Vec3d hitPos, UltPlaceSelection selection, int maxPlacements) {
        if (!(stack.getItem() instanceof BlockItem blockItem) || world == null || origin == null || face == null
                || maxPlacements <= 0) {
            return UltPlacePlan.empty(origin);
        }

        Vec3d safeHitPos = hitPos == null ? defaultHitPos(origin, face) : hitPos;
        Vec3d localHitOffset = safeHitPos.subtract(origin.getX(), origin.getY(), origin.getZ());
        return buildPlan(world, player, hand, stack, blockItem, origin.toImmutable(), face, localHitOffset,
                selection, maxPlacements, false);
    }

    public static PlannedPlacement predictPrimaryPlacement(BlockItem blockItem, ItemPlacementContext context) {
        if (blockItem == null || context == null) {
            return null;
        }
        ItemPlacementContext resolvedContext = blockItem.getPlacementContext(context);
        if (resolvedContext == null) {
            return null;
        }
        BlockPos placementPos = resolvedContext.getBlockPos().toImmutable();
        BlockState state = ((BlockItemInvoker) blockItem).murilloskills$invokeGetPlacementState(resolvedContext);
        if (state == null || !((BlockItemInvoker) blockItem).murilloskills$invokeCanPlace(resolvedContext, state)) {
            return null;
        }
        return new PlannedPlacement(placementPos, state, resolveFootprint(blockItem, placementPos, state));
    }

    private static UltPlacePlan buildPlan(World world, PlayerEntity player, Hand hand, ItemStack stack,
            BlockItem blockItem, BlockPos origin, Direction face, Vec3d localHitOffset, UltPlaceSelection selection,
            int maxPlacements, boolean includeOrigin) {
        UltPlaceSelection safeSelection = normalizeSelection(selection);
        Vec3d lookVec = player != null ? player.getRotationVec(1.0f) : Vec3d.of(face.getVector());
        Direction horizontalFacing = player != null ? player.getHorizontalFacing() : Direction.NORTH;

        List<BlockPos> layout = UltPlaceShapeCalculator.getShapeBlocks(origin, safeSelection.shape(), safeSelection.size(),
                safeSelection.length(), safeSelection.height(), face, lookVec, horizontalFacing, safeSelection.variant(),
                safeSelection.anchorMode(), safeSelection.rotationMode(), safeSelection.spacing());
        if (layout.isEmpty()) {
            return UltPlacePlan.empty(origin);
        }

        String fallbackReason = null;
        if (layout.size() > 1 && !supportsMassPlacement(blockItem)) {
            fallbackReason = COMPLEX_FALLBACK_KEY;
            if (includeOrigin) {
                layout = List.of(origin.toImmutable());
            } else {
                layout = List.of();
            }
        }

        List<PlannedPlacement> placements = new ArrayList<>();
        Map<BlockPos, PreviewBlock> previewBlocks = new LinkedHashMap<>();
        Set<BlockPos> occupied = new LinkedHashSet<>();

        for (BlockPos candidate : layout) {
            if (!includeOrigin && candidate.equals(origin)) {
                continue;
            }
            if (placements.size() >= maxPlacements) {
                break;
            }

            PlannedPlacement placement = predictPlacementAt(world, player, hand, stack, blockItem, candidate, face,
                    localHitOffset);
            if (placement == null) {
                if (candidate.equals(origin) && includeOrigin) {
                    return UltPlacePlan.empty(origin);
                }
                continue;
            }
            if (intersects(placement.footprint(), occupied)) {
                continue;
            }

            placements.add(placement);
            for (PreviewBlock previewBlock : placement.footprint()) {
                occupied.add(previewBlock.pos());
                previewBlocks.putIfAbsent(previewBlock.pos(), previewBlock);
            }
        }

        return new UltPlacePlan(origin, List.copyOf(placements), List.copyOf(previewBlocks.values()), fallbackReason);
    }

    private static PlannedPlacement predictPlacementAt(World world, PlayerEntity player, Hand hand, ItemStack stack,
            BlockItem blockItem, BlockPos candidate, Direction face, Vec3d localHitOffset) {
        ItemPlacementContext context = createPlacementContext(world, player, hand, stack, candidate, face, localHitOffset);
        ItemPlacementContext resolvedContext = blockItem.getPlacementContext(context);
        if (resolvedContext == null || !resolvedContext.getBlockPos().equals(candidate) || !resolvedContext.canPlace()) {
            return null;
        }

        BlockState state = ((BlockItemInvoker) blockItem).murilloskills$invokeGetPlacementState(resolvedContext);
        if (state == null || !((BlockItemInvoker) blockItem).murilloskills$invokeCanPlace(resolvedContext, state)) {
            return null;
        }

        return new PlannedPlacement(candidate.toImmutable(), state, resolveFootprint(blockItem, candidate, state));
    }

    private static ItemPlacementContext createPlacementContext(World world, PlayerEntity player, Hand hand, ItemStack stack,
            BlockPos candidate, Direction face, Vec3d localHitOffset) {
        Vec3d hitPos = new Vec3d(
                candidate.getX() + localHitOffset.x,
                candidate.getY() + localHitOffset.y,
                candidate.getZ() + localHitOffset.z);
        if (player != null) {
            return new ItemPlacementContext(player, hand, stack, createHitResult(candidate, face, hitPos));
        }
        return new AutomaticItemPlacementContext(world, candidate, face.getAxis().isHorizontal() ? face : Direction.NORTH,
                stack, face);
    }

    private static BlockHitResult createHitResult(BlockPos candidate, Direction face, Vec3d hitPos) {
        Vec3d safeHitPos = hitPos == null ? defaultHitPos(candidate, face) : hitPos;
        return new BlockHitResult(safeHitPos, face, candidate, false);
    }

    private static Vec3d defaultHitPos(BlockPos candidate, Direction face) {
        Vec3d center = candidate.toCenterPos();
        return center.add(face.getOffsetX() * 0.25, face.getOffsetY() * 0.25, face.getOffsetZ() * 0.25);
    }

    private static List<PreviewBlock> resolveFootprint(BlockItem blockItem, BlockPos placementPos, BlockState state) {
        List<PreviewBlock> footprint = new ArrayList<>();
        footprint.add(new PreviewBlock(placementPos.toImmutable(), state, true));

        if (blockItem instanceof BedItem && state.contains(Properties.BED_PART)
                && state.contains(Properties.HORIZONTAL_FACING)) {
            Direction facing = state.get(Properties.HORIZONTAL_FACING);
            BlockPos headPos = placementPos.offset(facing).toImmutable();
            footprint.add(new PreviewBlock(headPos, state.with(Properties.BED_PART, BedPart.HEAD), false));
            return List.copyOf(footprint);
        }

        if ((blockItem instanceof TallBlockItem || state.contains(Properties.DOUBLE_BLOCK_HALF))
                && state.contains(Properties.DOUBLE_BLOCK_HALF)) {
            BlockPos upperPos = placementPos.up().toImmutable();
            footprint.add(new PreviewBlock(upperPos,
                    state.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), false));
        }

        return List.copyOf(footprint);
    }

    private static boolean supportsMassPlacement(BlockItem blockItem) {
        return blockItem != null
                && (blockItem.getClass() == BlockItem.class || blockItem instanceof TallBlockItem
                        || blockItem instanceof BedItem);
    }

    private static boolean intersects(List<PreviewBlock> footprint, Set<BlockPos> occupied) {
        for (PreviewBlock previewBlock : footprint) {
            if (occupied.contains(previewBlock.pos())) {
                return true;
            }
        }
        return false;
    }

    private static UltPlaceSelection normalizeSelection(UltPlaceSelection selection) {
        UltPlaceShape shape = selection == null || selection.shape() == null ? UltPlaceShape.PLANE_NXN : selection.shape();
        int size = selection == null ? 1 : Math.max(1, selection.size());
        int length = selection == null ? 1 : Math.max(1, selection.length());
        int height = selection == null ? 1 : Math.max(1, selection.height());
        int variant = selection == null ? 0 : Math.max(0, selection.variant());
        UltPlaceAnchorMode anchorMode = UltPlaceAnchorMode.normalize(shape,
                selection == null ? UltPlaceAnchorMode.CENTER : selection.anchorMode());
        UltPlaceRotationMode rotationMode = UltPlaceRotationMode.normalize(shape,
                selection == null ? UltPlaceRotationMode.AUTO : selection.rotationMode());
        int spacing = shape.supportsSpacing()
                ? Math.max(1, selection == null ? 1 : selection.spacing())
                : 1;
        return new UltPlaceSelection(shape, size, length, height, variant, anchorMode, rotationMode, spacing);
    }

    public record PreviewBlock(BlockPos pos, BlockState state, boolean anchor) {
    }

    public record PlannedPlacement(BlockPos anchorPos, BlockState anchorState, List<PreviewBlock> footprint) {
    }

    public record UltPlacePlan(BlockPos origin, List<PlannedPlacement> placements, List<PreviewBlock> previewBlocks,
            String fallbackReason) {

        public static UltPlacePlan empty(BlockPos origin) {
            return new UltPlacePlan(origin, List.of(), List.of(), null);
        }
    }
}
