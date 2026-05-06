package com.murilloskills.skills;

import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Merges bulk Ultmine drops so thousands of block breaks do not become thousands
 * of item entities or inventory insertion attempts.
 */
public final class UltmineDropCollector {
    private UltmineDropCollector() {
    }

    public static void addMerged(List<ItemStack> destination, List<ItemStack> drops, Set<String> trashItemIds) {
        addMergedStacks(destination, drops, itemStackAdapter(stack -> isTrash(stack, trashItemIds)));
    }

    static void addMerged(List<ItemStack> destination, List<ItemStack> drops, Predicate<ItemStack> trashFilter) {
        addMergedStacks(destination, drops, itemStackAdapter(trashFilter));
    }

    private static void addMergedStacks(List<ItemStack> destination, List<ItemStack> drops,
            StackAdapter<ItemStack> adapter) {
        if (drops == null || drops.isEmpty()) {
            return;
        }
        for (ItemStack drop : drops) {
            addMerged(destination, drop, adapter);
        }
    }

    public static void addMerged(List<ItemStack> destination, ItemStack drop, Set<String> trashItemIds) {
        addMerged(destination, drop, stack -> isTrash(stack, trashItemIds));
    }

    static void addMerged(List<ItemStack> destination, ItemStack drop, Predicate<ItemStack> trashFilter) {
        addMerged(destination, drop, itemStackAdapter(trashFilter));
    }

    public static List<ItemStack> mergeDrops(List<ItemStack> drops, Set<String> trashItemIds) {
        return mergeDrops(drops, stack -> isTrash(stack, trashItemIds));
    }

    static List<ItemStack> mergeDrops(List<ItemStack> drops, Predicate<ItemStack> trashFilter) {
        List<ItemStack> merged = new ArrayList<>();
        addMerged(merged, drops, trashFilter);
        return merged;
    }

    public static boolean isTrash(ItemStack stack, Set<String> trashItemIds) {
        if (stack == null || stack.isEmpty() || trashItemIds == null || trashItemIds.isEmpty()) {
            return false;
        }
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        return trashItemIds.contains(itemId);
    }

    public static void insertOrSpawn(ServerPlayerEntity player, ServerWorld world, BlockPos pos, List<ItemStack> stacks) {
        if (player == null || world == null || pos == null || stacks == null || stacks.isEmpty()) {
            return;
        }
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemStack remaining = stack.copy();
            player.getInventory().insertStack(remaining);
            if (!remaining.isEmpty()) {
                spawnStack(world, pos, remaining);
            }
        }
        stacks.clear();
    }

    public static void spawn(ServerWorld world, BlockPos pos, List<ItemStack> stacks) {
        if (world == null || pos == null || stacks == null || stacks.isEmpty()) {
            return;
        }
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            Block.dropStack(world, pos, stack.copy());
        }
        stacks.clear();
    }

    private static void spawnStack(ServerWorld world, BlockPos pos, ItemStack stack) {
        ItemEntity itemEntity = new ItemEntity(world,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
        world.spawnEntity(itemEntity);
    }

    static <T> void addMerged(List<T> destination, T drop, StackAdapter<T> adapter) {
        if (destination == null || adapter == null || adapter.isEmpty(drop) || adapter.isTrash(drop)) {
            return;
        }

        int remaining = adapter.getCount(drop);
        while (remaining > 0) {
            int moved = mergeIntoExisting(destination, drop, remaining, adapter);
            if (moved <= 0) {
                int count = Math.min(remaining, adapter.getMaxCount(drop));
                destination.add(adapter.copyWithCount(drop, count));
                moved = count;
            }
            remaining -= moved;
        }
    }

    private static <T> int mergeIntoExisting(List<T> destination, T source, int maxToMove, StackAdapter<T> adapter) {
        for (T existing : destination) {
            if (!adapter.canMerge(existing, source)) {
                continue;
            }
            int space = adapter.getMaxCount(existing) - adapter.getCount(existing);
            if (space <= 0) {
                continue;
            }
            int moved = Math.min(space, maxToMove);
            adapter.increment(existing, moved);
            return moved;
        }
        return 0;
    }

    private static StackAdapter<ItemStack> itemStackAdapter(Predicate<ItemStack> trashFilter) {
        return new StackAdapter<>() {
            @Override
            public boolean isEmpty(ItemStack stack) {
                return stack == null || stack.isEmpty();
            }

            @Override
            public boolean isTrash(ItemStack stack) {
                return trashFilter != null && trashFilter.test(stack);
            }

            @Override
            public int getCount(ItemStack stack) {
                return stack.getCount();
            }

            @Override
            public int getMaxCount(ItemStack stack) {
                return stack.getMaxCount();
            }

            @Override
            public boolean canMerge(ItemStack existing, ItemStack source) {
                return ItemStack.areItemsAndComponentsEqual(existing, source);
            }

            @Override
            public void increment(ItemStack stack, int amount) {
                stack.increment(amount);
            }

            @Override
            public ItemStack copyWithCount(ItemStack stack, int count) {
                ItemStack copy = stack.copy();
                copy.setCount(count);
                return copy;
            }
        };
    }

    interface StackAdapter<T> {
        boolean isEmpty(T stack);

        boolean isTrash(T stack);

        int getCount(T stack);

        int getMaxCount(T stack);

        boolean canMerge(T existing, T source);

        void increment(T stack, int amount);

        T copyWithCount(T stack, int count);
    }
}
