package com.murilloskills.skills;

import com.murilloskills.data.ModAttachments;
import com.murilloskills.impl.BuilderSkill;
import com.murilloskills.utils.InventoryBlockFinder;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side Builder UltPlace execution and undo buffer.
 */
public final class UltPlaceHandler {
    private static final Map<UUID, UltPlaceShape> SHAPES = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SIZES = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LENGTHS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> HEIGHTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> VARIANTS = new ConcurrentHashMap<>();
    private static final Map<UUID, UltPlaceAnchorMode> ANCHOR_MODES = new ConcurrentHashMap<>();
    private static final Map<UUID, UltPlaceRotationMode> ROTATION_MODES = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SPACINGS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ENABLED = new ConcurrentHashMap<>();
    private static final Map<UUID, ArrayDeque<UndoSnapshot>> UNDO_HISTORY = new ConcurrentHashMap<>();
    private static final Set<UUID> ACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> SYNTHETIC_PLACEMENTS = ConcurrentHashMap.newKeySet();

    private UltPlaceHandler() {
    }

    public static void setSelection(ServerPlayerEntity player, UltPlaceShape shape, int size, int length, int height,
            int variant,
            UltPlaceAnchorMode anchorMode, UltPlaceRotationMode rotationMode, int spacing, boolean enabled) {
        UltPlaceSelection normalized = normalizeSelection(shape, size, length, height, variant, anchorMode, rotationMode,
                spacing);
        UUID uuid = player.getUuid();
        SHAPES.put(uuid, normalized.shape());
        SIZES.put(uuid, normalized.size());
        LENGTHS.put(uuid, normalized.length());
        HEIGHTS.put(uuid, normalized.height());
        VARIANTS.put(uuid, normalized.variant());
        ANCHOR_MODES.put(uuid, normalized.anchorMode());
        ROTATION_MODES.put(uuid, normalized.rotationMode());
        SPACINGS.put(uuid, normalized.spacing());
        ENABLED.put(uuid, enabled);
    }

    public static boolean isEnabled(ServerPlayerEntity player) {
        return ENABLED.getOrDefault(player.getUuid(), false);
    }

    public static boolean isSyntheticPlacementActive(ServerPlayerEntity player) {
        return player != null && SYNTHETIC_PLACEMENTS.contains(player.getUuid());
    }

    public static UltPlaceShape getShape(ServerPlayerEntity player) {
        return SHAPES.getOrDefault(player.getUuid(), UltPlaceShape.PLANE_NXN);
    }

    public static int getSize(ServerPlayerEntity player) {
        UltPlaceShape shape = getShape(player);
        int configured = SIZES.getOrDefault(player.getUuid(), SkillConfig.getUltPlaceShapeDefaultSize(shape));
        return clampSize(shape, configured);
    }

    public static int getLength(ServerPlayerEntity player) {
        UltPlaceShape shape = getShape(player);
        int configured = LENGTHS.getOrDefault(player.getUuid(), SkillConfig.getUltPlaceShapeDefaultLength(shape));
        return clampLength(shape, configured);
    }

    public static int getHeight(ServerPlayerEntity player) {
        UltPlaceShape shape = getShape(player);
        int configured = HEIGHTS.getOrDefault(player.getUuid(), SkillConfig.getUltPlaceShapeDefaultHeight(shape));
        return clampHeight(shape, configured);
    }

    public static int getVariant(ServerPlayerEntity player) {
        UltPlaceShape shape = getShape(player);
        int maxVariant = UltPlaceShape.getVariantCount(shape) - 1;
        return Math.max(0, Math.min(VARIANTS.getOrDefault(player.getUuid(), 0), maxVariant));
    }

    public static UltPlaceAnchorMode getAnchorMode(ServerPlayerEntity player) {
        return UltPlaceAnchorMode.normalize(getShape(player), ANCHOR_MODES.get(player.getUuid()));
    }

    public static UltPlaceRotationMode getRotationMode(ServerPlayerEntity player) {
        return UltPlaceRotationMode.normalize(getShape(player), ROTATION_MODES.get(player.getUuid()));
    }

    public static int getSpacing(ServerPlayerEntity player) {
        UltPlaceShape shape = getShape(player);
        return clampSpacing(shape, SPACINGS.getOrDefault(player.getUuid(), 1));
    }

    public static UltPlaceSelection getSelection(ServerPlayerEntity player) {
        return normalizeSelection(getShape(player), getSize(player), getLength(player), getHeight(player), getVariant(player),
                getAnchorMode(player), getRotationMode(player), getSpacing(player));
    }

    public static boolean canUseUltPlace(ServerPlayerEntity player) {
        if (player == null || !SkillConfig.isBuilderUltPlaceEnabled()) {
            return false;
        }

        try {
            var data = player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
            return data.isSkillSelected(MurilloSkillsList.BUILDER);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static List<BlockPos> getValidatedPreview(ServerPlayerEntity player, World world, BlockPos targetPos,
            Direction face, Vec3d hitPos) {
        if (!(world instanceof ServerWorld serverWorld) || player == null || targetPos == null || face == null) {
            return List.of();
        }
        if (!canUseUltPlace(player) || !isEnabled(player)) {
            return List.of();
        }

        HeldBlockSelection held = getHeldBlockSelection(player);
        if (held == null) {
            return List.of();
        }

        int availablePlacements = InventoryBlockFinder.countMatchingBlocks(player, held.stack());
        if (availablePlacements <= 0) {
            return List.of();
        }

        UltPlacePlanner.UltPlacePlan plan = UltPlacePlanner.planPreview(serverWorld, player, held.hand(), held.stack(),
                targetPos, face, hitPos, getSelection(player),
                Math.min(SkillConfig.getUltPlaceMaxBlocksPerUse(), availablePlacements));
        if (plan.previewBlocks().isEmpty()) {
            return List.of();
        }

        LinkedHashSet<BlockPos> valid = new LinkedHashSet<>();
        for (UltPlacePlanner.PlannedPlacement placement : plan.placements()) {
            if (!canPlayerModifyAll(serverWorld, player, placement.footprint())) {
                continue;
            }
            for (UltPlacePlanner.PreviewBlock previewBlock : placement.footprint()) {
                valid.add(previewBlock.pos().toImmutable());
            }
        }

        return new ArrayList<>(valid);
    }

    public static void handle(ServerPlayerEntity player, ServerWorld world, BlockPos origin, Direction face,
            Hand hand, ItemStack sourceStack, Vec3d hitPos, Map<BlockPos, BlockState> previousStates) {
        if (player == null || world == null || origin == null || face == null || sourceStack == null) {
            return;
        }
        if (!canUseUltPlace(player) || !isEnabled(player) || BuilderSkill.isCreativeBrushActive(player)) {
            return;
        }
        if (!(sourceStack.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        if (!ACTIVE_PLAYERS.add(player.getUuid())) {
            return;
        }

        try {
            UltPlaceSelection selection = getSelection(player);
            if (selection.shape() == UltPlaceShape.SINGLE) {
                return;
            }

            int maxPerUse = SkillConfig.getUltPlaceMaxBlocksPerUse();
            int remainingPlacements = Math.min(Math.max(0, maxPerUse - 1),
                    InventoryBlockFinder.countMatchingBlocks(player, sourceStack));
            if (remainingPlacements <= 0) {
                return;
            }

            UltPlacePlanner.UltPlacePlan plan = UltPlacePlanner.planFromOrigin(world, player, hand,
                    copySingle(sourceStack), origin, face, hitPos, selection, remainingPlacements);
            if (plan.placements().isEmpty()) {
                if (plan.fallbackReason() != null) {
                    player.sendMessage(Text.translatable(plan.fallbackReason()).formatted(Formatting.YELLOW), true);
                }
                return;
            }

            List<UndoEntry> snapshotEntries = new ArrayList<>();
            Set<BlockPos> capturedPositions = new LinkedHashSet<>();
            addCapturedStates(snapshotEntries, capturedPositions, previousStates);

            int itemsPlaced = previousStates == null || previousStates.isEmpty() ? 0 : 1;
            SYNTHETIC_PLACEMENTS.add(player.getUuid());
            try {
                for (UltPlacePlanner.PlannedPlacement placement : plan.placements()) {
                    if (!canPlayerModifyAll(world, player, placement.footprint())) {
                        continue;
                    }

                    ItemStack liveStack = InventoryBlockFinder.findMatchingBlock(player, sourceStack, hand);
                    if (liveStack == null || liveStack.isEmpty()) {
                        break;
                    }

                    List<UndoEntry> pendingSnapshots = captureCurrentStates(world, placement.footprint(), capturedPositions);
                    ActionResult result = blockItem.place(createPlacementContext(player, hand, liveStack,
                            placement.anchorPos(), face, hitPos));
                    if (result == null || !result.isAccepted()) {
                        continue;
                    }

                    snapshotEntries.addAll(pendingSnapshots);
                    for (UndoEntry pendingSnapshot : pendingSnapshots) {
                        capturedPositions.add(pendingSnapshot.pos());
                    }
                    itemsPlaced++;
                }
            } finally {
                SYNTHETIC_PLACEMENTS.remove(player.getUuid());
            }

            if (itemsPlaced > 1 && !snapshotEntries.isEmpty()) {
                pushUndoSnapshot(player, new UndoSnapshot(copySingle(sourceStack), itemsPlaced, snapshotEntries));
            }
        } finally {
            ACTIVE_PLAYERS.remove(player.getUuid());
        }
    }

    public static boolean undoLast(ServerPlayerEntity player) {
        if (player == null || !(player.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }

        ArrayDeque<UndoSnapshot> history = UNDO_HISTORY.get(player.getUuid());
        if (history == null || history.isEmpty()) {
            player.sendMessage(Text.translatable("murilloskills.ultplace.undo.empty").formatted(Formatting.YELLOW), true);
            return false;
        }

        UndoSnapshot snapshot = history.pollLast();
        if (snapshot == null || snapshot.entries().isEmpty()) {
            return false;
        }

        int restoredEntries = 0;
        List<UndoEntry> entries = snapshot.entries();
        for (int i = entries.size() - 1; i >= 0; i--) {
            UndoEntry entry = entries.get(i);
            if (!canPlayerModify(world, player, entry.pos())) {
                continue;
            }
            world.setBlockState(entry.pos(), entry.previousState());
            restoredEntries++;
        }

        int refundedItems = restoredEntries == entries.size() ? snapshot.itemsPlaced() : 0;
        if (refundedItems > 0) {
            InventoryBlockFinder.refundMatchingBlocks(player, snapshot.itemTemplate(), refundedItems);
        }
        player.sendMessage(Text.translatable("murilloskills.ultplace.undo.success", refundedItems)
                .formatted(Formatting.AQUA), true);
        return refundedItems > 0;
    }

    public static void cleanupPlayerState(UUID playerUuid) {
        SHAPES.remove(playerUuid);
        SIZES.remove(playerUuid);
        LENGTHS.remove(playerUuid);
        HEIGHTS.remove(playerUuid);
        VARIANTS.remove(playerUuid);
        ANCHOR_MODES.remove(playerUuid);
        ROTATION_MODES.remove(playerUuid);
        SPACINGS.remove(playerUuid);
        ENABLED.remove(playerUuid);
        UNDO_HISTORY.remove(playerUuid);
        ACTIVE_PLAYERS.remove(playerUuid);
        SYNTHETIC_PLACEMENTS.remove(playerUuid);
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

    private static HeldBlockSelection getHeldBlockSelection(PlayerEntity player) {
        if (player == null) {
            return null;
        }

        ItemStack main = player.getMainHandStack();
        if (main.getItem() instanceof BlockItem) {
            return new HeldBlockSelection(Hand.MAIN_HAND, copySingle(main));
        }
        ItemStack off = player.getOffHandStack();
        if (off.getItem() instanceof BlockItem) {
            return new HeldBlockSelection(Hand.OFF_HAND, copySingle(off));
        }
        return null;
    }

    private static boolean canPlayerModifyAll(ServerWorld world, ServerPlayerEntity player,
            List<UltPlacePlanner.PreviewBlock> footprint) {
        for (UltPlacePlanner.PreviewBlock previewBlock : footprint) {
            if (!canPlayerModify(world, player, previewBlock.pos())) {
                return false;
            }
        }
        return true;
    }

    private static boolean canPlayerModify(World world, ServerPlayerEntity player, BlockPos pos) {
        return player.canModifyAt((ServerWorld) world, pos);
    }

    private static void addCapturedStates(List<UndoEntry> target, Set<BlockPos> seen,
            Map<BlockPos, BlockState> capturedStates) {
        if (capturedStates == null || capturedStates.isEmpty()) {
            return;
        }
        for (Map.Entry<BlockPos, BlockState> entry : capturedStates.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || seen.contains(entry.getKey())) {
                continue;
            }
            target.add(new UndoEntry(entry.getKey().toImmutable(), entry.getValue()));
            seen.add(entry.getKey().toImmutable());
        }
    }

    private static List<UndoEntry> captureCurrentStates(ServerWorld world, List<UltPlacePlanner.PreviewBlock> footprint,
            Set<BlockPos> alreadyCaptured) {
        List<UndoEntry> pending = new ArrayList<>();
        for (UltPlacePlanner.PreviewBlock previewBlock : footprint) {
            BlockPos pos = previewBlock.pos();
            if (alreadyCaptured.contains(pos)) {
                continue;
            }
            pending.add(new UndoEntry(pos.toImmutable(), world.getBlockState(pos)));
        }
        return pending;
    }

    private static void pushUndoSnapshot(ServerPlayerEntity player, UndoSnapshot snapshot) {
        ArrayDeque<UndoSnapshot> history = UNDO_HISTORY.computeIfAbsent(player.getUuid(), ignored -> new ArrayDeque<>());
        history.addLast(snapshot);
        int maxHistory = SkillConfig.getUltPlaceUndoHistorySize();
        while (history.size() > maxHistory) {
            history.pollFirst();
        }
    }

    private static UltPlaceSelection normalizeSelection(UltPlaceShape shape, int size, int length, int height,
            int variant, UltPlaceAnchorMode anchorMode, UltPlaceRotationMode rotationMode, int spacing) {
        UltPlaceShape safeShape = shape == null ? UltPlaceShape.PLANE_NXN : shape;
        int safeSize = size > 0 ? clampSize(safeShape, size) : SkillConfig.getUltPlaceShapeDefaultSize(safeShape);
        int safeLength = length > 0
                ? clampLength(safeShape, length)
                : SkillConfig.getUltPlaceShapeDefaultLength(safeShape);
        int safeHeight = height > 0
                ? clampHeight(safeShape, height)
                : SkillConfig.getUltPlaceShapeDefaultHeight(safeShape);
        int maxVariant = UltPlaceShape.getVariantCount(safeShape) - 1;
        int safeVariant = Math.max(0, Math.min(variant, maxVariant));
        UltPlaceAnchorMode safeAnchorMode = UltPlaceAnchorMode.normalize(safeShape, anchorMode);
        UltPlaceRotationMode safeRotationMode = UltPlaceRotationMode.normalize(safeShape, rotationMode);
        int safeSpacing = clampSpacing(safeShape, spacing);
        return new UltPlaceSelection(safeShape, safeSize, safeLength, safeHeight, safeVariant, safeAnchorMode,
                safeRotationMode, safeSpacing);
    }

    private static int clampSize(UltPlaceShape shape, int size) {
        return Math.max(1, Math.min(size, SkillConfig.getUltPlaceShapeMaxSize(shape)));
    }

    private static int clampLength(UltPlaceShape shape, int length) {
        return Math.max(1, Math.min(length, SkillConfig.getUltPlaceShapeMaxLength(shape)));
    }

    private static int clampHeight(UltPlaceShape shape, int height) {
        return Math.max(1, Math.min(height, SkillConfig.getUltPlaceShapeMaxHeight(shape)));
    }

    private static int clampSpacing(UltPlaceShape shape, int spacing) {
        if (shape == null || !shape.supportsSpacing()) {
            return 1;
        }
        return Math.max(1, Math.min(spacing, SkillConfig.getUltPlaceShapeMaxSpacing(shape)));
    }

    private static ItemStack copySingle(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private record HeldBlockSelection(Hand hand, ItemStack stack) {
    }

    private record UndoEntry(BlockPos pos, BlockState previousState) {
    }

    private record UndoSnapshot(ItemStack itemTemplate, int itemsPlaced, List<UndoEntry> entries) {
    }
}
