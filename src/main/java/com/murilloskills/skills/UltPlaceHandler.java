package com.murilloskills.skills;

import com.murilloskills.data.ModAttachments;
import com.murilloskills.impl.BuilderSkill;
import com.murilloskills.utils.InventoryBlockFinder;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side Builder UltPlace execution and undo buffer.
 */
public final class UltPlaceHandler {
    private static final Map<UUID, UltPlaceShape> SHAPES = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SIZES = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LENGTHS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> VARIANTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ENABLED = new ConcurrentHashMap<>();
    private static final Map<UUID, ArrayDeque<UndoSnapshot>> UNDO_HISTORY = new ConcurrentHashMap<>();
    private static final java.util.Set<UUID> ACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();

    private UltPlaceHandler() {
    }

    public static void setSelection(ServerPlayerEntity player, UltPlaceShape shape, int size, int length, int variant,
            boolean enabled) {
        UltPlaceSelection normalized = normalizeSelection(shape, size, length, variant);
        UUID uuid = player.getUuid();
        SHAPES.put(uuid, normalized.shape());
        SIZES.put(uuid, normalized.size());
        LENGTHS.put(uuid, normalized.length());
        VARIANTS.put(uuid, normalized.variant());
        ENABLED.put(uuid, enabled);
    }

    public static boolean isEnabled(ServerPlayerEntity player) {
        return ENABLED.getOrDefault(player.getUuid(), false);
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

    public static int getVariant(ServerPlayerEntity player) {
        UltPlaceShape shape = getShape(player);
        int maxVariant = UltPlaceShape.getVariantCount(shape) - 1;
        return Math.max(0, Math.min(VARIANTS.getOrDefault(player.getUuid(), 0), maxVariant));
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
            Direction face) {
        if (!(world instanceof ServerWorld) || player == null || targetPos == null || face == null) {
            return List.of();
        }
        if (!canUseUltPlace(player) || !isEnabled(player)) {
            return List.of();
        }

        ItemStack held = getHeldBlockStack(player);
        if (!(held.getItem() instanceof BlockItem)) {
            return List.of();
        }

        BlockPos origin = resolvePlacementOrigin(world, targetPos, face);
        if (!canPlaceAt(world, origin)) {
            return List.of();
        }

        int availableBlocks = InventoryBlockFinder.countMatchingBlocks(player, held);
        if (availableBlocks <= 0) {
            return List.of();
        }

        UltPlaceSelection selection = normalizeSelection(getShape(player), getSize(player), getLength(player),
                getVariant(player));
        List<BlockPos> raw = UltPlaceShapeCalculator.getShapeBlocks(origin, selection.shape(), selection.size(),
                selection.length(), face, player.getRotationVec(1.0f), selection.variant());

        LinkedHashSet<BlockPos> valid = new LinkedHashSet<>();
        int maxAllowed = Math.min(SkillConfig.getUltPlaceMaxBlocksPerUse(), availableBlocks);
        for (BlockPos pos : raw) {
            if (valid.size() >= maxAllowed) {
                break;
            }
            if (!canPlayerModify(world, player, pos)) {
                continue;
            }
            if (!canPlaceAt(world, pos)) {
                continue;
            }
            valid.add(pos.toImmutable());
        }

        return new ArrayList<>(valid);
    }

    public static void handle(ServerPlayerEntity player, ServerWorld world, BlockPos origin, Direction face,
            Hand hand, ItemStack sourceStack, BlockState previousState) {
        if (player == null || world == null || origin == null || face == null || sourceStack == null) {
            return;
        }
        if (!canUseUltPlace(player) || !isEnabled(player)) {
            return;
        }
        if (BuilderSkill.isCreativeBrushActive(player)) {
            return;
        }
        if (!(sourceStack.getItem() instanceof BlockItem)) {
            return;
        }
        if (ACTIVE_PLAYERS.contains(player.getUuid())) {
            return;
        }

        ACTIVE_PLAYERS.add(player.getUuid());
        try {
            UltPlaceSelection selection = normalizeSelection(getShape(player), getSize(player), getLength(player),
                    getVariant(player));
            if (selection.shape() == UltPlaceShape.SINGLE) {
                return;
            }

            BlockState placedState = world.getBlockState(origin);
            if (placedState.isAir()) {
                return;
            }

            int maxBlocks = SkillConfig.getUltPlaceMaxBlocksPerUse();
            List<BlockPos> raw = UltPlaceShapeCalculator.getShapeBlocks(origin, selection.shape(), selection.size(),
                    selection.length(), face, player.getRotationVec(1.0f), selection.variant());
            LinkedHashSet<BlockPos> unique = new LinkedHashSet<>(raw);

            List<UndoEntry> snapshotEntries = new ArrayList<>();
            int placedExtras = 0;
            int placementsUsed = 0;

            if (previousState != null) {
                snapshotEntries.add(new UndoEntry(origin.toImmutable(), previousState));
                placementsUsed = 1;
            }

            for (BlockPos pos : unique) {
                if (pos.equals(origin)) {
                    continue;
                }
                if (placementsUsed >= maxBlocks) {
                    break;
                }
                if (!canPlayerModify(world, player, pos)) {
                    continue;
                }
                BlockState currentState = world.getBlockState(pos);
                if (!canReplace(currentState)) {
                    continue;
                }
                if (!InventoryBlockFinder.consumeOneMatchingBlock(player, sourceStack, hand)) {
                    break;
                }

                snapshotEntries.add(new UndoEntry(pos.toImmutable(), currentState));
                world.setBlockState(pos, placedState);
                placedExtras++;
                placementsUsed++;
            }

            if (placedExtras > 0) {
                pushUndoSnapshot(player, new UndoSnapshot(copySingle(sourceStack), placedState, snapshotEntries));
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

        int refunded = 0;
        List<UndoEntry> entries = snapshot.entries();
        for (int i = entries.size() - 1; i >= 0; i--) {
            UndoEntry entry = entries.get(i);
            if (!canPlayerModify(world, player, entry.pos())) {
                continue;
            }
            world.setBlockState(entry.pos(), entry.previousState());
            refunded++;
        }

        InventoryBlockFinder.refundMatchingBlocks(player, snapshot.itemTemplate(), refunded);
        player.sendMessage(Text.translatable("murilloskills.ultplace.undo.success", refunded)
                .formatted(Formatting.AQUA), true);
        return true;
    }

    public static void cleanupPlayerState(UUID playerUuid) {
        SHAPES.remove(playerUuid);
        SIZES.remove(playerUuid);
        LENGTHS.remove(playerUuid);
        VARIANTS.remove(playerUuid);
        ENABLED.remove(playerUuid);
        UNDO_HISTORY.remove(playerUuid);
        ACTIVE_PLAYERS.remove(playerUuid);
    }

    public static BlockPos resolvePlacementOrigin(World world, BlockPos targetPos, Direction face) {
        if (world == null || targetPos == null || face == null) {
            return targetPos;
        }
        BlockState targetState = world.getBlockState(targetPos);
        if (canReplace(targetState)) {
            return targetPos.toImmutable();
        }
        return targetPos.offset(face).toImmutable();
    }

    public static boolean canPlaceAt(World world, BlockPos pos) {
        return world != null && pos != null && canReplace(world.getBlockState(pos));
    }

    private static ItemStack getHeldBlockStack(PlayerEntity player) {
        ItemStack main = player.getMainHandStack();
        if (main.getItem() instanceof BlockItem) {
            return copySingle(main);
        }
        ItemStack off = player.getOffHandStack();
        if (off.getItem() instanceof BlockItem) {
            return copySingle(off);
        }
        return ItemStack.EMPTY;
    }

    private static boolean canReplace(BlockState state) {
        return state != null && (state.isAir() || state.isReplaceable() || !state.getFluidState().isEmpty());
    }

    private static boolean canPlayerModify(World world, ServerPlayerEntity player, BlockPos pos) {
        if (world instanceof ServerWorld serverWorld) {
            return player.canModifyAt(serverWorld, pos);
        }
        return true;
    }

    private static void pushUndoSnapshot(ServerPlayerEntity player, UndoSnapshot snapshot) {
        ArrayDeque<UndoSnapshot> history = UNDO_HISTORY.computeIfAbsent(player.getUuid(), ignored -> new ArrayDeque<>());
        history.addLast(snapshot);
        int maxHistory = SkillConfig.getUltPlaceUndoHistorySize();
        while (history.size() > maxHistory) {
            history.pollFirst();
        }
    }

    private static UltPlaceSelection normalizeSelection(UltPlaceShape shape, int size, int length, int variant) {
        UltPlaceShape safeShape = shape == null ? UltPlaceShape.PLANE_NXN : shape;
        int safeSize = size > 0 ? clampSize(safeShape, size) : SkillConfig.getUltPlaceShapeDefaultSize(safeShape);
        int safeLength = length > 0
                ? clampLength(safeShape, length)
                : SkillConfig.getUltPlaceShapeDefaultLength(safeShape);
        int maxVariant = UltPlaceShape.getVariantCount(safeShape) - 1;
        int safeVariant = Math.max(0, Math.min(variant, maxVariant));
        return new UltPlaceSelection(safeShape, safeSize, safeLength, safeVariant);
    }

    private static int clampSize(UltPlaceShape shape, int size) {
        return Math.max(1, Math.min(size, SkillConfig.getUltPlaceShapeMaxSize(shape)));
    }

    private static int clampLength(UltPlaceShape shape, int length) {
        return Math.max(1, Math.min(length, SkillConfig.getUltPlaceShapeMaxLength(shape)));
    }

    private static ItemStack copySingle(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private record UltPlaceSelection(UltPlaceShape shape, int size, int length, int variant) {
    }

    private record UndoEntry(BlockPos pos, BlockState previousState) {
    }

    private record UndoSnapshot(ItemStack itemTemplate, BlockState placedState, List<UndoEntry> entries) {
    }
}
