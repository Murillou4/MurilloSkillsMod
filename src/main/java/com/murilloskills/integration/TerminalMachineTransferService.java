package com.murilloskills.integration;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class TerminalMachineTransferService {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-TerminalTransfer");
    private static final int MAX_AMOUNT = 4096;
    private static final double MAX_DISTANCE_SQUARED = 144.0D;
    private static final String TECH_REBORN_STORAGE_UNIT_BE =
            "techreborn.blockentity.storage.item.StorageUnitBaseBlockEntity";
    private static volatile Class<?> techRebornStorageUnitClass;
    private static volatile Method techRebornProcessInputMethod;

    private TerminalMachineTransferService() {
    }

    public static void transfer(ServerPlayerEntity player, ItemStack itemKey, int requestedAmount, BlockPos targetPos,
            Direction face) {
        if (player == null || itemKey == null || itemKey.isEmpty()) {
            return;
        }
        int amount = Math.max(1, Math.min(requestedAmount, MAX_AMOUNT));
        LOGGER.info("Terminal transfer requested: player={}, item={}, amount={}, target={}, face={}",
                player.getName().getString(), itemKey.getName().getString(), amount, targetPos, face);
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        if (targetPos == null || !world.isChunkLoaded(targetPos) || isTooFar(player, targetPos)) {
            player.sendMessage(Text.translatable("murilloskills.terminal_transfer.target_too_far"), false);
            LOGGER.info("Terminal transfer rejected: target is missing, unloaded, or too far");
            return;
        }
        BlockEntity target = world.getBlockEntity(targetPos);
        if (target == null) {
            player.sendMessage(Text.translatable("murilloskills.terminal_transfer.no_target"), false);
            LOGGER.info("Terminal transfer rejected: target {} has no block entity ({})", targetPos,
                    Registries.BLOCK.getId(world.getBlockState(targetPos).getBlock()));
            return;
        }

        ItemStack pulled = TomsStorageBridge.pullFromBoundStorage(player, itemKey, amount);
        if (pulled.isEmpty()) {
            player.sendMessage(Text.translatable("murilloskills.terminal_transfer.no_storage", itemKey.getName()),
                    false);
            LOGGER.info("Terminal transfer could not pull {} x {}", amount, itemKey.getName().getString());
            return;
        }
        LOGGER.info("Terminal transfer pulled {} x {}", pulled.getCount(), pulled.getName().getString());

        ItemStack toInsert = pulled.copy();
        int inserted = insertIntoTarget(world, targetPos, target, face, toInsert);
        if (inserted <= 0) {
            returnLeftover(player, pulled);
            player.sendMessage(Text.translatable("murilloskills.terminal_transfer.no_input", pulled.getName()), false);
            LOGGER.info("Terminal transfer inserted nothing into {}", target.getCachedState().getBlock().getName().getString());
            return;
        }

        if (!toInsert.isEmpty()) {
            returnLeftover(player, toInsert);
        }

        Text targetName = target.getCachedState().getBlock().getName();
        if (inserted < pulled.getCount()) {
            player.sendMessage(Text.translatable("murilloskills.terminal_transfer.partial", inserted, pulled.getName(),
                    targetName), false);
        } else {
            player.sendMessage(Text.translatable("murilloskills.terminal_transfer.success", inserted, pulled.getName(),
                    targetName), false);
        }
        LOGGER.info("Terminal transfer inserted {} into {}; leftover={}", inserted, targetName.getString(),
                toInsert.getCount());
    }

    private static boolean isTooFar(ServerPlayerEntity player, BlockPos pos) {
        double dx = player.getX() - (pos.getX() + 0.5D);
        double dy = player.getY() - (pos.getY() + 0.5D);
        double dz = player.getZ() - (pos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz > MAX_DISTANCE_SQUARED;
    }

    private static int insertIntoTarget(ServerWorld world, BlockPos pos, BlockEntity target, Direction face,
            ItemStack stack) {
        int storageUnitInserted = insertIntoTechRebornStorageUnit(target, stack);
        if (stack.isEmpty()) {
            return storageUnitInserted;
        }
        int inserted = insertWithTransferApi(world, pos, face, stack);
        if (stack.isEmpty()) {
            return storageUnitInserted + inserted;
        }
        int fallbackInserted = insertWithVanillaInventory(target, face, stack);
        return storageUnitInserted + inserted + fallbackInserted;
    }

    private static int insertIntoTechRebornStorageUnit(BlockEntity target, ItemStack stack) {
        Class<?> storageUnitClass = resolveTechRebornStorageUnitClass();
        if (storageUnitClass == null || !storageUnitClass.isInstance(target) || stack.isEmpty()) {
            return 0;
        }
        try {
            Method processInput = resolveTechRebornProcessInputMethod(storageUnitClass);
            if (processInput == null) {
                return 0;
            }
            int before = stack.getCount();
            ItemStack leftover = (ItemStack) processInput.invoke(target, stack.copy());
            if (leftover == null) {
                leftover = ItemStack.EMPTY;
            }
            stack.setCount(leftover.getCount());
            target.markDirty();
            return before - stack.getCount();
        } catch (ReflectiveOperationException | ClassCastException e) {
            LOGGER.warn("Failed to insert through Tech Reborn Storage Unit reflection", e);
            return 0;
        }
    }

    private static Class<?> resolveTechRebornStorageUnitClass() {
        Class<?> cached = techRebornStorageUnitClass;
        if (cached != null) {
            return cached;
        }
        try {
            cached = Class.forName(TECH_REBORN_STORAGE_UNIT_BE);
            techRebornStorageUnitClass = cached;
            return cached;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Method resolveTechRebornProcessInputMethod(Class<?> storageUnitClass) {
        Method cached = techRebornProcessInputMethod;
        if (cached != null) {
            return cached;
        }
        try {
            cached = storageUnitClass.getMethod("processInput", ItemStack.class);
            cached.setAccessible(true);
            techRebornProcessInputMethod = cached;
            return cached;
        } catch (NoSuchMethodException e) {
            return null;
        }
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
