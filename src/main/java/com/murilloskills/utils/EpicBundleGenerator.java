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
 * REBALANCED - Epic Bundle chances (weighted random with level requirements):
 * | Weight | Item | Min Level |
 * |--------|-----------------------------------|-----------|
 * | 40% | Enchanted Book (fishing-focused) | 1 |
 * | 25% | 1-3x Gold Block | 1 |
 * | 20% | 1-2x Diamond Block | 25 |
 * | 10% | Trident with enchantment | 50 |
 * | 4% | Heart of the Sea | 75 |
 * | 1% | Enchanted Golden Apple | 90 |
 * 
 * NOTE: Netherite Block REMOVED - too powerful for fishing rewards.
 * Players should use Miner skill for Netherite.
 */
public class EpicBundleGenerator {

    private static final Random random = new Random();

    // Note: Constants replaced by SkillConfig getters

    /**
     * Generates and spawns an Epic Bundle item at the player's location.
     * 
     * @param player The player who caught the bundle
     * @param world  The server world
     */
    public static void generateAndSpawn(ServerPlayerEntity player, ServerWorld world) {
        // Get player's Fisher level
        var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
        var fisherStats = playerData.getSkill(com.murilloskills.skills.MurilloSkillsList.FISHER);
        int playerLevel = fisherStats.level;

        ItemStack bundleItem = generateRandomItem(player, world, playerLevel);

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
     * Generates a random Epic Bundle item based on weighted chances and player
     * level.
     * Items are level-gated - if player doesn't meet level requirement, rerolls to
     * lower tier.
     */
    public static ItemStack generateRandomItem(ServerPlayerEntity player, ServerWorld world, int playerLevel) {
        int totalWeight = SkillConfig.getEpicBundleWeightEnchantedBook() +
                SkillConfig.getEpicBundleWeightGoldBlock() +
                SkillConfig.getEpicBundleWeightDiamondBlock() +
                SkillConfig.getEpicBundleWeightTrident() +
                SkillConfig.getEpicBundleWeightHeartOfSea() +
                SkillConfig.getEpicBundleWeightGoldenApple();

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;

        // 40% - Enchanted Book (always available)
        cumulative += SkillConfig.getEpicBundleWeightEnchantedBook();
        if (roll < cumulative) {
            return createEnchantedBook(world);
        }

        // 25% - Gold Block (always available)
        cumulative += SkillConfig.getEpicBundleWeightGoldBlock();
        if (roll < cumulative) {
            int count = 1 + random.nextInt(3);
            return new ItemStack(Items.GOLD_BLOCK, count);
        }

        // 20% - Diamond Block (Level 25+)
        cumulative += SkillConfig.getEpicBundleWeightDiamondBlock();
        if (roll < cumulative) {
            if (playerLevel >= SkillConfig.getEpicBundleLevelDiamondBlock()) {
                int count = 1 + random.nextInt(2);
                return new ItemStack(Items.DIAMOND_BLOCK, count);
            }
            // Fallback: Gold Block for lower levels
            int count = 1 + random.nextInt(3);
            return new ItemStack(Items.GOLD_BLOCK, count);
        }

        // 10% - Trident with enchantment (Level 50+)
        cumulative += SkillConfig.getEpicBundleWeightTrident();
        if (roll < cumulative) {
            if (playerLevel >= SkillConfig.getEpicBundleLevelTrident()) {
                return createEnchantedTrident(world);
            }
            // Fallback: Diamond Block for level 25+, else Gold Block
            if (playerLevel >= SkillConfig.getEpicBundleLevelDiamondBlock()) {
                return new ItemStack(Items.DIAMOND_BLOCK, 1);
            }
            return new ItemStack(Items.GOLD_BLOCK, 2);
        }

        // 4% - Heart of the Sea (Level 75+)
        cumulative += SkillConfig.getEpicBundleWeightHeartOfSea();
        if (roll < cumulative) {
            if (playerLevel >= SkillConfig.getEpicBundleLevelHeartOfSea()) {
                return new ItemStack(Items.HEART_OF_THE_SEA, 1);
            }
            // Fallback: Trident for level 50+, Diamond for 25+, else Gold
            if (playerLevel >= SkillConfig.getEpicBundleLevelTrident()) {
                return createEnchantedTrident(world);
            }
            if (playerLevel >= SkillConfig.getEpicBundleLevelDiamondBlock()) {
                return new ItemStack(Items.DIAMOND_BLOCK, 1);
            }
            return new ItemStack(Items.GOLD_BLOCK, 2);
        }

        // 1% - Enchanted Golden Apple (Level 90+)
        if (playerLevel >= SkillConfig.getEpicBundleLevelGoldenApple()) {
            return new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1);
        }
        // Fallback: Heart of the Sea for 75+, Trident for 50+, Diamond for 25+, else
        // Gold
        if (playerLevel >= SkillConfig.getEpicBundleLevelHeartOfSea()) {
            return new ItemStack(Items.HEART_OF_THE_SEA, 1);
        }
        if (playerLevel >= SkillConfig.getEpicBundleLevelTrident()) {
            return createEnchantedTrident(world);
        }
        if (playerLevel >= SkillConfig.getEpicBundleLevelDiamondBlock()) {
            return new ItemStack(Items.DIAMOND_BLOCK, 1);
        }
        return new ItemStack(Items.GOLD_BLOCK, 2);
    }

    /**
     * Creates an enchanted book with a fishing-focused enchantment.
     * Enchantments: Mending, Lure III, Luck of the Sea III, Unbreaking III
     */
    private static ItemStack createEnchantedBook(ServerWorld world) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);

        // Fishing-focused enchantments (more thematic than old OP list)
        @SuppressWarnings("unchecked")
        RegistryKey<Enchantment>[] fishingEnchantments = new RegistryKey[] {
                Enchantments.MENDING,
                Enchantments.LURE,
                Enchantments.LUCK_OF_THE_SEA,
                Enchantments.UNBREAKING
        };

        // Pick a random enchantment
        RegistryKey<Enchantment> chosen = fishingEnchantments[random.nextInt(fishingEnchantments.length)];

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
