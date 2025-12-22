package com.murilloskills.impl;

import com.murilloskills.api.AbstractSkill;

import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Farmer skill implementation with all farmer-specific logic.
 * Features:
 * - Passive: +0.5% double harvest chance per level (max 50% at level 100)
 * - Passive: +0.15% golden crop chance per level (max 15% at level 100)
 * - Level 10: Green Thumb - +5% extra harvest, 10% seed not consumed
 * - Level 25: Fertile Ground - Crops grow 25% faster
 * - Level 50: Nutrient Cycle - 2x Bone Meal from composter, 5% extra seeds
 * - Level 75: Abundant Harvest - +15% extra harvest, 10% adjacent harvest
 * chance
 * - Level 100: Harvest Moon (active) - Auto-harvest in 8 block radius, triple
 * drops, auto-replant
 */
public class FarmerSkill extends AbstractSkill {

    // Map to track players with Harvest Moon active (UUID → start timestamp)
    private static final Map<UUID, Long> harvestMoonPlayers = new HashMap<>();

    // Map to track players with Area Planting (3x3) enabled
    private static final Map<UUID, Boolean> areaPlantingEnabled = new HashMap<>();

    // Rate limiting for area planting (UUID → last plant time in millis)
    private static final Map<UUID, Long> areaPlantingLastUse = new HashMap<>();
    private static final long AREA_PLANTING_COOLDOWN_MS = 100; // 100ms between area plants

    @Override
    public MurilloSkillsList getSkillType() {
        return MurilloSkillsList.FARMER;
    }

    @Override
    public void onActiveAbility(ServerPlayerEntity player, com.murilloskills.data.PlayerSkillData.SkillStats stats) {
        try {
            // 1. Check Level
            // 1. Verifica Nível (permite se level >= 100 OU se já prestigiou)
            boolean hasReachedMaster = stats.level >= SkillConfig.FARMER_MASTER_LEVEL || stats.prestige > 0;
            if (!hasReachedMaster) {
                player.sendMessage(Text.translatable("murilloskills.error.level_required", 100,
                        Text.translatable("murilloskills.skill.name.farmer")).formatted(Formatting.RED), true);
                return;
            }

            // 2. Check if already active
            if (isHarvestMoonActive(player)) {
                player.sendMessage(Text
                        .translatable("murilloskills.error.already_active",
                                Text.translatable("murilloskills.perk.name.farmer.master"))
                        .formatted(Formatting.RED), true);
                return;
            }

            // 3. Check Cooldown (skip if never used: lastAbilityUse == -1)
            long worldTime = player.getEntityWorld().getTime();
            long timeSinceUse = worldTime - stats.lastAbilityUse;

            long cooldownTicks = SkillConfig.toTicksLong(SkillConfig.FARMER_ABILITY_COOLDOWN_SECONDS);
            if (stats.lastAbilityUse >= 0 && timeSinceUse < cooldownTicks) {
                long secondsLeft = (cooldownTicks - timeSinceUse) / 20;
                player.sendMessage(Text.translatable("murilloskills.error.cooldown_seconds", secondsLeft)
                        .formatted(Formatting.RED), true);
                return;
            }

            // 4. Activate Harvest Moon
            stats.lastAbilityUse = worldTime;
            // Note: State is saved automatically, no need to force immediate save

            startHarvestMoon(player);

            LOGGER.info("Player {} ativou Harvest Moon", player.getName().getString());

        } catch (Exception e) {
            LOGGER.error("Error executing Farmer active ability for " + player.getName().getString(), e);
            player.sendMessage(Text.translatable("murilloskills.error.ability_error").formatted(Formatting.RED), false);
        }
    }

    @Override
    public void onTick(ServerPlayerEntity player, int level) {
        try {
            if (player.age % 20 != 0)
                return; // Execute only once per second

            // Check if Harvest Moon is active and handle it
            if (harvestMoonPlayers.containsKey(player.getUuid())) {
                long startTime = harvestMoonPlayers.get(player.getUuid());
                long currentTime = player.getEntityWorld().getTime();
                long elapsed = currentTime - startTime;

                // Harvest Moon still active - perform auto-harvest
                if (elapsed < SkillConfig.toTicks(SkillConfig.FARMER_ABILITY_DURATION_SECONDS)) {
                    performHarvestMoon(player);
                } else {
                    endHarvestMoon(player);
                }
            }

        } catch (Exception e) {
            if (player.age % 200 == 0) {
                LOGGER.error("Erro no tick do Agricultor para " + player.getName().getString(), e);
            }
        }
    }

    @Override
    public void updateAttributes(ServerPlayerEntity player, int level) {
        // Farmer doesn't use native Minecraft attributes
        // All bonuses are applied via handlers/mixins
        LOGGER.debug("Updated farmer attributes for {} - Level: {}",
                player.getName().getString(), level);
    }

    /**
     * Checks if Harvest Moon is currently active for the player
     */
    public static boolean isHarvestMoonActive(ServerPlayerEntity player) {
        if (!harvestMoonPlayers.containsKey(player.getUuid())) {
            return false;
        }

        long startTime = harvestMoonPlayers.get(player.getUuid());
        long currentTime = player.getEntityWorld().getTime();
        long elapsed = currentTime - startTime;

        return elapsed < SkillConfig.toTicks(SkillConfig.FARMER_ABILITY_DURATION_SECONDS);
    }

    /**
     * Calculates the total double harvest chance based on level and perks
     * 
     * @param level    The player's farmer level
     * @param prestige The player's prestige level for passive bonus
     */
    public static float getDoubleHarvestChance(int level, int prestige) {
        // Get prestige passive multiplier (+2% per prestige level)
        float prestigeMultiplier = com.murilloskills.utils.PrestigeManager.getPassiveMultiplier(prestige);

        // Base: 0.5% per level (with prestige bonus)
        float chance = level * SkillConfig.FARMER_DOUBLE_HARVEST_PER_LEVEL * prestigeMultiplier;

        // Level 10: +5% extra
        if (level >= SkillConfig.FARMER_GREEN_THUMB_LEVEL) {
            chance += SkillConfig.FARMER_GREEN_THUMB_EXTRA * prestigeMultiplier;
        }

        // Level 75: +15% extra
        if (level >= SkillConfig.FARMER_ABUNDANT_HARVEST_LEVEL) {
            chance += SkillConfig.FARMER_ABUNDANT_EXTRA * prestigeMultiplier;
        }

        return Math.min(chance, 1.0f); // Cap at 100%
    }

    /**
     * Calculates the golden crop chance based on level
     * 
     * @param level    The player's farmer level
     * @param prestige The player's prestige level for passive bonus
     */
    public static float getGoldenCropChance(int level, int prestige) {
        // Get prestige passive multiplier (+2% per prestige level)
        float prestigeMultiplier = com.murilloskills.utils.PrestigeManager.getPassiveMultiplier(prestige);

        // 0.15% per level (max 15% at level 100, with prestige bonus)
        return level * SkillConfig.FARMER_GOLDEN_CROP_PER_LEVEL * prestigeMultiplier;
    }

    /**
     * Gets the seed item for a given crop block
     */
    public static Item getSeedForCrop(Block block) {
        if (block == Blocks.WHEAT)
            return Items.WHEAT_SEEDS;
        if (block == Blocks.CARROTS)
            return Items.CARROT;
        if (block == Blocks.POTATOES)
            return Items.POTATO;
        if (block == Blocks.BEETROOTS)
            return Items.BEETROOT_SEEDS;
        if (block == Blocks.NETHER_WART)
            return Items.NETHER_WART;
        return null;
    }

    /**
     * Gets the crop block for a given seed item
     */
    public static Block getCropForSeed(Item seedItem) {
        if (seedItem == Items.WHEAT_SEEDS)
            return Blocks.WHEAT;
        if (seedItem == Items.CARROT)
            return Blocks.CARROTS;
        if (seedItem == Items.POTATO)
            return Blocks.POTATOES;
        if (seedItem == Items.BEETROOT_SEEDS)
            return Blocks.BEETROOTS;
        if (seedItem == Items.NETHER_WART)
            return Blocks.NETHER_WART;
        return null;
    }

    /**
     * Toggles area planting (3x3) for a player.
     * Requires level 25+.
     * 
     * @return true if now enabled, false if now disabled or player doesn't meet
     *         requirements
     */
    public static boolean toggleAreaPlanting(ServerPlayerEntity player, int level) {
        if (level < SkillConfig.FARMER_AREA_PLANTING_LEVEL) {
            return false;
        }

        UUID uuid = player.getUuid();
        boolean currentState = areaPlantingEnabled.getOrDefault(uuid, false);
        boolean newState = !currentState;
        areaPlantingEnabled.put(uuid, newState);
        return newState;
    }

    /**
     * Checks if area planting (3x3) is enabled for a player
     */
    public static boolean isAreaPlantingEnabled(ServerPlayerEntity player) {
        return areaPlantingEnabled.getOrDefault(player.getUuid(), false);
    }

    /**
     * Checks if area planting (3x3) is enabled by UUID (for mixin use)
     */
    public static boolean isAreaPlantingEnabled(UUID playerUuid) {
        return areaPlantingEnabled.getOrDefault(playerUuid, false);
    }

    /**
     * Checks if player can use area planting now (rate limit check).
     * Returns true if allowed, false if on cooldown.
     */
    public static boolean canUseAreaPlanting(UUID playerUuid) {
        long now = System.currentTimeMillis();
        long lastUse = areaPlantingLastUse.getOrDefault(playerUuid, 0L);

        if (now - lastUse >= AREA_PLANTING_COOLDOWN_MS) {
            areaPlantingLastUse.put(playerUuid, now);
            return true;
        }
        return false;
    }

    /**
     * Starts Harvest Moon ability
     */
    private void startHarvestMoon(ServerPlayerEntity player) {
        harvestMoonPlayers.put(player.getUuid(), player.getEntityWorld().getTime());

        // Sound effect
        player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);

        // Message
        player.sendMessage(
                Text.translatable("murilloskills.farmer.harvest_moon_activated").formatted(Formatting.GOLD,
                        Formatting.BOLD),
                false);
        player.sendMessage(Text.translatable("murilloskills.farmer.harvest_moon_description")
                .formatted(Formatting.YELLOW), true);
    }

    /**
     * Performs the Harvest Moon auto-harvest in radius.
     * Limited to 50 blocks per tick to prevent lag.
     */
    private void performHarvestMoon(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        BlockPos playerPos = player.getBlockPos();
        int radius = SkillConfig.FARMER_ABILITY_RADIUS;
        int harvested = 0;
        int maxHarvestPerTick = 50; // Prevent lag

        for (BlockPos pos : BlockPos.iterate(
                playerPos.add(-radius, -2, -radius),
                playerPos.add(radius, 2, radius))) {

            // Early exit to prevent lag
            if (harvested >= maxHarvestPerTick) {
                break;
            }

            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            // Check if it's a mature crop
            if (block instanceof CropBlock cropBlock) {
                if (cropBlock.isMature(state)) {
                    harvestAndReplant(player, world, pos.toImmutable(), state, block, true);
                    harvested++;
                }
            } else if (block == Blocks.NETHER_WART) {
                int age = state.get(net.minecraft.block.NetherWartBlock.AGE);
                if (age >= 3) {
                    harvestAndReplant(player, world, pos.toImmutable(), state, block, true);
                    harvested++;
                }
            }
        }
    }

    /**
     * Harvests a crop and replants if the player has seeds
     * 
     * @param tripleDrops If true, drops are tripled (Harvest Moon effect)
     */
    private void harvestAndReplant(ServerPlayerEntity player, ServerWorld world, BlockPos pos,
            BlockState state, Block block, boolean tripleDrops) {
        // Get drops
        java.util.List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player,
                player.getMainHandStack());

        // Triple drops during Harvest Moon
        int multiplier = tripleDrops ? 3 : 1;

        for (ItemStack drop : drops) {
            ItemStack multipliedDrop = drop.copy();
            multipliedDrop.setCount(drop.getCount() * multiplier);

            // Spawn item entity
            ItemEntity itemEntity = new ItemEntity(world,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    multipliedDrop);
            world.spawnEntity(itemEntity);
        }

        // Break the crop
        world.breakBlock(pos, false, player);

        // Try to replant (consume seeds from inventory)
        Item seedItem = getSeedForCrop(block);
        if (seedItem != null) {
            // Find seeds in inventory
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.getItem() == seedItem && stack.getCount() > 0) {
                    // Consume 1 seed
                    stack.decrement(1);

                    // Replant
                    world.setBlockState(pos, block.getDefaultState());

                    // Play sound
                    world.playSound(null, pos, SoundEvents.ITEM_CROP_PLANT, SoundCategory.BLOCKS, 0.5f, 1.0f);
                    break;
                }
            }
        }
    }

    /**
     * Ends Harvest Moon ability
     */
    private void endHarvestMoon(ServerPlayerEntity player) {
        harvestMoonPlayers.remove(player.getUuid());

        player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.8f);
        player.sendMessage(Text.translatable("murilloskills.farmer.harvest_moon_ended").formatted(Formatting.GRAY),
                true);

        LOGGER.debug("Player {} saiu do Harvest Moon", player.getName().getString());
    }

    /**
     * Cleanup player state when they disconnect to prevent memory leaks.
     * Should be called from player disconnect event.
     */
    public static void cleanupPlayerState(UUID playerUuid) {
        harvestMoonPlayers.remove(playerUuid);
        areaPlantingEnabled.remove(playerUuid);
        areaPlantingLastUse.remove(playerUuid);
    }

    /**
     * Cleanup all player states (for server shutdown).
     */
    public static void cleanupAllStates() {
        harvestMoonPlayers.clear();
        areaPlantingEnabled.clear();
        areaPlantingLastUse.clear();
    }
}
