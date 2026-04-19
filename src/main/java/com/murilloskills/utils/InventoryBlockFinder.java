package com.murilloskills.utils;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

/**
 * Shared inventory helper for Builder placement features.
 */
public final class InventoryBlockFinder {

    private InventoryBlockFinder() {
    }

    public static int countMatchingBlocks(ServerPlayerEntity player, ItemStack reference) {
        if (player == null || reference == null || reference.isEmpty()) {
            return 0;
        }

        int total = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (matches(stack, reference)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static ItemStack findMatchingBlock(ServerPlayerEntity player, ItemStack reference, Hand preferredHand) {
        if (player == null || reference == null || reference.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack preferredStack = getPreferredStack(player, preferredHand);
        if (matches(preferredStack, reference)) {
            return preferredStack;
        }

        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack == preferredStack) {
                continue;
            }
            if (matches(stack, reference)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    public static boolean consumeOneMatchingBlock(ServerPlayerEntity player, ItemStack reference, Hand preferredHand) {
        ItemStack stack = findMatchingBlock(player, reference, preferredHand);
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        stack.decrement(1);
        return true;
    }

    public static void refundMatchingBlocks(ServerPlayerEntity player, ItemStack template, int amount) {
        if (player == null || template == null || template.isEmpty() || amount <= 0) {
            return;
        }

        ItemStack refund = template.copy();
        refund.setCount(amount);
        boolean inserted = player.getInventory().insertStack(refund);
        if (!inserted && !refund.isEmpty()) {
            ItemEntity dropped = player.dropItem(refund, false);
            if (dropped != null) {
                dropped.resetPickupDelay();
            }
        }
    }

    private static ItemStack getPreferredStack(ServerPlayerEntity player, Hand preferredHand) {
        if (preferredHand == Hand.OFF_HAND) {
            return player.getOffHandStack();
        }
        return player.getMainHandStack();
    }

    private static boolean matches(ItemStack candidate, ItemStack reference) {
        return candidate != null
                && !candidate.isEmpty()
                && ItemStack.areItemsAndComponentsEqual(candidate, reference)
                && candidate.getCount() > 0;
    }
}
