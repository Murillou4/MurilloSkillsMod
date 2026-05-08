package com.murilloskills.integration;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public final class TerminalMachineTransferService {
    private static final int MAX_AMOUNT = 4096;
    private static final double MAX_DISTANCE_SQUARED = 144.0D;

    private TerminalMachineTransferService() {
    }

    public static void transfer(ServerPlayerEntity player, ItemStack itemKey, int requestedAmount, BlockPos targetPos,
            Direction face) {
        if (player == null || itemKey == null || itemKey.isEmpty()) {
            return;
        }
        int amount = Math.max(1, Math.min(requestedAmount, MAX_AMOUNT));
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        if (targetPos == null || !world.isChunkLoaded(targetPos) || isTooFar(player, targetPos)) {
            player.sendMessage(Text.translatable("murilloskills.terminal_transfer.target_too_far"), true);
            return;
        }
        BlockEntity target = world.getBlockEntity(targetPos);
        if (target == null) {
            player.sendMessage(Text.translatable("murilloskills.terminal_transfer.no_target"), true);
            return;
        }

        ItemStack pulled = TomsStorageBridge.pullFromBoundStorage(player, itemKey, amount);
        if (pulled.isEmpty()) {
            player.sendMessage(Text.translatable("murilloskills.terminal_transfer.no_storage", itemKey.getName()),
                    true);
            return;
        }

        ItemStack toInsert = pulled.copy();
        int inserted = insertIntoTarget(world, targetPos, target, face, toInsert);
        if (inserted <= 0) {
            returnLeftover(player, pulled);
            player.sendMessage(Text.translatable("murilloskills.terminal_transfer.no_input", pulled.getName()), true);
            return;
        }

        if (!toInsert.isEmpty()) {
            returnLeftover(player, toInsert);
        }

        Text targetName = target.getCachedState().getBlock().getName();
        if (inserted < pulled.getCount()) {
            player.sendMessage(Text.translatable("murilloskills.terminal_transfer.partial", inserted, pulled.getName(),
                    targetName), true);
        } else {
            player.sendMessage(Text.translatable("murilloskills.terminal_transfer.success", inserted, pulled.getName(),
                    targetName), true);
        }
    }

    private static boolean isTooFar(ServerPlayerEntity player, BlockPos pos) {
        double dx = player.getX() - (pos.getX() + 0.5D);
        double dy = player.getY() - (pos.getY() + 0.5D);
        double dz = player.getZ() - (pos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz > MAX_DISTANCE_SQUARED;
    }

    private static int insertIntoTarget(ServerWorld world, BlockPos pos, BlockEntity target, Direction face,
            ItemStack stack) {
        int inserted = insertWithTransferApi(world, pos, face, stack);
        if (stack.isEmpty()) {
            return inserted;
        }
        int fallbackInserted = insertWithVanillaInventory(target, face, stack);
        return inserted + fallbackInserted;
    }

    private static int insertWithTransferApi(ServerWorld world, BlockPos pos, Direction face, ItemStack stack) {
        int inserted = 0;
        for (Direction side : insertionSides(face)) {
            if (stack.isEmpty()) {
                break;
            }
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, pos, side);
            if (storage == null) {
                continue;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                long moved = storage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
                if (moved > 0) {
                    transaction.commit();
                    int movedInt = (int) Math.min(moved, stack.getCount());
                    stack.decrement(movedInt);
                    inserted += movedInt;
                }
            }
        }
        return inserted;
    }

    private static int insertWithVanillaInventory(BlockEntity target, Direction face, ItemStack stack) {
        if (!(target instanceof Inventory inventory)) {
            return 0;
        }
        int before = stack.getCount();
        if (target instanceof SidedInventory sided) {
            for (Direction side : insertionSides(face)) {
                if (stack.isEmpty()) {
                    break;
                }
                insertIntoSidedInventory(sided, side, stack);
            }
        } else {
            insertIntoSlots(inventory, allSlots(inventory), null, stack);
        }
        if (stack.getCount() != before) {
            inventory.markDirty();
        }
        return before - stack.getCount();
    }

    private static void insertIntoSidedInventory(SidedInventory inventory, Direction side, ItemStack stack) {
        if (side == null) {
            insertIntoSlots(inventory, allSlots(inventory), null, stack);
            return;
        }
        insertIntoSlots(inventory, inventory.getAvailableSlots(side), side, stack);
    }

    private static void insertIntoSlots(Inventory inventory, int[] slots, Direction side, ItemStack stack) {
        for (int slot : slots) {
            if (stack.isEmpty()) {
                return;
            }
            if (side != null && inventory instanceof SidedInventory sided && !sided.canInsert(slot, stack, side)) {
                continue;
            }
            if (!inventory.isValid(slot, stack)) {
                continue;
            }
            ItemStack current = inventory.getStack(slot);
            if (current.isEmpty()) {
                int move = Math.min(stack.getCount(), stack.getMaxCount());
                ItemStack placed = stack.copy();
                placed.setCount(move);
                inventory.setStack(slot, placed);
                stack.decrement(move);
                continue;
            }
            if (!ItemStack.areItemsAndComponentsEqual(current, stack)) {
                continue;
            }
            int capacity = Math.min(current.getMaxCount(), stack.getMaxCount()) - current.getCount();
            if (capacity <= 0) {
                continue;
            }
            int move = Math.min(stack.getCount(), capacity);
            current.increment(move);
            stack.decrement(move);
        }
    }

    private static int[] allSlots(Inventory inventory) {
        int[] slots = new int[inventory.size()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }

    private static List<Direction> insertionSides(Direction preferred) {
        List<Direction> sides = new ArrayList<>(Direction.values().length + 1);
        if (preferred != null) {
            sides.add(preferred);
        }
        sides.add(null);
        for (Direction direction : Direction.values()) {
            if (direction != preferred) {
                sides.add(direction);
            }
        }
        return sides;
    }

    private static void returnLeftover(ServerPlayerEntity player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ItemStack leftover = TomsStorageBridge.pushToBoundStorage(player, stack);
        if (!leftover.isEmpty()) {
            player.getInventory().insertStack(leftover);
        }
        if (!leftover.isEmpty()) {
            player.dropItem(leftover, false);
        }
    }

}
