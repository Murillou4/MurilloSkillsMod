package com.murilloskills.utils;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;
import java.util.Random;

/**
 * Generator for Epic Bundle items from fishing.
 * 
 * Epic Bundle chances (weighted random):
 * | 40% | Enchanted Book (Mending, Frost Walker, Depth Strider, Looting III,
 * Fortune III, Efficiency V, Sharpness V, Protection V) |
 * | 30% | 1-3x Gold Block |
 * | 15% | 1-2x Diamond Block |
 * | 10% | Trident with high-level enchantment |
 * | 4% | 1x Netherite Block |
 * | 1% | 1x Enchanted Golden Apple |
 */
public class EpicBundleGenerator {

    private static final Random random = new Random();

    // Weight constants (total = 100)
    private static final int WEIGHT_ENCHANTED_BOOK = 40;
    private static final int WEIGHT_GOLD_BLOCK = 30;
    private static final int WEIGHT_DIAMOND_BLOCK = 15;
    private static final int WEIGHT_TRIDENT = 10;
    private static final int WEIGHT_NETHERITE_BLOCK = 4;
    private static final int TOTAL_WEIGHT = 100;

    /**
     * Generates and spawns an Epic Bundle item at the player's location.
     * 
     * @param player The player who caught the bundle
     * @param world  The server world
     */
    public static void generateAndSpawn(ServerPlayerEntity player, ServerWorld world) {
        ItemStack bundleItem = generateRandomItem(player, world);

        if (bundleItem != null && !bundleItem.isEmpty()) {
            // Spawn the item at the player's location
            ItemEntity itemEntity = new ItemEntity(
                    world,
                    player.getX(),
                    player.getY() + 0.5,
                    player.getZ(),
                    bundleItem);
            itemEntity.setPickupDelay(0); // Instant pickup
            world.spawnEntity(itemEntity);
        }
    }

    /**
     * Generates a random Epic Bundle item based on weighted chances.
     */
    public static ItemStack generateRandomItem(ServerPlayerEntity player, ServerWorld world) {
        int roll = random.nextInt(TOTAL_WEIGHT);
        int cumulative = 0;

        // 40% - Enchanted Book
        cumulative += WEIGHT_ENCHANTED_BOOK;
        if (roll < cumulative) {
            return createEnchantedBook(world);
        }

        // 30% - Gold Block (1-3)
        cumulative += WEIGHT_GOLD_BLOCK;
        if (roll < cumulative) {
            int count = 1 + random.nextInt(3);
            return new ItemStack(Items.GOLD_BLOCK, count);
        }

        // 15% - Diamond Block (1-2)
        cumulative += WEIGHT_DIAMOND_BLOCK;
        if (roll < cumulative) {
            int count = 1 + random.nextInt(2);
            return new ItemStack(Items.DIAMOND_BLOCK, count);
        }

        // 10% - Trident with enchantment
        cumulative += WEIGHT_TRIDENT;
        if (roll < cumulative) {
            return createEnchantedTrident(world);
        }

        // 4% - Netherite Block
        cumulative += WEIGHT_NETHERITE_BLOCK;
        if (roll < cumulative) {
            return new ItemStack(Items.NETHERITE_BLOCK, 1);
        }

        // 1% - Enchanted Golden Apple
        return new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1);
    }

    /**
     * Creates an enchanted book with one OP enchantment.
     */
    private static ItemStack createEnchantedBook(ServerWorld world) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);

        // List of OP enchantments to choose from
        @SuppressWarnings("unchecked")
        RegistryKey<Enchantment>[] opEnchantments = new RegistryKey[] {
                Enchantments.MENDING,
                Enchantments.FROST_WALKER,
                Enchantments.DEPTH_STRIDER,
                Enchantments.LOOTING,
                Enchantments.FORTUNE,
                Enchantments.EFFICIENCY,
                Enchantments.SHARPNESS,
                Enchantments.PROTECTION
        };

        // Pick a random enchantment
        RegistryKey<Enchantment> chosen = opEnchantments[random.nextInt(opEnchantments.length)];

        // Get the enchantment registry
        Registry<Enchantment> registry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        Optional<RegistryEntry.Reference<Enchantment>> optionalEntry = registry.getOptional(chosen);

        if (optionalEntry.isPresent()) {
            RegistryEntry<Enchantment> enchantmentEntry = optionalEntry.get();
            Enchantment enchantment = enchantmentEntry.value();
            int level = enchantment.getMaxLevel();

            // Create enchantments component and apply to book
            ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(
                    ItemEnchantmentsComponent.DEFAULT);
            builder.add(enchantmentEntry, level);
            book.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());
        }

        return book;
    }

    /**
     * Creates a trident with a random high-level enchantment.
     */
    private static ItemStack createEnchantedTrident(ServerWorld world) {
        ItemStack trident = new ItemStack(Items.TRIDENT);

        // Trident-specific enchantments
        @SuppressWarnings("unchecked")
        RegistryKey<Enchantment>[] tridentEnchantments = new RegistryKey[] {
                Enchantments.LOYALTY,
                Enchantments.RIPTIDE,
                Enchantments.CHANNELING,
                Enchantments.IMPALING,
                Enchantments.UNBREAKING,
                Enchantments.MENDING
        };

        // Pick a random enchantment
        RegistryKey<Enchantment> chosen = tridentEnchantments[random.nextInt(tridentEnchantments.length)];

        // Get the enchantment registry
        Registry<Enchantment> registry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        Optional<RegistryEntry.Reference<Enchantment>> optionalEntry = registry.getOptional(chosen);

        if (optionalEntry.isPresent()) {
            RegistryEntry<Enchantment> enchantmentEntry = optionalEntry.get();
            Enchantment enchantment = enchantmentEntry.value();
            int level = enchantment.getMaxLevel();

            // Apply enchantment to trident
            trident.addEnchantment(enchantmentEntry, level);
        }

        return trident;
    }
}
